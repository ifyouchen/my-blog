package com.myblog.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myblog.domain.repository.AdminLogRepository;
import com.myblog.shared.exception.ApplicationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 管理员日志应用服务测试。
 *
 * @author Codex
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class AdminLogAppServiceTest {

    @Mock
    private AdminLogRepository adminLogRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AdminLogAppService adminLogAppService;

    /**
     * 非法开始日期应返回参数错误。
     */
    @Test
    @DisplayName("非法开始日期应返回参数错误")
    void shouldRejectInvalidStartDate() {
        assertThatThrownBy(() -> adminLogAppService.getLogs(1, 10, null, null, "2026-02-31", null))
            .isInstanceOf(ApplicationException.class)
            .hasMessage("开始日期格式不正确，应为 yyyy-MM-dd");
    }

    /**
     * 开始日期不能晚于结束日期。
     */
    @Test
    @DisplayName("开始日期晚于结束日期时应返回参数错误")
    void shouldRejectReverseDateRange() {
        assertThatThrownBy(() -> adminLogAppService.getLogs(1, 10, null, null, "2026-04-24", "2026-04-01"))
            .isInstanceOf(ApplicationException.class)
            .hasMessage("开始日期不能晚于结束日期");
    }
}
