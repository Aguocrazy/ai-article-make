package com.aiarticle.model.dto.article;

import com.aiarticle.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 当前用户分页查询自己的文章。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ArticleQueryRequest extends PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 选题关键字（模糊）
     */
    private String topic;

    /**
     * 状态：PENDING / PROCESSING / COMPLETED / FAILED
     */
    private String status;
}
