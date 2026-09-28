package com.aiarticle.service.image;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ImageSearchRequestTest {

    @Test
    void resolveQuery_prefersPromptThenKeywords() {
        assertEquals("draw a city", ImageSearchRequest.builder().prompt("draw a city").keywords("city").build().resolveQuery());
        assertEquals("city", ImageSearchRequest.builder().keywords("city").build().resolveQuery());
        assertNull(ImageSearchRequest.builder().build().resolveQuery());
    }
}
