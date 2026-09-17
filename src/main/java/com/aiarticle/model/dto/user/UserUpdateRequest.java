package com.aiarticle.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理员更新用户请求
 */
@Data
public class UserUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String userName;

    private String userAvatar;

    private String userProfile;

    private String userRole;
}