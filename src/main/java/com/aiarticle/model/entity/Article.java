package com.aiarticle.model.entity;

import com.aiarticle.constant.ArticleConstant;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章实体，对应表 article。
 * <p>
 * {@code camelToUnderline = false} 必须保留：库字段本身是驼峰（如 taskId、userId）。
 * MyBatis-Flex 默认会把 Java 驼峰转成下划线去拼 SQL（taskId → task_id），
 * 不加该配置会报 column 不存在。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "article", camelToUnderline = false)
public class Article implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花算法生成）
     */
    @Id(keyType = KeyType.Generator, value = "snowFlakeId")
    private Long id;

    /**
     * 任务 ID（UUID），唯一
     */
    private String taskId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 选题
     */
    private String topic;

    /**
     * 主标题
     */
    private String mainTitle;

    /**
     * 副标题
     */
    private String subTitle;

    /**
     * 大纲（JSON 格式）
     */
    private String outline;

    /**
     * 正文（Markdown 格式）
     */
    private String content;

    /**
     * 完整图文（Markdown 格式，含配图）
     */
    private String fullContent;

    /**
     * 封面图 URL
     */
    private String coverImage;

    /**
     * 配图列表（JSON 数组）
     */
    private String images;

    /**
     * 状态：PENDING / PROCESSING / COMPLETED / FAILED，默认 PENDING
     */
    @Builder.Default
    private String status = ArticleConstant.STATUS_PENDING;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;

    /**
     * 完成时间
     */
    private LocalDateTime completedTime;

    /**
     * 更新时间
     */
    @Column(onInsertValue = "now()", onUpdateValue = "now()")
    private LocalDateTime updateTime;

    /**
     * 是否删除（逻辑删除，0 未删 / 1 已删）
     */
    @Column(isLogicDelete = true)
    private Integer isDelete;
}
