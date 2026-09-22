package com.aiarticle.model.vo;

import com.aiarticle.enums.SseMessageTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * SSE 事件的稳定传输信封。
 */
@Getter
@AllArgsConstructor
public class SseEventVO {

    /**
     * 文章生成任务 ID
     */
    private final String taskId;

    /**
     * 事件类型
     */
    private final String type;

    /**
     * 事件数据
     */
    private final Object data;

    /**
     * 事件创建时间戳（毫秒）
     */
    private final long timestamp;

    /**
     * 创建 SSE 事件。
     *
     * @param taskId 任务 ID
     * @param type   消息类型
     * @param data   事件数据
     * @return SSE 事件
     */
    public static SseEventVO of(String taskId, SseMessageTypeEnum type, Object data) {
        return new SseEventVO(taskId, type.getValue(), data, System.currentTimeMillis());
    }
}
