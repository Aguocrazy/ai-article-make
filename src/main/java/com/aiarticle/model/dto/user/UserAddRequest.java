package com.aiarticle.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理员创建用户请求
 */
@Data
public class UserAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userAccount;

    private String userPassword;

    private String userName;

    private String userAvatar;

    private String userProfile;

    private String userRole;
}