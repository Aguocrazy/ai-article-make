package com.aiarticle.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验：标注在方法上，由 {@code AuthInterceptor} 在调用前校验登录态与角色。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {

    /**
     * 必须具备的角色，如 {@code admin}。空字符串表示仅校验已登录。
     */
    String mustRole() default "";
}
