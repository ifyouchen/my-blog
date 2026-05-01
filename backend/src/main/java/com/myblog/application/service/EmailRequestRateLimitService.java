package com.myblog.application.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 邮件请求 IP 限流服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class EmailRequestRateLimitService {

    private final Cache<String, LocalDateTime> latestRequestCache;
    private final long ipRateLimitSeconds;

    public EmailRequestRateLimitService(
        @Value("${my-blog.mail.ip-rate-limit-seconds:20}") long ipRateLimitSeconds) {
        this(ipRateLimitSeconds, false);
    }

    EmailRequestRateLimitService(long ipRateLimitSeconds, boolean testOnly) {
        this.ipRateLimitSeconds = Math.max(ipRateLimitSeconds, 1L);
        this.latestRequestCache = Caffeine.newBuilder()
            .expireAfterWrite(this.ipRateLimitSeconds, TimeUnit.SECONDS)
            .build();
    }

    public synchronized void checkAndRecord(String ip) {
        String normalizedIp = ip == null || ip.trim().isEmpty() ? "unknown" : ip.trim();
        LocalDateTime latestRequestAt = latestRequestCache.getIfPresent(normalizedIp);
        LocalDateTime now = LocalDateTime.now();
        if (latestRequestAt != null
            && Duration.between(latestRequestAt, now).getSeconds() < ipRateLimitSeconds) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "请求过于频繁，请稍后再试");
        }
        latestRequestCache.put(normalizedIp, now);
    }
}
