package com.myblog.interfaces.rest.controller;

import com.myblog.application.service.ColumnAppService;
import com.myblog.infrastructure.security.AuthContext;
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
 * 专栏控制器测试。
 *
 * @author Codex
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class ColumnControllerTest {

    @Mock
    private ColumnAppService columnAppService;

    @Mock
    private RestDtoMapper restDtoMapper;

    @InjectMocks
    private ColumnController columnController;

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    /**
     * 游客不能订阅专栏。
     */
    @Test
    @DisplayName("游客不能订阅专栏")
    void shouldRejectGuestWhenSubscribingColumn() {
        AuthContext.clear();

        assertThatThrownBy(() -> columnController.subscribeColumn(3001L))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("请先登录");
    }

    /**
     * 游客可查看推荐专栏。
     */
    @Test
    @DisplayName("游客可以查看推荐专栏")
    void shouldAllowGuestToLoadRecommendedColumns() {
        AuthContext.clear();

        columnController.listRecommendedColumns(3);

        verify(columnAppService).listRecommendedColumns(3, null);
    }
}
