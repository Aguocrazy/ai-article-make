package com.aiarticle.service.impl;

import com.aiarticle.exception.BusinessException;
import com.aiarticle.exception.ErrorCode;
import com.aiarticle.mapper.ArticleMapper;
import com.aiarticle.model.entity.Article;
import com.aiarticle.model.vo.ArticleTaskVO;
import com.aiarticle.service.SseEmitterService;
import com.aiarticle.task.ArticleGenerationTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleGenerationServiceImplTest {

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private ArticleGenerationTask articleGenerationTask;

    @Mock
    private SseEmitterService sseEmitterService;

    @Mock
    private ThreadPoolTaskExecutor articleGenerationExecutor;

    private ArticleGenerationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ArticleGenerationServiceImpl(
                articleMapper, articleGenerationTask, sseEmitterService, articleGenerationExecutor);
    }

    @Test
    void create_trimsTopicAssociatesUserAndSubmitsTask() {
        when(articleMapper.insert(any(Article.class))).thenReturn(1);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);

        ArticleTaskVO result = service.create("  人工智能写作  ", 42L);

        ArgumentCaptor<Article> articleCaptor = ArgumentCaptor.forClass(Article.class);
        verify(articleMapper).insert(articleCaptor.capture());
        Article article = articleCaptor.getValue();
        assertEquals("人工智能写作", article.getTopic());
        assertEquals(42L, article.getUserId());
        assertEquals("PENDING", article.getStatus());
        assertNotNull(article.getTaskId());
        assertFalse(article.getTaskId().isBlank());
        assertEquals(article.getTaskId(), result.taskId());

        verify(articleGenerationExecutor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();
        verify(articleGenerationTask).run(article.getTaskId());
    }

    @Test
    void create_generatesUniqueTaskIds() {
        when(articleMapper.insert(any(Article.class))).thenReturn(1);

        ArticleTaskVO first = service.create("主题", 1L);
        ArticleTaskVO second = service.create("主题", 1L);

        assertFalse(first.taskId().equals(second.taskId()));
    }

    @Test
    void create_acceptsTopicAtMaximumLength() {
        String topic = "x".repeat(500);
        when(articleMapper.insert(any(Article.class))).thenReturn(1);

        service.create(topic, 1L);

        ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
        verify(articleMapper).insert(captor.capture());
        assertEquals(topic, captor.getValue().getTopic());
    }

    @Test
    void create_rejectsInvalidTopicsAndUserIds() {
        assertParamsError(() -> service.create(null, 1L));
        assertParamsError(() -> service.create("  ", 1L));
        assertParamsError(() -> service.create("x".repeat(501), 1L));
        assertParamsError(() -> service.create("主题", 0L));
        assertParamsError(() -> service.create("主题", -1L));
        verify(articleMapper, never()).insert(any());
    }

    @Test
    void create_whenInsertAffectsZeroRows_throwsOperationErrorAndDoesNotSubmit() {
        when(articleMapper.insert(any(Article.class))).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.create("主题", 1L));

        assertEquals(ErrorCode.OPERATION_ERROR.getCode(), exception.getCode());
        verify(articleGenerationExecutor, never()).execute(any(Runnable.class));
    }

    @Test
    void create_whenRejected_marksFailedAndReturnsExactBusyMessage() {
        when(articleMapper.insert(any(Article.class))).thenReturn(1);
        doThrow(new TaskRejectedException("full", new RejectedExecutionException("secret")))
                .when(articleGenerationExecutor).execute(any(Runnable.class));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.create("主题", 1L));

        assertEquals(ErrorCode.OPERATION_ERROR.getCode(), exception.getCode());
        assertEquals("生成任务已满，请稍后重试", exception.getMessage());
        ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
        verify(articleMapper).update(captor.capture());
        assertEquals("FAILED", captor.getValue().getStatus());
        assertEquals("生成任务失败，请稍后重试", captor.getValue().getErrorMessage());
    }

    @Test
    void create_whenUnexpectedSubmissionFails_marksFailedAndSanitizesError() {
        when(articleMapper.insert(any(Article.class))).thenReturn(1);
        doThrow(new IllegalStateException("database password leaked"))
                .when(articleGenerationExecutor).execute(any(Runnable.class));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.create("主题", 1L));

        assertEquals(ErrorCode.OPERATION_ERROR.getCode(), exception.getCode());
        assertEquals("生成任务提交失败，请稍后重试", exception.getMessage());
        verify(articleMapper).update(any(Article.class));
    }

    @Test
    void create_whenFailureStatusUpdateFails_preservesBusyError() {
        when(articleMapper.insert(any(Article.class))).thenReturn(1);
        doThrow(new TaskRejectedException("full"))
                .when(articleGenerationExecutor).execute(any(Runnable.class));
        when(articleMapper.update(any(Article.class))).thenThrow(new IllegalStateException("update failed"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.create("主题", 1L));

        assertEquals("生成任务已满，请稍后重试", exception.getMessage());
    }

    @Test
    void subscribe_ownedTaskDelegatesToEmitterService() {
        Article article = Article.builder().taskId("task-1").userId(7L).build();
        SseEmitter emitter = new SseEmitter();
        when(articleMapper.selectOneByQuery(any())).thenReturn(article);
        when(sseEmitterService.subscribe("task-1")).thenReturn(emitter);

        assertSame(emitter, service.subscribe("task-1", 7L));
    }

    @Test
    void subscribe_whenMissing_returnsNotFound() {
        when(articleMapper.selectOneByQuery(any())).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.subscribe("missing", 7L));

        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), exception.getCode());
    }

    @Test
    void subscribe_whenWrongOwnerOrNullOwner_returnsNoAuthWithoutUnboxing() {
        when(articleMapper.selectOneByQuery(any()))
                .thenReturn(Article.builder().taskId("task").userId(8L).build())
                .thenReturn(Article.builder().taskId("task").userId(null).build());

        BusinessException wrongOwner = assertThrows(BusinessException.class,
                () -> service.subscribe("task", 7L));
        BusinessException nullOwner = assertThrows(BusinessException.class,
                () -> service.subscribe("task", 7L));

        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), wrongOwner.getCode());
        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), nullOwner.getCode());
        verify(sseEmitterService, never()).subscribe(any());
    }

    @Test
    void subscribe_rejectsNullAndBlankTaskIdsBeforeQuerying() {
        assertParamsError(() -> service.subscribe(null, 1L));
        assertParamsError(() -> service.subscribe(" ", 1L));
        verify(articleMapper, never()).selectOneByQuery(any());
    }

    @Test
    void subscribe_rejectsNonPositiveUserIdsBeforeQuerying() {
        assertParamsError(() -> service.subscribe("task", 0L));
        assertParamsError(() -> service.subscribe("task", -1L));
        verify(articleMapper, never()).selectOneByQuery(any());
    }

    private void assertParamsError(Runnable action) {
        BusinessException exception = assertThrows(BusinessException.class, action::run);
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
    }
}
