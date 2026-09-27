package com.aiarticle.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 文章详情（含正文与配图）。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ArticleDetailVO extends ArticleVO {

    @Serial
    private static final long serialVersionUID = 1L;

    private String outline;

    private String content;

    private String fullContent;

    private String images;

    private String errorMessage;
}
