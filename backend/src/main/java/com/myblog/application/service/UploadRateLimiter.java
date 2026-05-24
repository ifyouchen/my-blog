package com.myblog.application.service;

import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * 上传限流服务，基于 Redisson RRateLimiter 实现分布式限流。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class UploadRateLimiter {

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadRateLimiter.class);

    private final RedissonClient redissonClient;
    private final long imagePermits;
    private final long imageIntervalSeconds;
    private final long filePermits;
    private final long fileIntervalSeconds;

    public UploadRateLimiter(RedissonClient redissonClient,
                             @Value("${my-blog.upload.rate-limit.image-permits:10}") long imagePermits,
                             @Value("${my-blog.upload.rate-limit.image-interval-seconds:60}") long imageIntervalSeconds,
                             @Value("${my-blog.upload.rate-limit.file-permits:5}") long filePermits,
                             @Value("${my-blog.upload.rate-limit.file-interval-seconds:60}") long fileIntervalSeconds) {
        this.redissonClient = redissonClient;
        this.imagePermits = imagePermits;
        this.imageIntervalSeconds = imageIntervalSeconds;
        this.filePermits = filePermits;
        this.fileIntervalSeconds = fileIntervalSeconds;
    }

    @PostConstruct
    public void init() {
        LOGGER.info("上传限流服务初始化完成, image={}/{}/s, file={}/{}/s",
            imagePermits, imageIntervalSeconds, filePermits, fileIntervalSeconds);
    }

    /**
     * 获取上传许可，无法获取时抛出限流异常。
     *
     * @param userId 用户 ID
     * @param scope  上传用途（image/file）
     * @throws ApplicationException 超过限流阈值时抛出 429
     */
    public void acquire(Long userId, String scope) {
        RRateLimiter limiter = getRateLimiter(userId, scope);
        if (!limiter.tryAcquire()) {
            throw new ApplicationException(ErrorCode.TOO_MANY_REQUESTS,
                "上传过于频繁，请稍后再试");
        }
    }

    private RRateLimiter getRateLimiter(Long userId, String scope) {
        String key = "upload:rate:user:" + userId + ":" + scope;
        RRateLimiter limiter = redissonClient.getRateLimiter(key);
        if (limiter.isExists()) {
            return limiter;
        }
        long rate;
        long interval;
        if ("file".equalsIgnoreCase(scope)) {
            rate = filePermits;
            interval = fileIntervalSeconds;
        } else {
            rate = imagePermits;
            interval = imageIntervalSeconds;
        }
        limiter.trySetRate(RateType.OVERALL, rate, interval, RateIntervalUnit.SECONDS);
        return limiter;
    }
}
