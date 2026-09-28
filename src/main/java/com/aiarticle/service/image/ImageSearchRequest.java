package com.aiarticle.service.image;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

/**
 * 配图请求。图库检索用 keywords，AI 生图用 prompt，两者可并存。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageSearchRequest {

    private Integer position;

    private String type;

    private String sectionTitle;

    /**
     * 图库检索关键词。
     */
    private String keywords;

    /**
     * AI 生图提示词。
     */
    private String prompt;

    /**
     * 期望的配图方式，对应 {@link com.aiarticle.enums.ImageMethodEnum}。
     */
    private String method;

    /**
     * 实际用于检索或生图的文本：优先 prompt，否则 keywords。
     */
    public String resolveQuery() {
        if (StringUtils.hasText(prompt)) {
            return prompt.trim();
        }
        return StringUtils.hasText(keywords) ? keywords.trim() : null;
    }
}
