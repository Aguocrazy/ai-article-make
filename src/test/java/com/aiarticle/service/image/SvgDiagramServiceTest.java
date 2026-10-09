package com.aiarticle.service.image;

import com.aiarticle.constant.PromptConstant;
import com.aiarticle.util.AiModelClient;
import com.aiarticle.util.CosFileClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SvgDiagramServiceTest {

    @Mock
    private AiModelClient aiModelClient;

    @Mock
    private CosFileClient cosFileClient;

    @InjectMocks
    private SvgDiagramService service;

    @Test
    void searchImage_callsLlmAndUploadsSvg() {
        when(aiModelClient.callLlm(any())).thenReturn("""
                ```svg
                <?xml version="1.0" encoding="UTF-8"?>
                <svg viewBox="0 0 800 600"></svg>
                ```
                """);
        when(cosFileClient.upload(any(byte[].class), any(), eq("image/svg+xml")))
                .thenReturn(new CosFileClient.CosUploadResult("svg/a.svg", "https://cos/a.svg", "etag"));

        String url = service.searchImage(ImageSearchRequest.builder().prompt("解释微服务拆分").build());

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiModelClient).callLlm(promptCaptor.capture());
        assertTrue(promptCaptor.getValue().contains("解释微服务拆分"));
        assertTrue(promptCaptor.getValue().contains("直接返回完整的 SVG XML 代码"));
        assertEquals(PromptConstant.SVG_DIAGRAM_GENERATION_PROMPT.replace("{requirement}", "解释微服务拆分"),
                promptCaptor.getValue());
        assertEquals("https://cos/a.svg", url);
        assertEquals("SVG_DIAGRAM", service.getSearchMethod());
    }

    @Test
    void searchImage_returnsNullWhenQueryEmpty() {
        assertNull(service.searchImage(ImageSearchRequest.builder().build()));
    }
}
