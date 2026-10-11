package com.aiarticle.enums;

import lombok.Getter;

/**
 * 配图方式。
 * <p>
 * 优先 {@link #AI_GENERATE}、{@link #MERMAID}；{@link #PEXELS} 作兜底检索。
 */
@Getter
public enum ImageMethodEnum {

    AI_GENERATE("AI_GENERATE", "大模型 AI 生图", true, false),
    MERMAID("MERMAID", "Mermaid 流程图生成", true, false),
    PEXELS("PEXELS", "Pexels 图库（兜底）", false, true);

    private final String value;

    private final String description;

    private final boolean aiGenerated;

    private final boolean fallback;

    ImageMethodEnum(String value, String description, boolean aiGenerated, boolean fallback) {
        this.value = value;
        this.description = description;
        this.aiGenerated = aiGenerated;
        this.fallback = fallback;
    }

    public static ImageMethodEnum getByValue(String value) {
        if (value == null) {
            return null;
        }
        for (ImageMethodEnum method : values()) {
            if (method.value.equals(value)) {
                return method;
            }
        }
        return null;
    }

    /**
     * 未指定 method 时的默认策略：优先 AI 生图。
     */
    public static ImageMethodEnum getDefaultSearchMethod() {
        return AI_GENERATE;
    }

    public static ImageMethodEnum getDefaultAiMethod() {
        return AI_GENERATE;
    }

    /**
     * 主策略失败时的兜底：Pexels 检索 / 固定图。
     */
    public static ImageMethodEnum getFallbackMethod() {
        return PEXELS;
    }
}
