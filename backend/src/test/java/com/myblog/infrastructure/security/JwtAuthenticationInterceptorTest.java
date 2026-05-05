package com.myblog.infrastructure.security;

import com.myblog.domain.repository.UserRepository;
import com.myblog.shared.exception.ApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class JwtAuthenticationInterceptorTest {

    private JwtAuthenticationInterceptor interceptor;

    @BeforeEach
    void setUp() {
        AuthContext.clear();
        interceptor = new JwtAuthenticationInterceptor(
            mock(JwtTokenProvider.class),
            mock(UserRepository.class)
        );
    }

    @Test
    void guestCanAccessPassiveRecommendationEndpoints() {
        assertPublicGet("/api/articles/206/recommendations");
        assertPublicGet("/api/recommendations/articles");
        assertPublicGet("/api/recommendations/authors");
        assertPublicGet("/api/recommendations/columns");
    }

    @Test
    void guestCanAccessReadOnlyArticleInteractionStatus() {
        assertPublicGet("/api/articles/206/like/status");
        assertPublicGet("/api/articles/206/favorite/status");
    }

    @Test
    void guestWriteActionStillRequiresLogin() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/articles/206/like");

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("请先登录");
    }

    private void assertPublicGet(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
    }
}
