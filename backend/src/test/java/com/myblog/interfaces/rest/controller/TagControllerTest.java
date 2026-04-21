package com.myblog.interfaces.rest.controller;

import com.myblog.application.command.RecordAdminLogCommand;
import com.myblog.application.dto.TagDTO;
import com.myblog.application.service.AdminLogAppService;
import com.myblog.application.service.TagAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.infrastructure.security.JwtPayload;
import com.myblog.shared.exception.ApplicationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 标签控制器权限测试。
 *
 * @author Codex
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class TagControllerTest {

    @Mock
    private TagAppService tagAppService;

    @Mock
    private AdminLogAppService adminLogAppService;

    @InjectMocks
    private TagController tagController;

    /**
     * 清理认证上下文。
     */
    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    /**
     * 普通用户不能创建标签。
     */
    @Test
    @DisplayName("普通用户不能创建标签")
    void shouldRejectNonAdminWhenCreatingTag() {
        AuthContext.set(buildPayload(1002L, "USER"));
        Map<String, Object> request = new HashMap<String, Object>();
        request.put("name", "Spring");

        assertThatThrownBy(() -> tagController.createTag(request, null))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("仅管理员可管理标签");
    }

    /**
     * 管理员创建标签后应记录日志。
     */
    @Test
    @DisplayName("管理员创建标签后应记录操作日志")
    void shouldRecordLogWhenCreatingTag() {
        AuthContext.set(buildPayload(1001L, "ADMIN"));
        Map<String, Object> request = new HashMap<String, Object>();
        request.put("name", "Spring");
        request.put("description", "Spring 生态");
        TagDTO dto = new TagDTO();
        dto.setId(401L);
        dto.setName("Spring");
        dto.setDescription("Spring 生态");
        dto.setEnabled(true);
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());
        when(tagAppService.createTag("Spring", "Spring 生态")).thenReturn(dto);

        tagController.createTag(request, null);

        verify(adminLogAppService).recordOperation(any(RecordAdminLogCommand.class));
    }

    private JwtPayload buildPayload(Long userId, String role) {
        JwtPayload payload = new JwtPayload();
        payload.setUserId(userId);
        payload.setRole(role);
        payload.setUsername("tester");
        payload.setExpireAt(System.currentTimeMillis() + 60000L);
        return payload;
    }
}
