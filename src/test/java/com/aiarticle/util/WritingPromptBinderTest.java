package com.aiarticle.util;

import com.aiarticle.model.state.ArticleState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WritingPromptBinderTest {

    @Test
    void fill_usesDefaultsWhenSettingsMissing() {
        ArticleState state = new ArticleState();
        state.setTopic("选题");

        String filled = WritingPromptBinder.fill(
                "{topic}|{articleType}|{writingTone}|{wordCount}|{audience}|{extraRequirement}",
                state);

        assertEquals("选题|深度解读|专业严谨|1000|通用读者|无", filled);
    }

    @Test
    void wordCount_fallsBackOutsideRange() {
        ArticleState state = new ArticleState();
        state.setWordCount(50);
        assertEquals(1000, WritingPromptBinder.wordCount(state));
        state.setWordCount(9000);
        assertEquals(1000, WritingPromptBinder.wordCount(state));
        state.setWordCount(2000);
        assertEquals(2000, WritingPromptBinder.wordCount(state));
    }
}
