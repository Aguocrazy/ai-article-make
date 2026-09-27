package com.aiarticle.util;

import com.aiarticle.model.state.ArticleState;
import com.aiarticle.model.state.ArticleState.TitleResult;
import org.springframework.util.StringUtils;

/**
 * 把选题与创作设定填进提示词占位符。
 */
public final class WritingPromptBinder {

    public static final String DEFAULT_ARTICLE_TYPE = "深度解读";
    public static final String DEFAULT_WRITING_TONE = "专业严谨";
    public static final int DEFAULT_WORD_COUNT = 1000;
    public static final String DEFAULT_AUDIENCE = "通用读者";
    public static final String DEFAULT_EXTRA = "无";

    private WritingPromptBinder() {
    }

    public static String fill(String template, ArticleState state) {
        TitleResult title = state == null ? null : state.getTitle();
        return template
                .replace("{topic}", text(state == null ? null : state.getTopic(), ""))
                .replace("{mainTitle}", title == null ? "" : text(title.getMainTitle(), ""))
                .replace("{subTitle}", title == null ? "" : text(title.getSubTitle(), ""))
                .replace("{articleType}", articleType(state))
                .replace("{writingTone}", writingTone(state))
                .replace("{wordCount}", String.valueOf(wordCount(state)))
                .replace("{audience}", audience(state))
                .replace("{extraRequirement}", extraRequirement(state))
                .replace("{outline}", state == null || state.getOutline() == null
                        ? "" : GsonUtils.toJson(state.getOutline()))
                .replace("{content}", text(state == null ? null : state.getContent(), ""));
    }

    public static String articleType(ArticleState state) {
        return text(state == null ? null : state.getArticleType(), DEFAULT_ARTICLE_TYPE);
    }

    public static String writingTone(ArticleState state) {
        return text(state == null ? null : state.getWritingTone(), DEFAULT_WRITING_TONE);
    }

    public static int wordCount(ArticleState state) {
        Integer value = state == null ? null : state.getWordCount();
        if (value == null || value < 200 || value > 8000) {
            return DEFAULT_WORD_COUNT;
        }
        return value;
    }

    public static String audience(ArticleState state) {
        return text(state == null ? null : state.getAudience(), DEFAULT_AUDIENCE);
    }

    public static String extraRequirement(ArticleState state) {
        return text(state == null ? null : state.getExtraRequirement(), DEFAULT_EXTRA);
    }

    private static String text(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
