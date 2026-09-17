package com.aiarticle.service;

import com.aiarticle.model.dto.user.UserAddRequest;
import com.aiarticle.model.dto.user.UserLoginRequest;
import com.aiarticle.model.dto.user.UserQueryRequest;
import com.aiarticle.model.dto.user.UserRegisterRequest;
import com.aiarticle.model.entity.User;
import com.aiarticle.model.vo.LoginUserVO;
import com.aiarticle.model.vo.UserVO;
import com.mybatisflex.core.paginate.Page;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 用户服务
 */
public interface UserService {

    /**
     * 用户注册
     *
     * @return 新用户 id
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 用户登录（Session 认证）
     */
    LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request);

    /**
     * 获取当前登录用户
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取当前登录用户（视图对象，脱敏）
     */
    LoginUserVO getLoginUserVO(HttpServletRequest request);

    /**
     * 用户注销
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 管理员创建用户
     *
     * @return 新用户 id
     */
    long addUser(UserAddRequest userAddRequest);

    /**
     * 更新用户
     */
    boolean update(User user);

    /**
     * 根据 id 删除用户（逻辑删除）
     */
    boolean removeById(long id);

    /**
     * 根据用户名或账号搜索（模糊，用于分页查询）
     */
    Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest);
}
