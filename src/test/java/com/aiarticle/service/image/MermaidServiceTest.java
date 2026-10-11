package com.aiarticle.service.image;

import com.aiarticle.config.MermaidConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MermaidServiceTest {

    @Test
    void searchImage_returnsNullWhenSourceEmpty() {
        MermaidService service = new MermaidService(new MermaidConfig());
        assertNull(service.searchImage(ImageSearchRequest.builder().build()));
        assertEquals("MERMAID", service.getSearchMethod());
        assertNull(service.getFallbackImageUrl(1));
    }
}
