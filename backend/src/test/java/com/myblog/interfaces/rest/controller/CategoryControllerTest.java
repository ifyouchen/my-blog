package com.myblog.interfaces.rest.controller;

import com.myblog.application.command.RecordAdminLogCommand;
import com.myblog.application.dto.CategoryDTO;
import com.myblog.application.service.AdminLogAppService;
import com.myblog.application.service.CategoryAppService;
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
 * 分类控制器权限测试。
 *
 * @author Codex
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private CategoryAppService categoryAppService;

    @Mock
    private AdminLogAppService adminLogAppService;

    @InjectMocks
    private CategoryController categoryController;

    /**
     * 清理认证上下文。
     */
    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    /**
     * 普通用户不能创建分类。
     */
    @Test
    @DisplayName("普通用户不能创建分类")
    void shouldRejectNonAdminWhenCreatingCategory() {
        AuthContext.set(buildPayload(1002L, "USER"));
        Map<String, Object> request = new HashMap<String, Object>();
        request.put("name", "Java");

        assertThatThrownBy(() -> categoryController.createCategory(request, null))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("仅管理员可管理分类");
    }

    /**
     * 管理员创建分类后应记录日志。
     */
    @Test
    @DisplayName("管理员创建分类后应记录操作日志")
    void shouldRecordLogWhenCreatingCategory() {
        AuthContext.set(buildPayload(1001L, "ADMIN"));
        Map<String, Object> request = new HashMap<String, Object>();
        request.put("name", "Java");
        request.put("description", "Java 分类");
        request.put("sortOrder", 1);
        CategoryDTO dto = new CategoryDTO();
        dto.setId(301L);
        dto.setName("Java");
        dto.setDescription("Java 分类");
        dto.setSortOrder(1);
        dto.setEnabled(true);
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());
        when(categoryAppService.createCategory("Java", "Java 分类", 1)).thenReturn(dto);

        categoryController.createCategory(request, null);

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
