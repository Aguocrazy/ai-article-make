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
import com.aiarticle.model.state.ArticleState.ImageRequirement;
import com.aiarticle.model.state.ArticleState.ImageResult;
import com.aiarticle.model.state.ArticleState.OutlineResult;
import com.aiarticle.model.state.ArticleState.OutlineSection;
import com.aiarticle.model.state.ArticleState.TitleResult;
import com.aiarticle.service.SseEmitterService;
import com.aiarticle.util.GsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleGenerationTaskTest {

    @Mock
    private ArticleMapper articleMapper;
    @Mock
    private TitleAgent titleAgent;
    @Mock
    private OutlineAgent outlineAgent;
    @Mock
    private ContentAgent contentAgent;
    @Mock
    private ImageRequirementAgent imageRequirementAgent;
    @Mock
    private ImageAgent imageAgent;
    @Mock
    private ArticleMergeAgent articleMergeAgent;
    @Mock
    private SseEmitterService sseEmitterService;

    private ArticleGenerationTask task;

    @BeforeEach
    void setUp() {
        task = new ArticleGenerationTask(articleMapper, titleAgent, outlineAgent, contentAgent,
                imageRequirementAgent, imageAgent, articleMergeAgent, sseEmitterService);
    }

    @Test
    void run_executesPipelineInOrderAndPersistsCompletedArticle() {
        Article article = Article.builder().taskId("task-1").topic("AI 写作").build();
        when(articleMapper.selectOneByQuery(any())).thenReturn(article);
        List<String> persistedStatuses = new ArrayList<>();
        when(articleMapper.update(any(Article.class))).thenAnswer(invocation -> {
            persistedStatuses.add(invocation.<Article>getArgument(0).getStatus());
            return 1;
        });

        TitleResult title = new TitleResult();
        title.setMainTitle("主标题");
        title.setSubTitle("副标题");
        OutlineSection section = new OutlineSection();
        section.setSection(1);
        section.setTitle("第一章");
        section.setPoints(List.of("要点"));
        OutlineResult outline = new OutlineResult();
        outline.setSections(List.of(section));
        ImageRequirement requirement = new ImageRequirement();
        requirement.setPosition(1);
        requirement.setKeywords("人工智能");
        ImageResult image = new ImageResult();
        image.setPosition(1);
        image.setUrl("https://example.com/cover.jpg");

        when(titleAgent.generate(any())).thenAnswer(invocation -> {
            ArticleState state = invocation.getArgument(0);
            state.setTitle(title);
            return state;
        });
        when(outlineAgent.generate(any(), any())).thenAnswer(invocation -> {
            ArticleState state = invocation.getArgument(0);
            invocation.<Consumer<String>>getArgument(1).accept("AGENT2_STREAMING:大纲:片段");
            state.setOutline(outline);
            return state;
        });
        when(contentAgent.generate(any(), any())).thenAnswer(invocation -> {
            ArticleState state = invocation.getArgument(0);
            invocation.<Consumer<String>>getArgument(1).accept("AGENT3_STREAMING:正文片段");
            state.setContent("完整正文");
            return state;
        });
        when(imageRequirementAgent.generate(any())).thenAnswer(invocation -> {
            ArticleState state = invocation.getArgument(0);
            state.setImageRequirements(List.of(requirement));
            return state;
        });
        when(imageAgent.generate(any(), any())).thenAnswer(invocation -> {
            ArticleState state = invocation.getArgument(0);
            invocation.<Consumer<String>>getArgument(1).accept("IMAGE_COMPLETE:{\"url\":\"a:b\"}");
            state.setCoverImage(image.getUrl());
            state.setImages(List.of(image));
            return state;
        });
        when(articleMergeAgent.merge(any())).thenAnswer(invocation -> {
            ArticleState state = invocation.getArgument(0);
            state.setFullContent("完整图文");
            return state;
        });

        task.run("task-1");

        InOrder agents = inOrder(titleAgent, outlineAgent, contentAgent, imageRequirementAgent,
                imageAgent, articleMergeAgent);
        agents.verify(titleAgent).generate(any());
        agents.verify(outlineAgent).generate(any(), any());
        agents.verify(contentAgent).generate(any(), any());
        agents.verify(imageRequirementAgent).generate(any());
        agents.verify(imageAgent).generate(any(), any());
        agents.verify(articleMergeAgent).merge(any());
        assertEquals(List.of(ArticleConstant.STATUS_PROCESSING, ArticleConstant.STATUS_COMPLETED),
                persistedStatuses);
        assertEquals("主标题", article.getMainTitle());
        assertEquals("副标题", article.getSubTitle());
        assertEquals(GsonUtils.toJson(outline), article.getOutline());
        assertEquals("完整正文", article.getContent());
        assertEquals("完整图文", article.getFullContent());
        assertEquals(image.getUrl(), article.getCoverImage());
        assertEquals(GsonUtils.toJson(List.of(image)), article.getImages());
        assertEquals(ArticleConstant.STATUS_COMPLETED, article.getStatus());
        assertNotNull(article.getCompletedTime());
        assertEquals(null, article.getErrorMessage());

        InOrder events = inOrder(sseEmitterService);
        events.verify(sseEmitterService).send("task-1", SseMessageTypeEnum.AGENT1_COMPLETE, title);
        events.verify(sseEmitterService).send("task-1", SseMessageTypeEnum.AGENT2_STREAMING, "大纲:片段");
        events.verify(sseEmitterService).send("task-1", SseMessageTypeEnum.AGENT2_COMPLETE, outline);
        events.verify(sseEmitterService).send("task-1", SseMessageTypeEnum.AGENT3_STREAMING, "正文片段");
        events.verify(sseEmitterService).send("task-1", SseMessageTypeEnum.AGENT3_COMPLETE, "完整正文");
        events.verify(sseEmitterService).send("task-1", SseMessageTypeEnum.AGENT4_COMPLETE,
                List.of(requirement));
        events.verify(sseEmitterService).send("task-1", SseMessageTypeEnum.IMAGE_COMPLETE,
                "{\"url\":\"a:b\"}");
        events.verify(sseEmitterService).send("task-1", SseMessageTypeEnum.AGENT5_COMPLETE,
                List.of(image));
        events.verify(sseEmitterService).send("task-1", SseMessageTypeEnum.MERGE_COMPLETE, "完整图文");
        events.verify(sseEmitterService).complete(eq("task-1"), eq(SseMessageTypeEnum.ALL_COMPLETE),
                any(ArticleState.class));
    }

    @Test
    void run_whenGenerationFails_persistsSafeFailureAndCompletesError() {
        Article article = Article.builder().taskId("task-2").topic("失败主题").build();
        when(articleMapper.selectOneByQuery(any())).thenReturn(article);
        when(articleMapper.update(any())).thenReturn(1);
        when(titleAgent.generate(any())).thenThrow(new RuntimeException(" "));

        task.run("task-2");

        assertEquals(ArticleConstant.STATUS_FAILED, article.getStatus());
        assertTrue(article.getErrorMessage() != null && !article.getErrorMessage().isBlank());
        assertTrue(article.getErrorMessage().length() <= 2000);
        verify(sseEmitterService).complete("task-2", SseMessageTypeEnum.ERROR,
                article.getErrorMessage());
        verify(outlineAgent, never()).generate(any(), any());
    }

    @Test
    void run_whenFailurePersistenceAlsoFails_stillCompletesOriginalError() {
        Article article = Article.builder().taskId("task-3").topic("异常主题").build();
        when(articleMapper.selectOneByQuery(any())).thenReturn(article);
        when(articleMapper.update(any())).thenReturn(1).thenThrow(new RuntimeException("数据库异常"));
        when(titleAgent.generate(any())).thenThrow(new RuntimeException("模型异常"));

        task.run("task-3");

        verify(articleMapper, times(2)).update(article);
        verify(sseEmitterService).complete("task-3", SseMessageTypeEnum.ERROR,
                "文章生成失败，请稍后重试");
    }

    @Test
    void run_whenProcessingUpdateAffectsNoRows_failsBeforeAgentWorkAndAttemptsFailureUpdate() {
        Article article = Article.builder().taskId("task-processing-zero").topic("主题").build();
        when(articleMapper.selectOneByQuery(any())).thenReturn(article);
        when(articleMapper.update(any())).thenReturn(0).thenReturn(1);

        task.run("task-processing-zero");

        assertEquals(ArticleConstant.STATUS_FAILED, article.getStatus());
        verify(articleMapper, times(2)).update(article);
        verify(titleAgent, never()).generate(any());
        verify(sseEmitterService).complete("task-processing-zero", SseMessageTypeEnum.ERROR,
                "文章生成失败，请稍后重试");
    }

    @Test
    void run_whenCompletedUpdateAffectsNoRows_marksFailedAndSendsError() {
        Article article = Article.builder().taskId("task-completed-zero").topic("主题").build();
        when(articleMapper.selectOneByQuery(any())).thenReturn(article);
        when(articleMapper.update(any())).thenReturn(1).thenReturn(0).thenReturn(1);
        stubSuccessfulPipeline();

        task.run("task-completed-zero");

        assertEquals(ArticleConstant.STATUS_FAILED, article.getStatus());
        verify(articleMapper, times(3)).update(article);
        verify(sseEmitterService, never()).complete(eq("task-completed-zero"),
                eq(SseMessageTypeEnum.ALL_COMPLETE), any());
        verify(sseEmitterService).complete("task-completed-zero", SseMessageTypeEnum.ERROR,
                "文章生成失败，请稍后重试");
    }

    @Test
    void run_whenStageNotificationFails_continuesPipelineAndPersistsCompleted() {
        Article article = Article.builder().taskId("task-stage-send").topic("主题").build();
        when(articleMapper.selectOneByQuery(any())).thenReturn(article);
        when(articleMapper.update(any())).thenReturn(1);
        stubSuccessfulPipeline();
        doThrow(new RuntimeException("SSE 断开")).when(sseEmitterService)
                .send(eq("task-stage-send"), eq(SseMessageTypeEnum.AGENT1_COMPLETE), any());

        task.run("task-stage-send");

        assertEquals(ArticleConstant.STATUS_COMPLETED, article.getStatus());
        verify(articleMergeAgent).merge(any());
        verify(articleMapper, times(2)).update(article);
        verify(sseEmitterService).complete(eq("task-stage-send"),
                eq(SseMessageTypeEnum.ALL_COMPLETE), any());
    }

    @Test
    void run_whenStreamingNotificationFails_continuesPipelineAndPersistsCompleted() {
        Article article = Article.builder().taskId("task-stream-send").topic("主题").build();
        when(articleMapper.selectOneByQuery(any())).thenReturn(article);
        when(articleMapper.update(any())).thenReturn(1);
        stubSuccessfulPipeline();
        when(outlineAgent.generate(any(), any())).thenAnswer(invocation -> {
            ArticleState state = invocation.getArgument(0);
            invocation.<Consumer<String>>getArgument(1)
                    .accept("AGENT2_STREAMING:不能泄露的文章片段");
            return state;
        });
        doThrow(new RuntimeException("SSE 断开")).when(sseEmitterService)
                .send("task-stream-send", SseMessageTypeEnum.AGENT2_STREAMING,
                        "不能泄露的文章片段");

        task.run("task-stream-send");

        assertEquals(ArticleConstant.STATUS_COMPLETED, article.getStatus());
        verify(contentAgent).generate(any(), any());
        verify(articleMergeAgent).merge(any());
        verify(articleMapper, times(2)).update(article);
    }

    @Test
    void run_whenCompletedUpdateThrows_restoresPreCompletionShapeBeforeFailureUpdate() {
        Article article = Article.builder().taskId("task-completed-throw").topic("主题").build();
        when(articleMapper.selectOneByQuery(any())).thenReturn(article);
        List<Article> persistenceSnapshots = new ArrayList<>();
        when(articleMapper.update(any())).thenAnswer(invocation -> {
            Article value = invocation.getArgument(0);
            persistenceSnapshots.add(Article.builder()
                    .status(value.getStatus())
                    .mainTitle(value.getMainTitle())
                    .content(value.getContent())
                    .fullContent(value.getFullContent())
                    .completedTime(value.getCompletedTime())
                    .errorMessage(value.getErrorMessage())
                    .build());
            if (persistenceSnapshots.size() == 2) {
                throw new RuntimeException("完成状态写入失败");
            }
            return 1;
        });
        stubSuccessfulPipeline();

        task.run("task-completed-throw");

        assertEquals(3, persistenceSnapshots.size());
        Article failedWrite = persistenceSnapshots.get(2);
        assertEquals(ArticleConstant.STATUS_FAILED, failedWrite.getStatus());
        assertEquals(null, failedWrite.getMainTitle());
        assertEquals(null, failedWrite.getContent());
        assertEquals(null, failedWrite.getFullContent());
        assertEquals(null, failedWrite.getCompletedTime());
        assertEquals("文章生成失败，请稍后重试", failedWrite.getErrorMessage());
    }

    @Test
    void run_whenAllCompleteNotificationFails_keepsPersistedArticleCompleted() {
        Article article = Article.builder().taskId("task-terminal-failure").topic("主题").build();
        when(articleMapper.selectOneByQuery(any())).thenReturn(article);
        when(articleMapper.update(any())).thenReturn(1);
        stubSuccessfulPipeline();
        doThrow(new RuntimeException("SSE 断开")).when(sseEmitterService)
                .complete(eq("task-terminal-failure"), eq(SseMessageTypeEnum.ALL_COMPLETE), any());

        assertDoesNotThrow(() -> task.run("task-terminal-failure"));

        assertEquals(ArticleConstant.STATUS_COMPLETED, article.getStatus());
        verify(articleMapper, times(2)).update(article);
        verify(sseEmitterService, never()).complete(eq("task-terminal-failure"),
                eq(SseMessageTypeEnum.ERROR), any());
    }

    @Test
    void run_whenErrorNotificationFails_doesNotEscape() {
        Article article = Article.builder().taskId("task-error-notification").topic("主题").build();
        when(articleMapper.selectOneByQuery(any())).thenReturn(article);
        when(articleMapper.update(any())).thenReturn(1);
        when(titleAgent.generate(any())).thenThrow(new RuntimeException("模型异常"));
        doThrow(new RuntimeException("SSE 断开")).when(sseEmitterService)
                .complete("task-error-notification", SseMessageTypeEnum.ERROR,
                        "文章生成失败，请稍后重试");

        assertDoesNotThrow(() -> task.run("task-error-notification"));

        assertEquals(ArticleConstant.STATUS_FAILED, article.getStatus());
        verify(articleMapper, times(2)).update(article);
    }

    @Test
    void run_whenInternalFailureContainsSecret_persistsAndSendsSanitizedMessage() {
        Article article = Article.builder().taskId("task-secret").topic("主题").build();
        when(articleMapper.selectOneByQuery(any())).thenReturn(article);
        when(articleMapper.update(any())).thenReturn(1);
        when(titleAgent.generate(any())).thenThrow(
                new RuntimeException("DashScope apiKey=sk-secret credential rejected"));

        task.run("task-secret");

        assertEquals("文章生成失败，请稍后重试", article.getErrorMessage());
        verify(sseEmitterService).complete("task-secret", SseMessageTypeEnum.ERROR,
                "文章生成失败，请稍后重试");
    }

    @Test
    void run_rejectsMalformedAndUnknownCallbacks() {
        Article article = Article.builder().taskId("task-4").topic("回调主题").build();
        when(articleMapper.selectOneByQuery(any())).thenReturn(article);
        when(articleMapper.update(any())).thenReturn(1);
        when(titleAgent.generate(any())).thenAnswer(invocation -> {
            ArticleState state = invocation.getArgument(0);
            state.setTitle(new TitleResult());
            return state;
        });
        when(outlineAgent.generate(any(), any())).thenAnswer(invocation -> {
            Consumer<String> callback = invocation.getArgument(1);
            callback.accept("malformed");
            callback.accept("UNKNOWN:data");
            throw new RuntimeException("stop");
        });

        task.run("task-4");

        verify(sseEmitterService, never()).send(eq("task-4"), eq(SseMessageTypeEnum.AGENT2_STREAMING),
                any());
        verify(sseEmitterService, never()).send(eq("task-4"), eq(SseMessageTypeEnum.AGENT3_STREAMING),
                any());
        verify(sseEmitterService, never()).send(eq("task-4"), eq(SseMessageTypeEnum.IMAGE_COMPLETE),
                any());
    }

    @Test
    void run_whenTaskMissing_failsBeforeAgentWork() {
        when(articleMapper.selectOneByQuery(any())).thenReturn(null);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> task.run("missing"));

        assertTrue(exception.getMessage().contains("missing"));
        verify(titleAgent, never()).generate(any());
        verify(articleMapper, never()).update(any());
        verify(sseEmitterService, never()).complete(any(), any(), any());
    }

    private void stubSuccessfulPipeline() {
        when(titleAgent.generate(any())).thenAnswer(invocation -> {
            ArticleState state = invocation.getArgument(0);
            state.setTitle(new TitleResult());
            return state;
        });
        when(outlineAgent.generate(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(contentAgent.generate(any(), any())).thenAnswer(invocation -> {
            ArticleState state = invocation.getArgument(0);
            state.setContent("正文");
            return state;
        });
        when(imageRequirementAgent.generate(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(imageAgent.generate(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(articleMergeAgent.merge(any())).thenAnswer(invocation -> {
            ArticleState state = invocation.getArgument(0);
            state.setFullContent("完整图文");
            return state;
        });
    }
}
