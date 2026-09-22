package com.aiarticle.service.impl;

import cn.hutool.core.util.IdUtil;
import com.aiarticle.config.ArticleGenerationExecutorConfig;
import com.aiarticle.constant.ArticleConstant;
import com.aiarticle.exception.BusinessException;
import com.aiarticle.exception.ErrorCode;
import com.aiarticle.mapper.ArticleMapper;
import com.aiarticle.model.entity.Article;
import com.aiarticle.model.vo.ArticleTaskVO;
import com.aiarticle.service.ArticleGenerationService;
import com.aiarticle.service.SseEmitterService;
import com.aiarticle.task.ArticleGenerationTask;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;

import static com.aiarticle.model.entity.table.ArticleTableDef.ARTICLE;

/**
 * 文章异步生成服务实现。
 */
@Slf4j
@Service
public class ArticleGenerationServiceImpl implements ArticleGenerationService {

    private static final int MAX_TOPIC_LENGTH = 500;
    private static final String SAFE_FAILURE_MESSAGE = "生成任务失败，请稍后重试";
    private static final String BUSY_MESSAGE = "生成任务已满，请稍后重试";
    private static final String SUBMIT_FAILURE_MESSAGE = "生成任务提交失败，请稍后重试";

    private final ArticleMapper articleMapper;
    private final ArticleGenerationTask articleGenerationTask;
    private final SseEmitterService sseEmitterService;
    private final ThreadPoolTaskExecutor articleGenerationExecutor;

    public ArticleGenerationServiceImpl(
            ArticleMapper articleMapper,
            ArticleGenerationTask articleGenerationTask,
            SseEmitterService sseEmitterService,
            @Qualifier(ArticleGenerationExecutorConfig.EXECUTOR_NAME)
            ThreadPoolTaskExecutor articleGenerationExecutor) {
        this.articleMapper = articleMapper;
        this.articleGenerationTask = articleGenerationTask;
        this.sseEmitterService = sseEmitterService;
        this.articleGenerationExecutor = articleGenerationExecutor;
    }

    @Override
    public ArticleTaskVO create(String topic, long userId) {
        String normalizedTopic = validateAndNormalize(topic, userId);
        String taskId = IdUtil.fastSimpleUUID();
        Article article = Article.builder()
                .taskId(taskId)
                .userId(userId)
                .topic(normalizedTopic)
                .status(ArticleConstant.STATUS_PENDING)
                .build();
        if (articleMapper.insert(article) != 1) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建生成任务失败");
        }

        try {
            articleGenerationExecutor.execute(() -> articleGenerationTask.run(taskId));
        } catch (RejectedExecutionException exception) {
            markFailedBestEffort(article, exception);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, BUSY_MESSAGE);
        } catch (RuntimeException exception) {
            markFailedBestEffort(article, exception);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, SUBMIT_FAILURE_MESSAGE);
        }
        return new ArticleTaskVO(taskId);
    }

    @Override
    public SseEmitter subscribe(String taskId, long userId) {
        if (taskId == null || taskId.isBlank() || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Article article = articleMapper.selectOneByQuery(QueryWrapper.create()
                .where(ARTICLE.TASK_ID.eq(taskId)));
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (!Objects.equals(article.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return sseEmitterService.subscribe(taskId);
    }

    private String validateAndNormalize(String topic, long userId) {
        if (topic == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String normalizedTopic = topic.trim();
        if (normalizedTopic.isEmpty() || normalizedTopic.length() > MAX_TOPIC_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return normalizedTopic;
    }

    private void markFailedBestEffort(Article article, RuntimeException submissionException) {
        log.error("文章生成任务提交失败, taskId={}", article.getTaskId(), submissionException);
        article.setStatus(ArticleConstant.STATUS_FAILED);
        article.setErrorMessage(SAFE_FAILURE_MESSAGE);
        try {
            int affectedRows = articleMapper.update(article);
            if (affectedRows != 1) {
                log.error("文章任务失败状态未更新记录, taskId={}, affectedRows={}",
                        article.getTaskId(), affectedRows);
            }
        } catch (RuntimeException updateException) {
            log.error("文章任务失败状态落库失败, taskId={}", article.getTaskId(), updateException);
        }
    }
}
