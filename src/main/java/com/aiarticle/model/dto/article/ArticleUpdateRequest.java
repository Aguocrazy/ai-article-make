package com.aiarticle.model.dto.article;

/**
 * 更新自己的文章正文。
 *
 * @param id      文章 ID
 * @param topic   选题（可选）
 * @param content 正文 Markdown
 */
public record ArticleUpdateRequest(Long id, String topic, String content) {
}
