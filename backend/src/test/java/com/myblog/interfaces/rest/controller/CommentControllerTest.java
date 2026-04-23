package com.myblog.interfaces.rest.controller;

import com.myblog.application.service.CommentAppService;
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

/**
 * 评论控制器权限测试。
 *
 * @author Codex
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    @Mock
    private CommentAppService commentAppService;

    @Mock
    private RestDtoMapper restDtoMapper;

    @InjectMocks
    private CommentController commentController;

    /**
     * 清理认证上下文。
     */
    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    /**
     * 游客不能点赞评论。
     */
    @Test
    @DisplayName("游客不能点赞评论")
    void shouldRejectGuestWhenLikingComment() {
        AuthContext.clear();

        assertThatThrownBy(() -> commentController.likeComment(701L))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("请先登录");
    }

    /**
     * 已登录用户可访问点赞接口。
     */
    @Test
    @DisplayName("登录用户可以访问评论点赞接口")
    void shouldAllowAuthenticatedUserToLikeComment() {
        JwtPayload payload = new JwtPayload();
        payload.setUserId(1002L);
        payload.setRole("USER");
        payload.setUsername("demo");
        payload.setExpireAt(System.currentTimeMillis() + 60000L);
        AuthContext.set(payload);

        commentController.likeComment(701L);
    }
}
