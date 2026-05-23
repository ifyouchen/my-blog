package com.myblog.application.service;

import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.util.BizLogHelper;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 注册邮箱验证码应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class RegisterEmailCodeAppService {

    private static final Logger log = LoggerFactory.getLogger(RegisterEmailCodeAppService.class);
    private static final int MAX_VERIFY_FAIL_COUNT = 5;
    private static final long CODE_EXPIRE_MILLIS = TimeUnit.MINUTES.toMillis(10);
    private static final String CACHE_NAME = "emailVerifyCode";

    private final RMapCache<String, RegisterEmailCode> emailCodeCache;
    private final EmailQueueAppService emailQueueAppService;
    private final long resendIntervalSeconds;

    @Autowired
    public RegisterEmailCodeAppService(
        RedissonClient redissonClient,
        EmailQueueAppService emailQueueAppService,
        @Value("${my-blog.mail.email-resend-interval-seconds:30}") long resendIntervalSeconds) {
        this.emailCodeCache = redissonClient.getMapCache(CACHE_NAME);
        this.emailQueueAppService = emailQueueAppService;
        this.resendIntervalSeconds = Math.max(resendIntervalSeconds, 1L);
    }

    RegisterEmailCodeAppService(EmailQueueAppService emailQueueAppService,
                                long codeExpireMillis,
                                long resendIntervalSeconds,
                                RMapCache<String, RegisterEmailCode> testCache) {
        this.emailCodeCache = testCache;
        this.emailQueueAppService = emailQueueAppService;
        this.resendIntervalSeconds = Math.max(resendIntervalSeconds, 1L);
    }

    /**
     * 发送注册验证码。
     *
     * @param email 邮箱
     */
    public void sendCode(String email) {
        long _start = System.currentTimeMillis();
        String normalizedEmail = normalizeEmail(email);
        RegisterEmailCode existingCode = emailCodeCache.get(normalizedEmail);
        LocalDateTime now = LocalDateTime.now();
        if (existingCode != null
            && Duration.between(existingCode.getSentAt(), now).getSeconds() < resendIntervalSeconds) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "验证码发送过于频繁，请稍后再试");
        }

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        emailCodeCache.put(normalizedEmail, new RegisterEmailCode(code, now), CODE_EXPIRE_MILLIS, TimeUnit.MILLISECONDS);
        try {
            emailQueueAppService.enqueueRegisterCode(normalizedEmail, code);
        } catch (RuntimeException exception) {
            emailCodeCache.remove(normalizedEmail);
            throw exception;
        }
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "发送注册验证码",
            BizLogHelper.trace(),
            BizLogHelper.params("email", normalizedEmail),
            BizLogHelper.result("OK"),
            BizLogHelper.elapsed(_start));
    }

    public void verifyAndConsume(String email, String code) {
        long _start = System.currentTimeMillis();
        String normalizedEmail = normalizeEmail(email);
        RegisterEmailCode cachedCode = emailCodeCache.get(normalizedEmail);
        if (cachedCode == null) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "验证码无效或已过期，请重新获取");
        }

        String submittedCode = code == null ? "" : code.trim();
        if (!cachedCode.getCode().equals(submittedCode)) {
            int failCount = cachedCode.increaseFailCount();
            emailCodeCache.put(normalizedEmail, cachedCode, CODE_EXPIRE_MILLIS, TimeUnit.MILLISECONDS);
            if (failCount >= MAX_VERIFY_FAIL_COUNT) {
                emailCodeCache.remove(normalizedEmail);
                throw new ApplicationException(ErrorCode.PARAM_ERROR, "验证码错误次数过多，请重新获取");
            }
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "验证码不正确");
        }

        emailCodeCache.remove(normalizedEmail);
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "校验验证码",
            BizLogHelper.trace(),
            BizLogHelper.params("email", normalizedEmail),
            BizLogHelper.result("OK"),
            BizLogHelper.elapsed(_start));
    }

    private String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "邮箱不能为空");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    static class RegisterEmailCode {

        private String code;
        private LocalDateTime sentAt;
        private int failCount;

        RegisterEmailCode() {
        }

        RegisterEmailCode(String code, LocalDateTime sentAt) {
            this.code = code;
            this.sentAt = sentAt;
        }

        String getCode() {
            return code;
        }

        void setCode(String code) {
            this.code = code;
        }

        LocalDateTime getSentAt() {
            return sentAt;
        }

        void setSentAt(LocalDateTime sentAt) {
            this.sentAt = sentAt;
        }

        int increaseFailCount() {
            return ++failCount;
        }

        int getFailCount() {
            return failCount;
        }

        void setFailCount(int failCount) {
            this.failCount = failCount;
        }
    }
}
