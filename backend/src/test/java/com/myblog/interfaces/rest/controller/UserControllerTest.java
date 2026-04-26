package com.myblog.interfaces.rest.controller;

import com.myblog.application.service.FollowAppService;
import com.myblog.application.service.UserAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.infrastructure.security.JwtPayload;
import com.myblog.interfaces.rest.mapper.RestDtoMapper;
import com.myblog.shared.exception.ApplicationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

/**
 * 用户控制器测试。
 *
 * @author Codex
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserAppService userAppService;

    @Mock
    private FollowAppService followAppService;

    @Mock
    private RestDtoMapper restDtoMapper;

    @InjectMocks
    private UserController userController;

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    /**
     * 游客不能关注作者。
     */
    @Test
    @DisplayName("游客不能关注作者")
    void shouldRejectGuestWhenFollowingUser() {
        AuthContext.clear();

        assertThatThrownBy(() -> userController.followUser(1002L))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("请先登录");
    }

    /**
     * 已登录用户可以关注作者。
     */
    @Test
    @DisplayName("登录用户可以关注作者")
    void shouldAllowAuthenticatedUserToFollow() {
        JwtPayload payload = new JwtPayload();
        payload.setUserId(1001L);
        payload.setRole("USER");
        payload.setUsername("demo");
        payload.setExpireAt(System.currentTimeMillis() + 60000L);
        AuthContext.set(payload);

        userController.followUser(1002L);

        verify(followAppService).followUser(1002L, 1001L);
    }
}
