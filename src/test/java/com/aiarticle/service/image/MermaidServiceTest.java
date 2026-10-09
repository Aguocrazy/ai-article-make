package com.aiarticle.service.image;

import com.aiarticle.config.MermaidConfig;
import com.aiarticle.util.CosFileClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class MermaidServiceTest {

    @Test
    void searchImage_returnsNullWhenSourceEmpty() {
        MermaidService service = new MermaidService(new MermaidConfig(), mock(CosFileClient.class));
        assertNull(service.searchImage(ImageSearchRequest.builder().build()));
        assertEquals("MERMAID", service.getSearchMethod());
        assertNull(service.getFallbackImageUrl(1));
    }
}
