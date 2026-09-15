package com.aiarticle.exception;

/**
 * 异常工具类：条件成立则抛出业务异常
 */
public class ThrowUtils {

    private ThrowUtils() {
        // 工具类，禁止实例化
    }

    /**
     * 条件成立则抛异常
     *
     * @param condition        条件
     * @paramRuntimeException 业务异常
     */
    public static void throwIf(boolean condition, BusinessException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }

    /**
     * 条件成立则抛异常
     *
     * @param condition  条件
     * @param errorCode  错误码
     */
    public static void throwIf(boolean condition, ErrorCode errorCode) {
        if (condition) {
            throw new BusinessException(errorCode);
        }
    }

    /**
     * 条件成立则抛异常
     *
     * @param condition 条件
     * @param errorCode 错误码
     * @param message   自定义错误信息
     */
    public static void throwIf(boolean condition, ErrorCode errorCode, String message) {
        if (condition) {
            throw new BusinessException(errorCode, message);
        }
    }
}