package com.aiarticle.enums;

import lombok.Getter;

/**
 * 配图方式。
 * <p>
 * 扩展新来源时增加枚举值即可：
 * {@code aiGenerated} 为 true 时用 prompt 生图，否则用 keywords 检索；
 * {@code fallback} 表示检索失败后的降级方案。
 */
@Getter
public enum ImageMethodEnum {

    PEXELS("PEXELS", "Pexels 图库", false, false),
    NANO_BANANA("NANO_BANANA", "Nano Banana AI 生图", true, false),
    MERMAID("MERMAID", "Mermaid 流程图生成", true, false),
    ICONIFY("ICONIFY", "Iconify 图标库", false, false),
    EMOJI_PACK("EMOJI_PACK", "表情包检索", false, false),
    SVG_DIAGRAM("SVG_DIAGRAM", "SVG 概念示意图", true, false),
    PICSUM("PICSUM", "Picsum 随机图片", false, true);

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

    public static ImageMethodEnum getDefaultSearchMethod() {
        return PEXELS;
    }

    public static ImageMethodEnum getDefaultAiMethod() {
        return NANO_BANANA;
    }

    public static ImageMethodEnum getFallbackMethod() {
        return PICSUM;
    }
}
