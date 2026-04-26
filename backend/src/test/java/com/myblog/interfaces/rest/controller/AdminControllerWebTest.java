package com.myblog.interfaces.rest.controller;

import com.myblog.application.command.RecordAdminLogCommand;
import com.myblog.application.dto.CategoryDTO;
import com.myblog.application.dto.TagDTO;
import com.myblog.application.service.AdminAppService;
import com.myblog.application.service.AdminLogAppService;
import com.myblog.application.service.CategoryAppService;
import com.myblog.application.service.TagAppService;
import com.myblog.infrastructure.security.JwtAuthenticationInterceptor;
import com.myblog.infrastructure.security.JwtPayload;
import com.myblog.infrastructure.security.JwtTokenProvider;
import com.myblog.shared.result.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 管理后台 Web 层回归测试。
 *
 * @author Codex
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class AdminControllerWebTest {

    private static final String ADMIN_TOKEN = "admin-token";
    private static final String USER_TOKEN = "user-token";

    @Mock
    private AdminAppService adminAppService;

    @Mock
    private AdminLogAppService adminLogAppService;

    @Mock
    private CategoryAppService categoryAppService;

    @Mock
    private TagAppService tagAppService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private MockMvc mockMvc;

    /**
     * 初始化 MockMvc。
     */
    @BeforeEach
    void setUp() {
        AdminController adminController = new AdminController(
            adminAppService,
            adminLogAppService,
            categoryAppService,
            tagAppService
        );
        JwtAuthenticationInterceptor jwtAuthenticationInterceptor = new JwtAuthenticationInterceptor(jwtTokenProvider);
        mockMvc = MockMvcBuilders.standaloneSetup(adminController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .addInterceptors(jwtAuthenticationInterceptor)
            .build();
    }

    /**
     * 游客请求后台接口应被拒绝。
     *
     * @throws Exception 测试异常
     */
    @Test
    @DisplayName("游客访问后台接口应返回未登录")
    void shouldRejectGuestOnAdminApi() throws Exception {
        mockMvc.perform(get("/api/admin/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(401))
            .andExpect(jsonPath("$.message").value("请先登录"));
    }

    /**
     * 普通用户请求后台接口应被拒绝。
     *
     * @throws Exception 测试异常
     */
    @Test
    @DisplayName("普通用户访问后台接口应返回无权限")
    void shouldRejectNormalUserOnAdminApi() throws Exception {
        when(jwtTokenProvider.parseToken(USER_TOKEN)).thenReturn(buildPayload(1002L, "USER"));

        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + USER_TOKEN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("仅管理员可访问后台"));
    }

    /**
     * 管理员访问后台列表应成功。
     *
     * @throws Exception 测试异常
     */
    @Test
    @DisplayName("管理员访问后台用户列表应成功并透传筛选参数")
    void shouldAllowAdminAndPassFilters() throws Exception {
        when(jwtTokenProvider.parseToken(ADMIN_TOKEN)).thenReturn(buildPayload(1001L, "ADMIN"));
        PageResult<Map<String, Object>> pageResult =
            new PageResult<Map<String, Object>>(Collections.<Map<String, Object>>emptyList(), 2, 10, 21);
        when(adminAppService.getUsers(2, 10, "NORMAL", "demo")).thenReturn(pageResult);

        mockMvc.perform(
                get("/api/admin/users")
                    .header("Authorization", "Bearer " + ADMIN_TOKEN)
                    .param("page", "2")
                    .param("pageSize", "10")
                    .param("status", "NORMAL")
                    .param("keyword", "demo")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.page").value(2))
            .andExpect(jsonPath("$.data.pageSize").value(10))
            .andExpect(jsonPath("$.data.total").value(21));
    }

    /**
     * 日志接口应校验非法日期。
     *
     * @throws Exception 测试异常
     */
    @Test
    @DisplayName("日志接口非法日期应返回参数错误")
    void shouldRejectInvalidLogDate() throws Exception {
        when(jwtTokenProvider.parseToken(ADMIN_TOKEN)).thenReturn(buildPayload(1001L, "ADMIN"));
        when(adminLogAppService.getLogs(1, 10, null, null, "2026-02-31", null))
            .thenThrow(new com.myblog.shared.exception.ApplicationException(400, "开始日期格式不正确，应为 yyyy-MM-dd"));

        mockMvc.perform(
                get("/api/admin/logs")
                    .header("Authorization", "Bearer " + ADMIN_TOKEN)
                    .param("dateFrom", "2026-02-31")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("开始日期格式不正确，应为 yyyy-MM-dd"));
    }

    /**
     * 管理员操作后应记录日志。
     *
     * @throws Exception 测试异常
     */
    @Test
    @DisplayName("管理员删除评论后应记录操作日志")
    void shouldRecordAdminLogWhenDeletingComment() throws Exception {
        when(jwtTokenProvider.parseToken(ADMIN_TOKEN)).thenReturn(buildPayload(1001L, "ADMIN"));
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("id", 7001L);
        result.put("articleId", 501L);
        result.put("userId", 1002L);
        result.put("content", "测试评论");
        result.put("deleted", true);
        when(adminAppService.deleteComment(7001L)).thenReturn(result);

        mockMvc.perform(delete("/api/admin/comments/7001").header("Authorization", "Bearer " + ADMIN_TOKEN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.deleted").value(true));

        verify(adminLogAppService).recordOperation(any(RecordAdminLogCommand.class));
    }

    /**
     * 非法日期请求不应记录后台日志。
     *
     * @throws Exception 测试异常
     */
    @Test
    @DisplayName("日志筛选参数错误时不应触发写操作日志")
    void shouldNotWriteAdminLogOnReadValidationFailure() throws Exception {
        when(jwtTokenProvider.parseToken(ADMIN_TOKEN)).thenReturn(buildPayload(1001L, "ADMIN"));
        when(adminLogAppService.getLogs(1, 10, null, null, null, "bad-date"))
            .thenThrow(new com.myblog.shared.exception.ApplicationException(400, "结束日期格式不正确，应为 yyyy-MM-dd"));

        mockMvc.perform(
                get("/api/admin/logs")
                    .header("Authorization", "Bearer " + ADMIN_TOKEN)
                    .param("dateTo", "bad-date")
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400));

        verify(adminLogAppService, never()).recordOperation(any(RecordAdminLogCommand.class));
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
