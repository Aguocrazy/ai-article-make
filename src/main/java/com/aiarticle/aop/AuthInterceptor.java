package com.aiarticle.aop;

import com.aiarticle.annotation.AuthCheck;
import com.aiarticle.constant.UserConstant;
import com.aiarticle.exception.BusinessException;
import com.aiarticle.exception.ErrorCode;
import com.aiarticle.model.entity.User;
import com.aiarticle.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 拦截 {@link AuthCheck}，从 Session 取登录用户并校验角色。
 */
@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        HttpServletRequest request = currentRequest();
        User loginUser = userService.getLoginUser(request);
        String mustRole = authCheck.mustRole();
        if (!StringUtils.hasText(mustRole)) {
            return joinPoint.proceed();
        }
        if (UserConstant.ADMIN_ROLE.equals(mustRole)
                && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅管理员可操作");
        }
        return joinPoint.proceed();
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return attributes.getRequest();
    }
}
