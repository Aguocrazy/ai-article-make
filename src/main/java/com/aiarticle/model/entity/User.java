package com.aiarticle.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "user", camelToUnderline = false)
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id（使用雪花算法生成）
     */
    @Id(keyType = KeyType.Generator, value = "snowFlakeId")
    private Long id;

    private String userAccount;

    private String userPassword;

    private String userName;

    private String userAvatar;

    private String userProfile;

    private String userRole;

    private LocalDateTime editTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 逻辑删除
     */
    @Column(isLogicDelete = true)
    private Integer isDelete;
}