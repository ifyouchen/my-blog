package com.myblog.infrastructure.security;

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

    /**
     * 创建 JWT 认证拦截器。
     *
     * @param jwtTokenProvider JWT 工具
     */
    public JwtAuthenticationInterceptor(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
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

    private boolean isPublicRequest(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if ("/api/auth/register".equals(path) || "/api/auth/login".equals(path) || "/api/health".equals(path)) {
            return true;
        }
        if (!"GET".equalsIgnoreCase(method)) {
            return false;
        }
        return "/api/articles".equals(path)
            || "/api/categories".equals(path)
            || "/api/tags".equals(path)
            || path.matches("^/api/articles/\\d+$")
            || path.matches("^/api/articles/\\d+/comments$")
            || path.matches("^/api/articles/\\d+/like/status$")
            || path.matches("^/api/articles/\\d+/favorite/status$")
            || path.matches("^/api/users/\\d+$")
            || path.matches("^/api/users/\\d+/articles$");
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
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }
}
