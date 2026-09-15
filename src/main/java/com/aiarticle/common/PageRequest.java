package com.aiarticle.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 分页请求
 */
@Data
public class PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页号（从 1 开始）
     */
    private int current = 1;

    /**
     * 页面大小（默认 10）
     */
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（ascend / descend）
     */
    private String sortOrder;
}