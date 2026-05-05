package com.myblog.infrastructure.security;

import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.UserRepository;
import com.myblog.shared.enums.UserStatus;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT 认证拦截器。
 *
 * @author Codex
 * @since 1.0.0
 */
@Component
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    /**
     * 创建 JWT 认证拦截器。
     *
     * @param jwtTokenProvider JWT 工具
     */
    public JwtAuthenticationInterceptor(JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    /**
     * 请求进入 Controller 前完成认证校验。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param handler 处理器
     * @return 是否继续处理
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String header = request.getHeader(AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            JwtPayload payload = jwtTokenProvider.parseToken(header.substring(BEARER_PREFIX.length()));
            ensureUserAvailable(payload.getUserId());
            AuthContext.set(payload);
            return true;
        }
        // 支持从 query param 读取 token（用于 SSE 等不支持自定义 Header 的请求）
        String queryToken = request.getParameter("token");
        if (queryToken != null && !queryToken.trim().isEmpty()) {
            JwtPayload payload = jwtTokenProvider.parseToken(queryToken.trim());
            ensureUserAvailable(payload.getUserId());
            AuthContext.set(payload);
            return true;
        }
        if (isPublicRequest(request)) {
            return true;
        }
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return true;
    }

    private void ensureUserAvailable(Long userId) {
        User user = userRepository.findById(new UserId(userId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录"));
        if (!UserStatus.NORMAL.equals(user.getStatus())) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "账号不可用");
        }
    }

    private boolean isPublicRequest(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if ("/api/auth/register".equals(path)
            || "/api/auth/register/email-code".equals(path)
            || "/api/auth/login".equals(path)
            || "/api/auth/password/forgot".equals(path)
            || "/api/auth/password/reset".equals(path)
            || "/api/health".equals(path)) {
            return true;
        }
        if (path.matches("^/api/ads/\\d+/(impression|click)$")) {
            return true;
        }
        if (!"GET".equalsIgnoreCase(method)) {
            return false;
        }
        if ("/api/articles".equals(path)
            || "/api/ads".equals(path)
            || "/api/categories".equals(path)
            || "/api/tags".equals(path)
            || "/api/home/stats".equals(path)
            || "/api/home/bootstrap".equals(path)
            || "/api/rankings/articles".equals(path)
            || "/api/rankings/authors".equals(path)
            || "/api/recommendations/articles".equals(path)
            || "/api/recommendations/authors".equals(path)
            || "/api/recommendations/columns".equals(path)
            || "/api/columns".equals(path)
            || "/api/columns/recommended".equals(path)
            || "/api/topics".equals(path)
            || "/api/search/bootstrap".equals(path)
            || "/api/search/users".equals(path)
            || "/api/search/columns".equals(path)
            || "/api/search/hot-keywords".equals(path)
            || path.matches("^/api/articles/\\d+$")
            || path.matches("^/api/articles/\\d+/related$")
            || path.matches("^/api/articles/\\d+/recommendations$")
            || path.matches("^/api/articles/\\d+/neighbors$")
            || path.matches("^/api/articles/\\d+/comments$")
            || path.matches("^/api/comments/\\d+/replies$")
            || path.matches("^/api/articles/\\d+/like/status$")
            || path.matches("^/api/articles/\\d+/favorite/status$")
            || path.matches("^/api/users/\\d+$")
            || path.matches("^/api/users/\\d+/articles$")
            || path.matches("^/api/users/\\d+/articles/hot$")
            || path.matches("^/api/users/\\d+/followers$")
            || path.matches("^/api/users/\\d+/following$")
            || path.matches("^/api/users/\\d+/follow-status$")
            || "/api/announcements/active".equals(path)
            || path.matches("^/api/categories/\\d+$")
            || path.matches("^/api/categories/\\d+/articles$")
            || path.matches("^/api/tags/\\d+$")
            || path.matches("^/api/tags/\\d+/articles$")
            || path.matches("^/api/columns/\\d+$")
            || path.matches("^/api/columns/\\d+/articles$")
            || path.matches("^/api/topics/\\d+$")
            || path.matches("^/api/topics/\\d+/articles$")
            || "/api/tags/hot".equals(path)) {
            return true;
        }
        return false;
    }

    /**
     * 请求完成后清理线程上下文。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param handler 处理器
     * @param ex 异常信息
     */
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        AuthContext.clear();
    }
}
