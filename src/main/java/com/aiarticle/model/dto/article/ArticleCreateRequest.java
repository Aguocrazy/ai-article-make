package com.aiarticle.model.dto.article;

/**
 * 文章生成请求。
 *
 * @param topic            文章选题
 * @param articleType      文章类型
 * @param writingTone      写作语气
 * @param wordCount        目标字数
 * @param audience         目标读者
 * @param extraRequirement 补充写作要求
 */
public record ArticleCreateRequest(
        String topic,
        String articleType,
        String writingTone,
        Integer wordCount,
        String audience,
        String extraRequirement
) {
}
