package com.aiarticle.controller;

import cn.hutool.core.bean.BeanUtil;
import com.aiarticle.common.BaseResponse;
import com.aiarticle.common.DeleteRequest;
import com.aiarticle.common.ResultUtils;
import com.aiarticle.constant.UserConstant;
import com.aiarticle.exception.BusinessException;
import com.aiarticle.exception.ErrorCode;
import com.aiarticle.exception.ThrowUtils;
import com.aiarticle.model.dto.user.UserAddRequest;
import com.aiarticle.model.dto.user.UserLoginRequest;
import com.aiarticle.model.dto.user.UserQueryRequest;
import com.aiarticle.model.dto.user.UserRegisterRequest;
import com.aiarticle.model.dto.user.UserUpdateRequest;
import com.aiarticle.model.entity.User;
import com.aiarticle.model.vo.LoginUserVO;
import com.aiarticle.model.vo.UserVO;
import com.aiarticle.service.UserService;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口
 */
@Tag(name = "用户接口", description = "用户注册、登录、注销及用户管理（管理员）")
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    // region 登录相关

    /**
     * 用户注册
     */
    @Operation(summary = "用户注册", description = "账号密码注册")
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        long userId = userService.userRegister(userRegisterRequest);
        return ResultUtils.success(userId);
    }

    /**
     * 用户登录（Session 认证）
     */
    @Operation(summary = "用户登录", description = "Session 认证")
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        LoginUserVO loginUserVO = userService.userLogin(userLoginRequest, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获取当前登录用户
     */
    @Operation(summary = "获取登录用户", description = "获取当前用户信息")
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        LoginUserVO loginUserVO = userService.getLoginUserVO(request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 用户注销
     */
    @Operation(summary = "用户注销", description = "退出登录")
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    // endregion

    // region 管理员专用

    /**
     * 创建用户（管理员专用）
     */
    @Operation(summary = "创建用户", description = "管理员专用")
    @PostMapping("/add")
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest, HttpServletRequest request) {
        // 仅管理员可操作
        checkAdmin(request);
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        long userId = userService.addUser(userAddRequest);
        return ResultUtils.success(userId);
    }

    /**
     * 删除用户（管理员专用）
     */
    @Operation(summary = "删除用户", description = "管理员专用")
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        checkAdmin(request);
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null, ErrorCode.PARAMS_ERROR, "id 为空");
        boolean removed = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(removed);
    }

    /**
     * 更新用户（管理员专用）
     */
    @Operation(summary = "更新用户", description = "管理员专用")
    @PostMapping("/update")
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest, HttpServletRequest request) {
        checkAdmin(request);
        ThrowUtils.throwIf(userUpdateRequest == null || userUpdateRequest.getId() == null, ErrorCode.PARAMS_ERROR, "id 为空");
        User user = new User();
        BeanUtil.copyProperties(userUpdateRequest, user);
        boolean updated = userService.update(user);
        return ResultUtils.success(updated);
    }

    /**
     * 分页查询用户（管理员专用）
     */
    @Operation(summary = "分页查询用户", description = "管理员专用")
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest, HttpServletRequest request) {
        checkAdmin(request);
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        ThrowUtils.throwIf(current < 1 || pageSize < 1 || pageSize > 50, ErrorCode.PARAMS_ERROR, "分页参数错误");
        Page<UserVO> userVOPage = userService.listUserVOByPage(userQueryRequest);
        return ResultUtils.success(userVOPage);
    }

    // endregion

    /**
     * 校验当前登录用户是否为管理员
     */
    private void checkAdmin(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(!UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole()), ErrorCode.NO_AUTH_ERROR, "仅管理员可操作");
    }
}