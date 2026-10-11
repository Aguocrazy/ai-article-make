package com.aiarticle.service.image;

import com.aiarticle.constant.PromptConstant;
import com.aiarticle.util.AiModelClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SvgDiagramServiceTest {

    @Mock
    private AiModelClient aiModelClient;

    @InjectMocks
    private SvgDiagramService service;

    @Test
    void searchImage_callsLlmAndReturnsBase64() {
        when(aiModelClient.callLlm(any())).thenReturn("""
                ```svg
                <?xml version="1.0" encoding="UTF-8"?>
                <svg viewBox="0 0 800 600"></svg>
                ```
                """);

        ImagePayload payload = service.searchImage(ImageSearchRequest.builder().prompt("解释微服务拆分").build());

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiModelClient).callLlm(promptCaptor.capture());
        assertEquals(PromptConstant.SVG_DIAGRAM_GENERATION_PROMPT.replace("{requirement}", "解释微服务拆分"),
                promptCaptor.getValue());
        assertEquals(ImagePayloadType.BASE64, payload.getType());
        assertEquals("svg", payload.getExtension());
        assertEquals("svg-diagram", payload.getKeyPrefix());
        String decoded = new String(Base64.getDecoder().decode(payload.getContent()), StandardCharsets.UTF_8);
        assertTrue(decoded.contains("<svg"));
        assertTrue(decoded.contains("width=\"800\""));
        assertTrue(decoded.contains("height=\"600\""));
        assertEquals("AI_GENERATE", service.getSearchMethod());
    }

    @Test
    void normalizeSvg_addsWidthHeightFromViewBox() {
        String normalized = SvgDiagramService.normalizeSvg("<svg viewBox=\"0 0 800 600\"></svg>");
        assertTrue(normalized.contains("width=\"800\""));
        assertTrue(normalized.contains("height=\"600\""));
    }

    @Test
    void searchImage_returnsNullWhenQueryEmpty() {
        assertNull(service.searchImage(ImageSearchRequest.builder().build()));
    }
}
