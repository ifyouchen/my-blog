package com.myblog.interfaces.rest.controller;

import com.myblog.application.command.RecordAdminLogCommand;
import com.myblog.application.service.AdminAppService;
import com.myblog.application.service.AdminLogAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.infrastructure.security.JwtPayload;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.result.PageResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 后台控制器权限测试。
 *
 * @author Codex
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminAppService adminAppService;

    @Mock
    private AdminLogAppService adminLogAppService;

    @InjectMocks
    private AdminController adminController;

    /**
     * 清理认证上下文。
     */
    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    /**
     * 游客不可访问后台统计接口。
     */
    @Test
    @DisplayName("未登录用户不能访问后台统计")
    void shouldRejectGuestWhenGettingStats() {
        assertThatThrownBy(() -> adminController.getStats())
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("请先登录");
    }

    /**
     * 普通用户不可访问后台。
     */
    @Test
    @DisplayName("普通用户不能访问后台用户列表")
    void shouldRejectNonAdminWhenGettingUsers() {
        AuthContext.set(buildPayload(1002L, "USER"));

        assertThatThrownBy(() -> adminController.getUsers(1, 10, null, null))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("仅管理员可访问后台");
    }

    /**
     * 管理员可查询后台操作日志。
     */
    @Test
    @DisplayName("管理员可以查询后台操作日志")
    void shouldAllowAdminToGetLogs() {
        AuthContext.set(buildPayload(1001L, "ADMIN"));
        PageResult<Map<String, Object>> pageResult =
            new PageResult<Map<String, Object>>(Collections.<Map<String, Object>>emptyList(), 1, 10, 0);
        when(adminLogAppService.getLogs(1, 10, null, null, null, null)).thenReturn(pageResult);

        assertThat(adminController.getLogs(1, 10, null, null, null, null).getData()).isSameAs(pageResult);
        verify(adminLogAppService).getLogs(1, 10, null, null, null, null);
    }

    /**
     * 管理员更新用户状态后应记录日志。
     */
    @Test
    @DisplayName("管理员更新用户状态后应写入操作日志")
    void shouldRecordLogWhenUpdatingUserStatus() {
        AuthContext.set(buildPayload(1001L, "ADMIN"));
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("id", 2001L);
        result.put("username", "tester");
        result.put("previousStatus", "NORMAL");
        result.put("status", "DISABLED");
        when(adminAppService.updateUserStatus(2001L, "DISABLED")).thenReturn(result);

        assertThat(adminController.updateUserStatus(2001L, "DISABLED", null).getData().get("status"))
            .isEqualTo("DISABLED");
        verify(adminLogAppService).recordOperation(any(RecordAdminLogCommand.class));
    }

    /**
     * 管理员查询用户列表时应透传关键字。
     */
    @Test
    @DisplayName("管理员查询用户列表时应透传状态和关键字")
    void shouldPassFiltersWhenGettingUsers() {
        AuthContext.set(buildPayload(1001L, "ADMIN"));
        PageResult<Map<String, Object>> pageResult =
            new PageResult<Map<String, Object>>(Collections.<Map<String, Object>>emptyList(), 1, 10, 0);
        when(adminAppService.getUsers(1, 10, "NORMAL", "demo")).thenReturn(pageResult);

        assertThat(adminController.getUsers(1, 10, "NORMAL", "demo").getData()).isSameAs(pageResult);
        verify(adminAppService).getUsers(1, 10, "NORMAL", "demo");
    }

    /**
     * 管理员查询评论列表时应透传筛选参数。
     */
    @Test
    @DisplayName("管理员查询评论列表时应透传文章和关键字筛选")
    void shouldPassFiltersWhenGettingComments() {
        AuthContext.set(buildPayload(1001L, "ADMIN"));
        PageResult<Map<String, Object>> pageResult =
            new PageResult<Map<String, Object>>(Collections.<Map<String, Object>>emptyList(), 1, 10, 0);
        when(adminAppService.getComments(1, 10, 101L, "Spring")).thenReturn(pageResult);

        assertThat(adminController.getComments(1, 10, 101L, "Spring").getData()).isSameAs(pageResult);
        verify(adminAppService).getComments(1, 10, 101L, "Spring");
    }

    /**
     * 管理员查询日志时应透传筛选参数。
     */
    @Test
    @DisplayName("管理员查询日志时应透传筛选参数")
    void shouldPassFiltersWhenGettingLogs() {
        AuthContext.set(buildPayload(1001L, "ADMIN"));
        PageResult<Map<String, Object>> pageResult =
            new PageResult<Map<String, Object>>(Collections.<Map<String, Object>>emptyList(), 1, 10, 0);
        when(adminLogAppService.getLogs(1, 10, "DELETE_COMMENT", "SUCCESS", "2026-04-01", "2026-04-24"))
            .thenReturn(pageResult);

        assertThat(
            adminController.getLogs(1, 10, "DELETE_COMMENT", "SUCCESS", "2026-04-01", "2026-04-24").getData()
        ).isSameAs(pageResult);
        verify(adminLogAppService).getLogs(1, 10, "DELETE_COMMENT", "SUCCESS", "2026-04-01", "2026-04-24");
    }

    private JwtPayload buildPayload(Long userId, String role) {
        JwtPayload payload = new JwtPayload();
        payload.setUserId(userId);
        payload.setUsername("tester");
        payload.setRole(role);
        payload.setExpireAt(System.currentTimeMillis() + 60000L);
        return payload;
    }
}
