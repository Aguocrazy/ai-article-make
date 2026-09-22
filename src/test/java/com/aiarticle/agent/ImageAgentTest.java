package com.aiarticle.agent;

import com.aiarticle.enums.SseMessageTypeEnum;
import com.aiarticle.exception.BusinessException;
import com.aiarticle.model.state.ArticleState;
import com.aiarticle.model.state.ArticleState.ImageRequirement;
import com.aiarticle.model.state.ArticleState.ImageResult;
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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageAgentTest {

    @Mock
    private ImageSearchService imageSearchService;

    @Mock
    private Consumer<String> streamHandler;

    @InjectMocks
    private ImageAgent imageAgent;

    @Test
    void generate_searchesSequentially_fallsBackAndPushesEachImage() {
        ImageRequirement cover = requirement(1, "cover", "", "future office");
        ImageRequirement section = requirement(2, "section", "第一章", "teamwork");
        ArticleState state = new ArticleState();
        state.setImageRequirements(List.of(cover, section));

        when(imageSearchService.searchImage("future office")).thenReturn("https://img/cover.jpg");
        when(imageSearchService.searchImage("teamwork")).thenReturn(null);
        when(imageSearchService.getSearchMethod()).thenReturn("PEXELS");
        when(imageSearchService.getFallbackImageUrl(2)).thenReturn("https://img/fallback.jpg");

        ArticleState result = imageAgent.generate(state, streamHandler);

        assertSame(state, result);
        assertEquals("https://img/cover.jpg", state.getCoverImage());
        assertEquals(2, state.getImages().size());
        assertEquals("PEXELS", state.getImages().get(0).getMethod());
        assertEquals("FALLBACK", state.getImages().get(1).getMethod());
        assertEquals("https://img/fallback.jpg", state.getImages().get(1).getUrl());

        InOrder order = inOrder(imageSearchService, streamHandler);
        order.verify(imageSearchService).searchImage("future office");
        order.verify(streamHandler).accept(SseMessageTypeEnum.IMAGE_COMPLETE.getStreamingPrefix()
                + GsonUtils.toJson(state.getImages().get(0)));
        order.verify(imageSearchService).searchImage("teamwork");
        order.verify(imageSearchService).getFallbackImageUrl(2);
        order.verify(streamHandler).accept(SseMessageTypeEnum.IMAGE_COMPLETE.getStreamingPrefix()
                + GsonUtils.toJson(state.getImages().get(1)));
    }

    @Test
    void generate_usesFallbackWhenSearchThrows() {
        ArticleState state = new ArticleState();
        state.setImageRequirements(List.of(requirement(1, "cover", "", "broken")));
        when(imageSearchService.searchImage("broken")).thenThrow(new RuntimeException("图库不可用"));
        when(imageSearchService.getFallbackImageUrl(1)).thenReturn("https://img/fallback.jpg");

        imageAgent.generate(state, streamHandler);

        assertEquals("https://img/fallback.jpg", state.getCoverImage());
        assertEquals("FALLBACK", state.getImages().get(0).getMethod());
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
        state.setImageRequirements(List.of(requirement(1, "cover", "", "cover")));
        assertThrows(BusinessException.class, () -> imageAgent.generate(state, null));
    }

    private ImageRequirement requirement(int position, String type, String sectionTitle, String keywords) {
        ImageRequirement requirement = new ImageRequirement();
        requirement.setPosition(position);
        requirement.setType(type);
        requirement.setSectionTitle(sectionTitle);
        requirement.setKeywords(keywords);
        return requirement;
    }
}
