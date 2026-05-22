package com.myblog.interfaces.rest.interceptor;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Cache<String, AtomicInteger> counterCache = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build();

    private static final int MAX_REQUESTS_PER_MINUTE = 10;

    private static final String[] LIMITED_PATHS = {
        "/api/auth/register",
        "/api/auth/login",
        "/api/auth/password/forgot",
        "/api/auth/register/email-code",
        "/api/uploads/images",
        "/api/uploads/files"
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
        throws Exception {
        String path = request.getRequestURI();
        boolean isLimited = false;
        for (String prefix : LIMITED_PATHS) {
            if (path.startsWith(prefix)) {
                isLimited = true;
                break;
            }
        }
        if (!isLimited) {
            return true;
        }

        String key = getClientIp(request) + ":" + path;
        AtomicInteger counter = counterCache.get(key, k -> new AtomicInteger(0));
        if (counter.incrementAndGet() > MAX_REQUESTS_PER_MINUTE) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(429);
            response.getWriter().write(
                "{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\",\"data\":null}");
            return false;
        }
        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isEmpty()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
