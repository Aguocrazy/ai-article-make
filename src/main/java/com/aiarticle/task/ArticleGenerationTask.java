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
import org.springframework.util.StringUtils;

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

    private static final int MAX_ERROR_MESSAGE_LENGTH = 2_000;
    private static final String DEFAULT_ERROR_MESSAGE = "文章生成失败";

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
            articleMapper.update(article);

            titleAgent.generate(state);
            sseEmitterService.send(taskId, SseMessageTypeEnum.AGENT1_COMPLETE, state.getTitle());

            outlineAgent.generate(state, streamingHandler(taskId, SseMessageTypeEnum.AGENT2_STREAMING));
            sseEmitterService.send(taskId, SseMessageTypeEnum.AGENT2_COMPLETE, state.getOutline());

            contentAgent.generate(state, streamingHandler(taskId, SseMessageTypeEnum.AGENT3_STREAMING));
            sseEmitterService.send(taskId, SseMessageTypeEnum.AGENT3_COMPLETE, state.getContent());

            imageRequirementAgent.generate(state);
            sseEmitterService.send(taskId, SseMessageTypeEnum.AGENT4_COMPLETE,
                    state.getImageRequirements());

            imageAgent.generate(state, streamingHandler(taskId, SseMessageTypeEnum.IMAGE_COMPLETE));
            sseEmitterService.send(taskId, SseMessageTypeEnum.AGENT5_COMPLETE, state.getImages());

            articleMergeAgent.merge(state);
            sseEmitterService.send(taskId, SseMessageTypeEnum.MERGE_COMPLETE, state.getFullContent());

            copyCompletedState(article, state);
            articleMapper.update(article);
            sseEmitterService.complete(taskId, SseMessageTypeEnum.ALL_COMPLETE, state);
        } catch (RuntimeException exception) {
            handleFailure(article, taskId, exception);
        }
    }

    private Consumer<String> streamingHandler(String taskId, SseMessageTypeEnum expectedType) {
        return callback -> forwardCallback(taskId, expectedType, callback);
    }

    private void forwardCallback(String taskId, SseMessageTypeEnum expectedType, String callback) {
        if (callback == null) {
            log.warn("忽略格式错误的智能体回调, taskId={}, callback=null", taskId);
            return;
        }
        int separator = callback.indexOf(':');
        if (separator <= 0) {
            log.warn("忽略格式错误的智能体回调, taskId={}, callback={}", taskId, callback);
            return;
        }

        String prefix = callback.substring(0, separator);
        SseMessageTypeEnum actualType = Arrays.stream(SseMessageTypeEnum.values())
                .filter(type -> type.getValue().equals(prefix))
                .findFirst()
                .orElse(null);
        if (actualType != expectedType) {
            log.warn("忽略未知或非预期的智能体回调, taskId={}, prefix={}, expected={}",
                    taskId, prefix, expectedType.getValue());
            return;
        }
        sseEmitterService.send(taskId, actualType, callback.substring(separator + 1));
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

    private void handleFailure(Article article, String taskId, RuntimeException exception) {
        log.error("文章生成失败, taskId={}", taskId, exception);
        String errorMessage = safeErrorMessage(exception);
        article.setStatus(ArticleConstant.STATUS_FAILED);
        article.setErrorMessage(errorMessage);
        try {
            articleMapper.update(article);
        } catch (RuntimeException persistenceException) {
            log.error("文章失败状态落库失败, taskId={}", taskId, persistenceException);
        }
        sseEmitterService.complete(taskId, SseMessageTypeEnum.ERROR, errorMessage);
    }

    private String safeErrorMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (!StringUtils.hasText(message)) {
            message = DEFAULT_ERROR_MESSAGE;
        }
        return message.length() <= MAX_ERROR_MESSAGE_LENGTH
                ? message
                : message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }
}
