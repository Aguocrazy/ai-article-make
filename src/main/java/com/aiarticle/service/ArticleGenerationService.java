package com.aiarticle.service;

import com.aiarticle.model.vo.ArticleTaskVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 文章异步生成服务。
 */
public interface ArticleGenerationService {

    /**
     * 创建文章生成任务。
     *
     * @param topic  文章选题
     * @param userId 当前用户 ID
     * @return 任务信息
     */
    ArticleTaskVO create(String topic, long userId);

    /**
     * 订阅文章生成进度。
     *
     * @param taskId 任务 ID
     * @param userId 当前用户 ID
     * @return SSE 连接
     */
    SseEmitter subscribe(String taskId, long userId);
}
