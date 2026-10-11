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
        assertEquals(ImageMethodEnum.AI_GENERATE, ImageMethodEnum.getByValue("AI_GENERATE"));
        assertNull(ImageMethodEnum.getByValue(null));
        assertNull(ImageMethodEnum.getByValue("UNKNOWN"));
    }

    @Test
    void defaultsPreferAiAndPexelsFallback() {
        assertEquals(ImageMethodEnum.AI_GENERATE, ImageMethodEnum.getDefaultSearchMethod());
        assertEquals(ImageMethodEnum.AI_GENERATE, ImageMethodEnum.getDefaultAiMethod());
        assertEquals(ImageMethodEnum.PEXELS, ImageMethodEnum.getFallbackMethod());
        assertTrue(ImageMethodEnum.AI_GENERATE.isAiGenerated());
        assertTrue(ImageMethodEnum.MERMAID.isAiGenerated());
        assertFalse(ImageMethodEnum.PEXELS.isAiGenerated());
        assertTrue(ImageMethodEnum.PEXELS.isFallback());
    }
}
