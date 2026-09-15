package com.aiarticle.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用响应类
 *
 * @param <T> 数据类型
 */
@Data
public class BaseResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int code;

    private T data;

    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(com.aiarticle.exception.ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}