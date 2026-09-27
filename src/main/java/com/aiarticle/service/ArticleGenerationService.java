package com.aiarticle.service;

import com.aiarticle.model.dto.article.ArticleCreateRequest;
import com.aiarticle.model.vo.ArticleTaskVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 文章异步生成服务。
 */
public interface ArticleGenerationService {

    /**
     * 创建文章生成任务。
     *
     * @param request 选题与创作设定
     * @param userId  当前用户 ID
     * @return 任务信息
     */
    ArticleTaskVO create(ArticleCreateRequest request, long userId);

    /**
     * 订阅文章生成进度。
     *
     * @param taskId 任务 ID
     * @param userId 当前用户 ID
     * @return SSE 连接
     */
    SseEmitter subscribe(String taskId, long userId);
}
