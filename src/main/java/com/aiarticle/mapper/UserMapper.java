package com.aiarticle.mapper;

import com.aiarticle.model.entity.User;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据库操作
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}