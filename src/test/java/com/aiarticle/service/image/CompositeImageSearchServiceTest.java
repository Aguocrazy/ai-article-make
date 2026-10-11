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
    private IconifyService iconifyService;

    @Mock
    private SvgDiagramService svgDiagramService;

    private CompositeImageSearchService composite;

    @BeforeEach
    void setUp() {
        composite = new CompositeImageSearchService(
                pexelsImageSearchService, mermaidService, iconifyService, svgDiagramService);
    }

    @Test
    void routesMermaidByMethod() {
        ImageSearchRequest request = ImageSearchRequest.builder()
                .method(ImageMethodEnum.MERMAID.getValue())
                .prompt("flowchart LR\n  A --> B")
                .build();
        ImagePayload payload = ImagePayload.base64("x".getBytes(), "image/svg+xml", "svg", "mermaid");
        when(mermaidService.searchImage(request)).thenReturn(payload);

        assertSame(payload, composite.searchImage(request));
        verify(mermaidService).searchImage(request);
        verifyNoInteractions(pexelsImageSearchService, iconifyService, svgDiagramService);
    }

    @Test
    void routesDefaultAndUnknownToPexels() {
        ImageSearchRequest blank = ImageSearchRequest.builder().keywords("office").build();
        ImageSearchRequest unknown = ImageSearchRequest.builder()
                .method("NANO_BANANA")
                .keywords("office")
                .build();
        ImagePayload payload = ImagePayload.url("https://img/a.jpg");
        when(pexelsImageSearchService.searchImage(blank)).thenReturn(payload);
        when(pexelsImageSearchService.searchImage(unknown)).thenReturn(payload);

        assertSame(payload, composite.searchImage(blank));
        assertSame(payload, composite.searchImage(unknown));
        verifyNoInteractions(mermaidService, iconifyService, svgDiagramService);
    }

    @Test
    void fallbackDelegatesToPexels() {
        when(pexelsImageSearchService.getFallbackImageUrl(3)).thenReturn("https://fallback/3.jpg");
        when(pexelsImageSearchService.getSearchMethod()).thenReturn("PEXELS");

        assertEquals("https://fallback/3.jpg", composite.getFallbackImageUrl(3));
        assertEquals("PEXELS", composite.getSearchMethod());
    }
}
