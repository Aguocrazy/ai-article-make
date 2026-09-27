package com.aiarticle.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章列表项（不含正文）。
 */
@Data
public class ArticleVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String taskId;

    private String topic;

    private String articleType;

    private String writingTone;

    private Integer wordCount;

    private String audience;

    private String extraRequirement;

    private String mainTitle;

    private String subTitle;

    private String coverImage;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime completedTime;

    private LocalDateTime updateTime;
}
