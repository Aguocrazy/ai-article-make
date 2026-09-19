package com.aiarticle.mapper;

import com.aiarticle.model.entity.Article;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章表数据访问：增删改查走 MyBatis-Flex {@link BaseMapper}，
 * 查询条件可用 {@code ArticleTableDef.ARTICLE}。
 */
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

}
