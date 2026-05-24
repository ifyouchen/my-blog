package com.myblog.interfaces.rest.interceptor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;
    private final int maxRequestsPerMinute;

    private static final String[] LIMITED_PATHS = {
        "/api/auth/register",
        "/api/auth/login",
        "/api/auth/password/forgot",
        "/api/auth/register/email-code",
        "/api/uploads/images",
        "/api/uploads/files"
    };

    public RateLimitInterceptor(StringRedisTemplate redisTemplate,
                                @Value("${my-blog.rate-limit.ip-max-requests-per-minute:20}") int maxRequestsPerMinute) {
        this.redisTemplate = redisTemplate;
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

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

        String ip = getClientIp(request);
        String minuteKey = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        String redisKey = "rate:ip:" + ip + ":" + path.replace('/', ':') + ":" + minuteKey;

        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count == 1) {
            redisTemplate.expire(redisKey, 60, TimeUnit.SECONDS);
        }

        if (count > maxRequestsPerMinute) {
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
