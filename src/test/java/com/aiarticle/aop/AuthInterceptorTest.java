package com.aiarticle.aop;

import com.aiarticle.annotation.AuthCheck;
import com.aiarticle.constant.UserConstant;
import com.aiarticle.exception.BusinessException;
import com.aiarticle.exception.ErrorCode;
import com.aiarticle.model.entity.User;
import com.aiarticle.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthInterceptorTest {

    @Mock
    private UserService userService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @InjectMocks
    private AuthInterceptor authInterceptor;

    @BeforeEach
    void setRequest() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void adminRequired_rejectsNormalUser() throws Throwable {
        User user = User.builder().id(1L).userRole(UserConstant.DEFAULT_ROLE).build();
        when(userService.getLoginUser(any())).thenReturn(user);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authInterceptor.doInterceptor(joinPoint, adminCheck()));
        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), ex.getCode());
        verify(joinPoint, never()).proceed();
    }

    @Test
    void adminRequired_allowsAdmin() throws Throwable {
        User admin = User.builder().id(1L).userRole(UserConstant.ADMIN_ROLE).build();
        when(userService.getLoginUser(any())).thenReturn(admin);
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = authInterceptor.doInterceptor(joinPoint, adminCheck());
        assertEquals("ok", result);
        verify(joinPoint).proceed();
    }

    private static AuthCheck adminCheck() {
        AuthCheck authCheck = mock(AuthCheck.class);
        when(authCheck.mustRole()).thenReturn(UserConstant.ADMIN_ROLE);
        return authCheck;
    }
}
