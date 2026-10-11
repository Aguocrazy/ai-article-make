package com.aiarticle.service.image;

import com.aiarticle.enums.ImageMethodEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompositeImageSearchServiceTest {

    @Mock
    private PexelsImageSearchService pexelsImageSearchService;

    @Mock
    private MermaidService mermaidService;

    @Mock
    private SvgDiagramService svgDiagramService;

    private CompositeImageSearchService composite;

    @BeforeEach
    void setUp() {
        composite = new CompositeImageSearchService(
                pexelsImageSearchService, mermaidService, svgDiagramService);
    }

    @Test
    void routesMermaidAndAiGenerateByMethod() {
        ImageSearchRequest mermaidReq = ImageSearchRequest.builder()
                .method(ImageMethodEnum.MERMAID.getValue())
                .prompt("flowchart LR\n  A --> B")
                .build();
        ImageSearchRequest aiReq = ImageSearchRequest.builder()
                .method(ImageMethodEnum.AI_GENERATE.getValue())
                .prompt("对比两种架构")
                .build();
        ImagePayload mermaidPayload = ImagePayload.base64("x".getBytes(), "image/svg+xml", "svg", "mermaid");
        ImagePayload aiPayload = ImagePayload.base64("y".getBytes(), "image/svg+xml", "svg", "svg-diagram");
        when(mermaidService.searchImage(mermaidReq)).thenReturn(mermaidPayload);
        when(svgDiagramService.searchImage(aiReq)).thenReturn(aiPayload);

        assertSame(mermaidPayload, composite.searchImage(mermaidReq));
        assertSame(aiPayload, composite.searchImage(aiReq));
        verify(mermaidService).searchImage(mermaidReq);
        verify(svgDiagramService).searchImage(aiReq);
        verifyNoInteractions(pexelsImageSearchService);
    }

    @Test
    void routesBlankToAiGenerateAndUnknownToPexels() {
        ImageSearchRequest blank = ImageSearchRequest.builder().keywords("office").build();
        ImageSearchRequest unknown = ImageSearchRequest.builder()
                .method("UNKNOWN")
                .keywords("office")
                .build();
        ImagePayload aiPayload = ImagePayload.base64("z".getBytes(), "image/svg+xml", "svg", "svg-diagram");
        ImagePayload pexelsPayload = ImagePayload.url("https://img/a.jpg");
        when(svgDiagramService.searchImage(blank)).thenReturn(aiPayload);
        when(pexelsImageSearchService.searchImage(unknown)).thenReturn(pexelsPayload);

        assertSame(aiPayload, composite.searchImage(blank));
        assertSame(pexelsPayload, composite.searchImage(unknown));
        verifyNoInteractions(mermaidService);
    }

    @Test
    void fallbackDelegatesToPexels() {
        when(pexelsImageSearchService.getFallbackImageUrl(3)).thenReturn("https://fallback/3.jpg");
        when(pexelsImageSearchService.getSearchMethod()).thenReturn("PEXELS");

        assertEquals("https://fallback/3.jpg", composite.getFallbackImageUrl(3));
        assertEquals("PEXELS", composite.getSearchMethod());
    }
}
