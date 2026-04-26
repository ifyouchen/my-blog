package com.myblog.interfaces.rest.controller;

import com.myblog.application.service.RankingAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.interfaces.rest.mapper.RestDtoMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

/**
 * 排行榜控制器测试。
 *
 * @author Codex
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class RankingControllerTest {

    @Mock
    private RankingAppService rankingAppService;

    @Mock
    private RestDtoMapper restDtoMapper;

    @InjectMocks
    private RankingController rankingController;

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    /**
     * 游客可查看作者榜。
     */
    @Test
    @DisplayName("游客可以查看作者榜")
    void shouldAllowGuestToLoadAuthorRankings() {
        AuthContext.clear();

        rankingController.listAuthorRankings(10);

        verify(rankingAppService).listAuthorRankings(10, null);
    }
}
