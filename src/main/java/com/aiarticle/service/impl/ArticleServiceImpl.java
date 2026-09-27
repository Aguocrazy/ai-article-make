package com.aiarticle.service.impl;

import cn.hutool.core.util.StrUtil;
import com.aiarticle.exception.BusinessException;
import com.aiarticle.exception.ErrorCode;
import com.aiarticle.mapper.ArticleMapper;
import com.aiarticle.model.dto.article.ArticleQueryRequest;
import com.aiarticle.model.entity.Article;
import com.aiarticle.model.vo.ArticleDetailVO;
import com.aiarticle.model.vo.ArticleVO;
import com.aiarticle.service.ArticleService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.aiarticle.model.entity.table.ArticleTableDef.ARTICLE;

@Service
public class ArticleServiceImpl implements ArticleService {

    private final ArticleMapper articleMapper;

    public ArticleServiceImpl(ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    @Override
    public Page<ArticleVO> listMine(ArticleQueryRequest queryRequest, long userId) {
        if (queryRequest == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = queryRequest.getCurrent();
        long pageSize = queryRequest.getPageSize();
        if (current < 1 || pageSize < 1 || pageSize > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分页参数错误");
        }

        QueryWrapper queryWrapper = QueryWrapper.create().where(ARTICLE.USER_ID.eq(userId));
        if (StrUtil.isNotBlank(queryRequest.getStatus())) {
            queryWrapper.and(ARTICLE.STATUS.eq(queryRequest.getStatus().trim()));
        }
        if (StrUtil.isNotBlank(queryRequest.getTopic())) {
            queryWrapper.and(ARTICLE.TOPIC.like(queryRequest.getTopic().trim()));
        }
        queryWrapper.orderBy(ARTICLE.CREATE_TIME.desc());

        Page<Article> articlePage = articleMapper.paginate(current, pageSize, queryWrapper);
        List<ArticleVO> records = new ArrayList<>();
        for (Article article : articlePage.getRecords()) {
            records.add(toListVo(article));
        }
        return new Page<>(records, articlePage.getPageNumber(), articlePage.getPageSize(), articlePage.getTotalRow());
    }

    @Override
    public ArticleDetailVO getMine(long id, long userId) {
        if (id <= 0 || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Article article = articleMapper.selectOneById(id);
        if (article == null || !Objects.equals(article.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return toDetailVo(article);
    }

    private ArticleVO toListVo(Article article) {
        ArticleVO vo = new ArticleVO();
        BeanUtils.copyProperties(article, vo);
        return vo;
    }

    private ArticleDetailVO toDetailVo(Article article) {
        ArticleDetailVO vo = new ArticleDetailVO();
        BeanUtils.copyProperties(article, vo);
        return vo;
    }
}
