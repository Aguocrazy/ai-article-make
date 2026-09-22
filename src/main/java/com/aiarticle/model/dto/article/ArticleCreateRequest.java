package com.aiarticle.model.dto.article;

/**
 * 文章生成请求。
 *
 * @param topic 文章选题
 */
public record ArticleCreateRequest(String topic) {
}
