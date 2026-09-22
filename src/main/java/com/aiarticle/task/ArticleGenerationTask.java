package com.aiarticle.task;

import com.aiarticle.agent.ArticleMergeAgent;
import com.aiarticle.agent.ContentAgent;
import com.aiarticle.agent.ImageAgent;
import com.aiarticle.agent.ImageRequirementAgent;
import com.aiarticle.agent.OutlineAgent;
import com.aiarticle.agent.TitleAgent;
import com.aiarticle.constant.ArticleConstant;
import com.aiarticle.enums.SseMessageTypeEnum;
import com.aiarticle.mapper.ArticleMapper;
import com.aiarticle.model.entity.Article;
import com.aiarticle.model.state.ArticleState;
import com.aiarticle.service.SseEmitterService;
import com.aiarticle.util.GsonUtils;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.function.Consumer;

import static com.aiarticle.model.entity.table.ArticleTableDef.ARTICLE;

/**
 * 文章生成任务：按固定顺序编排各智能体，并同步生成状态与 SSE 事件。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleGenerationTask {

    private static final String SAFE_ERROR_MESSAGE = "文章生成失败，请稍后重试";

    private final ArticleMapper articleMapper;
    private final TitleAgent titleAgent;
    private final OutlineAgent outlineAgent;
    private final ContentAgent contentAgent;
    private final ImageRequirementAgent imageRequirementAgent;
    private final ImageAgent imageAgent;
    private final ArticleMergeAgent articleMergeAgent;
    private final SseEmitterService sseEmitterService;

    /**
     * 执行指定文章任务。
     *
     * @param taskId 任务 ID
     */
    public void run(String taskId) {
        Article article = articleMapper.selectOneByQuery(QueryWrapper.create()
                .where(ARTICLE.TASK_ID.eq(taskId)));
        if (article == null) {
            throw new IllegalStateException("文章任务不存在: " + taskId);
        }

        ArticleState state = new ArticleState();
        state.setTaskId(taskId);
        state.setTopic(article.getTopic());

        try {
            article.setStatus(ArticleConstant.STATUS_PROCESSING);
            updateRequired(article, ArticleConstant.STATUS_PROCESSING);

            titleAgent.generate(state);
            safeSend(taskId, SseMessageTypeEnum.AGENT1_COMPLETE, state.getTitle());

            outlineAgent.generate(state, streamingHandler(taskId, SseMessageTypeEnum.AGENT2_STREAMING));
            safeSend(taskId, SseMessageTypeEnum.AGENT2_COMPLETE, state.getOutline());

            contentAgent.generate(state, streamingHandler(taskId, SseMessageTypeEnum.AGENT3_STREAMING));
            safeSend(taskId, SseMessageTypeEnum.AGENT3_COMPLETE, state.getContent());

            imageRequirementAgent.generate(state);
            safeSend(taskId, SseMessageTypeEnum.AGENT4_COMPLETE,
                    state.getImageRequirements());

            imageAgent.generate(state, streamingHandler(taskId, SseMessageTypeEnum.IMAGE_COMPLETE));
            safeSend(taskId, SseMessageTypeEnum.AGENT5_COMPLETE, state.getImages());

            articleMergeAgent.merge(state);
            safeSend(taskId, SseMessageTypeEnum.MERGE_COMPLETE, state.getFullContent());

            persistCompleted(article, state);
        } catch (RuntimeException exception) {
            handleFailure(article, taskId, exception);
            return;
        }

        try {
            sseEmitterService.complete(taskId, SseMessageTypeEnum.ALL_COMPLETE, state);
        } catch (RuntimeException exception) {
            log.error("文章已完成但终态通知失败, taskId={}", taskId, exception);
        }
    }

    private Consumer<String> streamingHandler(String taskId, SseMessageTypeEnum expectedType) {
        return callback -> forwardCallback(taskId, expectedType, callback);
    }

    private void forwardCallback(String taskId, SseMessageTypeEnum expectedType, String callback) {
        if (callback == null) {
            log.warn("忽略格式错误的智能体回调, taskId={}, expected={}, length=0",
                    taskId, expectedType.getValue());
            return;
        }
        int separator = callback.indexOf(':');
        if (separator <= 0) {
            log.warn("忽略格式错误的智能体回调, taskId={}, expected={}, length={}",
                    taskId, expectedType.getValue(), callback.length());
            return;
        }

        String prefix = callback.substring(0, separator);
        SseMessageTypeEnum actualType = Arrays.stream(SseMessageTypeEnum.values())
                .filter(type -> type.getValue().equals(prefix))
                .findFirst()
                .orElse(null);
        if (actualType != expectedType) {
            log.warn("忽略未知或非预期的智能体回调, taskId={}, expected={}, length={}",
                    taskId, expectedType.getValue(), callback.length());
            return;
        }
        safeSend(taskId, actualType, callback.substring(separator + 1));
    }

    private void safeSend(String taskId, SseMessageTypeEnum type, Object data) {
        try {
            sseEmitterService.send(taskId, type, data);
        } catch (RuntimeException exception) {
            log.error("文章进度通知失败, taskId={}, type={}", taskId, type.getValue(), exception);
        }
    }

    private void persistCompleted(Article article, ArticleState state) {
        CompletionSnapshot snapshot = CompletionSnapshot.capture(article);
        try {
            copyCompletedState(article, state);
            updateRequired(article, ArticleConstant.STATUS_COMPLETED);
        } catch (RuntimeException exception) {
            snapshot.restore(article);
            throw exception;
        }
    }

    private void copyCompletedState(Article article, ArticleState state) {
        if (state.getTitle() != null) {
            article.setMainTitle(state.getTitle().getMainTitle());
            article.setSubTitle(state.getTitle().getSubTitle());
        }
        article.setOutline(GsonUtils.toJson(state.getOutline()));
        article.setContent(state.getContent());
        article.setFullContent(state.getFullContent());
        article.setCoverImage(state.getCoverImage());
        article.setImages(GsonUtils.toJson(state.getImages()));
        article.setStatus(ArticleConstant.STATUS_COMPLETED);
        article.setCompletedTime(LocalDateTime.now());
        article.setErrorMessage(null);
    }

    private void updateRequired(Article article, String targetStatus) {
        int affectedRows = articleMapper.update(article);
        if (affectedRows != 1) {
            throw new IllegalStateException("文章状态更新失败: " + targetStatus);
        }
    }

    private void handleFailure(Article article, String taskId, RuntimeException exception) {
        log.error("文章生成失败, taskId={}", taskId, exception);
        article.setStatus(ArticleConstant.STATUS_FAILED);
        article.setErrorMessage(SAFE_ERROR_MESSAGE);
        try {
            int affectedRows = articleMapper.update(article);
            if (affectedRows != 1) {
                log.error("文章失败状态落库未更新记录, taskId={}, affectedRows={}",
                        taskId, affectedRows);
            }
        } catch (RuntimeException persistenceException) {
            log.error("文章失败状态落库失败, taskId={}", taskId, persistenceException);
        }
        try {
            sseEmitterService.complete(taskId, SseMessageTypeEnum.ERROR, SAFE_ERROR_MESSAGE);
        } catch (RuntimeException notificationException) {
            log.error("文章失败终态通知失败, taskId={}", taskId, notificationException);
        }
    }

    private record CompletionSnapshot(
            String mainTitle,
            String subTitle,
            String outline,
            String content,
            String fullContent,
            String coverImage,
            String images,
            String status,
            String errorMessage,
            LocalDateTime completedTime) {

        private static CompletionSnapshot capture(Article article) {
            return new CompletionSnapshot(
                    article.getMainTitle(),
                    article.getSubTitle(),
                    article.getOutline(),
                    article.getContent(),
                    article.getFullContent(),
                    article.getCoverImage(),
                    article.getImages(),
                    article.getStatus(),
                    article.getErrorMessage(),
                    article.getCompletedTime());
        }

        private void restore(Article article) {
            article.setMainTitle(mainTitle);
            article.setSubTitle(subTitle);
            article.setOutline(outline);
            article.setContent(content);
            article.setFullContent(fullContent);
            article.setCoverImage(coverImage);
            article.setImages(images);
            article.setStatus(status);
            article.setErrorMessage(errorMessage);
            article.setCompletedTime(completedTime);
        }
    }
}
