package com.aiarticle.exception;

import com.aiarticle.common.BaseResponse;
import com.aiarticle.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 拦截业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        if (e.getCode() == ErrorCode.NOT_FOUND_ERROR.getCode()
                || e.getCode() == ErrorCode.NOT_LOGIN_ERROR.getCode()) {
            log.warn("BusinessException code={}, message={}", e.getCode(), e.getMessage());
        } else {
            log.error("BusinessException", e);
        }
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    /**
     * 拦截运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }
}