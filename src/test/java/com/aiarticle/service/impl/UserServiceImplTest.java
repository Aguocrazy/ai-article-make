package com.aiarticle.service.impl;

import com.aiarticle.exception.BusinessException;
import com.aiarticle.exception.ErrorCode;
import com.aiarticle.mapper.UserMapper;
import com.aiarticle.model.dto.user.UserRegisterRequest;
import com.aiarticle.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private UserRegisterRequest request;

    @BeforeEach
    void setUp() {
        request = new UserRegisterRequest();
        request.setUserAccount("alice");
        request.setUserPassword("12345678");
        request.setCheckPassword("12345678");
    }

    @Test
    void register_rejectsShortAccount() {
        request.setUserAccount("abc");
        BusinessException ex = assertThrows(BusinessException.class, () -> userService.userRegister(request));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
        assertEquals("账号长度必须在 4 到 256 位之间", ex.getMessage());
        verify(userMapper, never()).insert(any());
    }

    @Test
    void register_rejectsShortPassword() {
        request.setUserPassword("1234567");
        request.setCheckPassword("1234567");
        BusinessException ex = assertThrows(BusinessException.class, () -> userService.userRegister(request));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
        assertEquals("密码长度必须在 8 到 512 位之间", ex.getMessage());
        verify(userMapper, never()).insert(any());
    }

    @Test
    void register_whenAccountExists_doesNotInsert() {
        when(userMapper.selectCountByQuery(any())).thenReturn(1L);
        BusinessException ex = assertThrows(BusinessException.class, () -> userService.userRegister(request));
        assertEquals("账号已存在", ex.getMessage());
        verify(userMapper, never()).insert(any());
    }

    @Test
    void register_whenUniqueIndexViolated_returnsAccountExists() {
        when(userMapper.selectCountByQuery(any())).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenThrow(new DuplicateKeyException("uk_userAccount"));
        BusinessException ex = assertThrows(BusinessException.class, () -> userService.userRegister(request));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
        assertEquals("账号已存在", ex.getMessage());
    }

    @Test
    void register_storesSaltedPasswordNotPlaintext() {
        when(userMapper.selectCountByQuery(any())).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(100L);
            return 1;
        });

        long id = userService.userRegister(request);
        assertEquals(100L, id);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        String stored = captor.getValue().getUserPassword();
        assertNotEquals("12345678", stored);
        assertTrue(stored.contains("$"), "应使用 盐$摘要 存储，每人一盐以抵御彩虹表");
        String salt = stored.substring(0, stored.indexOf('$'));
        assertTrue(salt.length() >= 16);
        assertNotEquals(stored.substring(stored.indexOf('$') + 1), salt);
    }
}
