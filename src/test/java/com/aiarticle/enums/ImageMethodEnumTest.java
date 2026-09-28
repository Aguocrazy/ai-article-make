package com.aiarticle.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageMethodEnumTest {

    @Test
    void getByValue_returnsMatchingEnum() {
        assertEquals(ImageMethodEnum.PEXELS, ImageMethodEnum.getByValue("PEXELS"));
        assertNull(ImageMethodEnum.getByValue(null));
        assertNull(ImageMethodEnum.getByValue("UNKNOWN"));
    }

    @Test
    void defaultsMatchSearchAiAndFallback() {
        assertEquals(ImageMethodEnum.PEXELS, ImageMethodEnum.getDefaultSearchMethod());
        assertEquals(ImageMethodEnum.NANO_BANANA, ImageMethodEnum.getDefaultAiMethod());
        assertEquals(ImageMethodEnum.PICSUM, ImageMethodEnum.getFallbackMethod());
        assertFalse(ImageMethodEnum.PEXELS.isAiGenerated());
        assertTrue(ImageMethodEnum.NANO_BANANA.isAiGenerated());
        assertTrue(ImageMethodEnum.PICSUM.isFallback());
    }
}
