package com.myblog.infrastructure.security;

import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.UserRepository;
import com.myblog.shared.enums.UserStatus;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

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
    private static final String USER_CACHE_NAME = "userStatus";
    private static final long USER_CACHE_TTL_MINUTES = 5;

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RMapCache<Long, UserCacheEntry> userStatusCache;

    public JwtAuthenticationInterceptor(JwtTokenProvider jwtTokenProvider,
                                         UserRepository userRepository,
                                         RedissonClient redissonClient) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.userStatusCache = redissonClient.getMapCache(USER_CACHE_NAME);
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                              @NonNull HttpServletResponse response,
                              @NonNull Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String header = request.getHeader(AUTHORIZATION);

        if (isPublicRequest(request)) {
            authenticateIfPresent(header, true);
            return true;
        }

        if (authenticateIfPresent(header, false)) {
            return true;
        }
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return true;
    }

    private boolean authenticateIfPresent(String header, boolean lenient) {
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return authenticateToken(header.substring(BEARER_PREFIX.length()), lenient);
        }
        return false;
    }

    private boolean authenticateToken(String token, boolean lenient) {
        try {
            JwtPayload payload = jwtTokenProvider.parseToken(token);
            ensureUserAvailable(payload);
            AuthContext.set(payload);
            return true;
        } catch (ApplicationException e) {
            if (!lenient) {
                throw e;
            }
            return false;
        }
    }

    private void ensureUserAvailable(JwtPayload payload) {
        Long userId = payload.getUserId();

        UserCacheEntry cached = userStatusCache.get(userId);
        if (cached != null) {
            if (!UserStatus.NORMAL.equals(cached.status)) {
                throw new ApplicationException(ErrorCode.FORBIDDEN, "账号不可用");
            }
            payload.setRole(cached.role);
            return;
        }

        User user = userRepository.findById(new UserId(userId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录"));

        UserStatus status = user.getStatus();
        String role = user.getRole().name();

        userStatusCache.put(userId, new UserCacheEntry(status, role),
            USER_CACHE_TTL_MINUTES, TimeUnit.MINUTES);

        if (!UserStatus.NORMAL.equals(status)) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "账号不可用");
        }
        payload.setRole(role);
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
        return "/api/articles".equals(path)
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
            || path.matches("^/api/columns/users/\\d+$")
            || path.matches("^/api/columns/\\d+/articles$")
            || path.matches("^/api/columns/\\d+/articles/\\d+/neighbors$")
            || path.matches("^/api/topics/\\d+$")
            || path.matches("^/api/topics/\\d+/articles$")
            || path.matches("^/api/topics/\\d+/articles/\\d+/neighbors$")
            || "/api/tags/hot".equals(path);
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                 @NonNull HttpServletResponse response,
                                 @NonNull Object handler,
                                 @Nullable Exception ex) {
        AuthContext.clear();
    }

    static class UserCacheEntry {
        private UserStatus status;
        private String role;

        UserCacheEntry() {
        }

        UserCacheEntry(UserStatus status, String role) {
            this.status = status;
            this.role = role;
        }

        public UserStatus getStatus() {
            return status;
        }

        public void setStatus(UserStatus status) {
            this.status = status;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }
}
