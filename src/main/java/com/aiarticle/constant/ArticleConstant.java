package com.aiarticle.constant;

/**
 * 文章生成状态，对应 article.status。
 */
public interface ArticleConstant {

    /**
     * 待处理：已提交选题，Worker 尚未开始
     */
    String STATUS_PENDING = "PENDING";

    /**
     * 生成中：智能体链路执行中
     */
    String STATUS_PROCESSING = "PROCESSING";

    /**
     * 已完成：合成落库成功
     */
    String STATUS_COMPLETED = "COMPLETED";

    /**
     * 失败：生成中断，详见 errorMessage
     */
    String STATUS_FAILED = "FAILED";
}
