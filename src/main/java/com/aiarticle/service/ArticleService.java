package com.aiarticle.service;

import com.aiarticle.model.dto.article.ArticleQueryRequest;
import com.aiarticle.model.vo.ArticleDetailVO;
import com.aiarticle.model.vo.ArticleVO;
import com.mybatisflex.core.paginate.Page;

/**
 * 当前用户的文章查询。
 */
public interface ArticleService {

    /**
     * 分页查询当前用户的文章。
     */
    Page<ArticleVO> listMine(ArticleQueryRequest queryRequest, long userId);

    /**
     * 查询当前用户的一篇文章详情。
     */
    ArticleDetailVO getMine(long id, long userId);
}
