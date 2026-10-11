package com.aiarticle.agent;

import com.aiarticle.enums.SseMessageTypeEnum;
import com.aiarticle.exception.BusinessException;
import com.aiarticle.model.state.ArticleState;
import com.aiarticle.model.state.ArticleState.ImageRequirement;
import com.aiarticle.service.image.ImageCloudStore;
import com.aiarticle.service.image.ImagePayload;
import com.aiarticle.service.image.ImageSearchRequest;
import com.aiarticle.service.image.ImageSearchService;
import com.aiarticle.util.GsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageAgentTest {

    @Mock
    private ImageSearchService imageSearchService;

    @Mock
    private ImageCloudStore imageCloudStore;

    @Mock
    private Consumer<String> streamHandler;

    @InjectMocks
    private ImageAgent imageAgent;

    @Test
    void generate_fallsBackToPexelsWhenMermaidFails() {
        ImageRequirement cover = requirement(1, "cover", "", "AI_GENERATE", "future office", "主题信息图");
        ImageRequirement section = requirement(2, "section", "第一章", "MERMAID", "pipeline", "flowchart LR\n  A --> B");
        ArticleState state = new ArticleState();
        state.setImageRequirements(List.of(cover, section));

        when(imageSearchService.searchImage(request(1, "cover", "", "AI_GENERATE", "future office", "主题信息图")))
                .thenReturn(ImagePayload.base64("x".getBytes(), "image/svg+xml", "svg", "svg-diagram"));
        when(imageSearchService.searchImage(request(2, "section", "第一章", "MERMAID", "pipeline",
                "flowchart LR\n  A --> B")))
                .thenReturn(null);
        when(imageSearchService.searchImage(request(2, "section", "第一章", "PEXELS", "pipeline",
                "flowchart LR\n  A --> B")))
                .thenReturn(ImagePayload.url("https://img/pexels-fallback.jpg"));
        when(imageCloudStore.toPublicUrl(any())).thenAnswer(invocation -> {
            ImagePayload payload = invocation.getArgument(0);
            if (payload == null) {
                return null;
            }
            return payload.getContent() != null && payload.getContent().startsWith("http")
                    ? payload.getContent()
                    : "https://cos/ai.svg";
        });

        ArticleState result = imageAgent.generate(state, streamHandler);

        assertSame(state, result);
        assertEquals("https://cos/ai.svg", state.getCoverImage());
        assertEquals("AI_GENERATE", state.getImages().get(0).getMethod());
        assertEquals("PEXELS", state.getImages().get(1).getMethod());
        assertEquals("https://img/pexels-fallback.jpg", state.getImages().get(1).getUrl());

        InOrder order = inOrder(imageSearchService, streamHandler);
        order.verify(imageSearchService).searchImage(request(1, "cover", "", "AI_GENERATE", "future office", "主题信息图"));
        order.verify(streamHandler).accept(SseMessageTypeEnum.IMAGE_COMPLETE.getStreamingPrefix()
                + GsonUtils.toJson(state.getImages().get(0)));
        order.verify(imageSearchService).searchImage(request(2, "section", "第一章", "MERMAID", "pipeline",
                "flowchart LR\n  A --> B"));
        order.verify(imageSearchService).searchImage(request(2, "section", "第一章", "PEXELS", "pipeline",
                "flowchart LR\n  A --> B"));
        order.verify(streamHandler).accept(SseMessageTypeEnum.IMAGE_COMPLETE.getStreamingPrefix()
                + GsonUtils.toJson(state.getImages().get(1)));
    }

    @Test
    void generate_usesStaticFallbackWhenPexelsAlsoFails() {
        ArticleState state = new ArticleState();
        state.setImageRequirements(List.of(requirement(1, "cover", "", "AI_GENERATE", "broken", "说明")));
        when(imageSearchService.searchImage(request(1, "cover", "", "AI_GENERATE", "broken", "说明")))
                .thenThrow(new RuntimeException("模型不可用"));
        when(imageSearchService.searchImage(request(1, "cover", "", "PEXELS", "broken", "说明")))
                .thenReturn(null);
        when(imageCloudStore.toPublicUrl(any())).thenReturn(null);
        when(imageSearchService.getFallbackImageUrl(1)).thenReturn("https://img/fallback.jpg");

        imageAgent.generate(state, streamHandler);

        assertEquals("https://img/fallback.jpg", state.getCoverImage());
        assertEquals("PEXELS", state.getImages().get(0).getMethod());
        verify(streamHandler).accept(SseMessageTypeEnum.IMAGE_COMPLETE.getStreamingPrefix()
                + GsonUtils.toJson(state.getImages().get(0)));
    }

    @Test
    void generate_rejectsEmptyRequirements() {
        ArticleState state = new ArticleState();
        state.setImageRequirements(List.of());
        assertThrows(BusinessException.class, () -> imageAgent.generate(state, streamHandler));
    }

    @Test
    void generate_rejectsNullStreamHandler() {
        ArticleState state = new ArticleState();
        state.setImageRequirements(List.of(requirement(1, "cover", "", "AI_GENERATE", "cover", "主题")));
        assertThrows(BusinessException.class, () -> imageAgent.generate(state, null));
    }

    private ImageSearchRequest request(
            int position, String type, String sectionTitle, String method, String keywords, String prompt) {
        return ImageSearchRequest.builder()
                .position(position)
                .type(type)
                .sectionTitle(sectionTitle)
                .method(method)
                .keywords(keywords)
                .prompt(prompt)
                .build();
    }

    private ImageRequirement requirement(
            int position, String type, String sectionTitle, String method, String keywords, String prompt) {
        ImageRequirement requirement = new ImageRequirement();
        requirement.setPosition(position);
        requirement.setType(type);
        requirement.setSectionTitle(sectionTitle);
        requirement.setMethod(method);
        requirement.setKeywords(keywords);
        requirement.setPrompt(prompt);
        return requirement;
    }
}
