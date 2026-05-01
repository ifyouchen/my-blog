package com.myblog.application.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
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

    private static final int MAX_VERIFY_FAIL_COUNT = 5;
    private static final long CODE_EXPIRE_MILLIS = TimeUnit.MINUTES.toMillis(10);

    private final Cache<String, RegisterEmailCode> emailCodeCache;
    private final EmailQueueAppService emailQueueAppService;
    private final long resendIntervalSeconds;

    @Autowired
    public RegisterEmailCodeAppService(
        EmailQueueAppService emailQueueAppService,
        @Value("${my-blog.mail.email-resend-interval-seconds:30}") long resendIntervalSeconds) {
        this(emailQueueAppService, CODE_EXPIRE_MILLIS, resendIntervalSeconds);
    }

    RegisterEmailCodeAppService(EmailQueueAppService emailQueueAppService,
                                long codeExpireMillis,
                                long resendIntervalSeconds) {
        this.emailQueueAppService = emailQueueAppService;
        this.emailCodeCache = Caffeine.newBuilder()
            .expireAfterWrite(codeExpireMillis, TimeUnit.MILLISECONDS)
            .build();
        this.resendIntervalSeconds = Math.max(resendIntervalSeconds, 1L);
    }

    /**
     * 发送注册验证码。
     *
     * @param email 邮箱
     */
    public void sendCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        RegisterEmailCode existingCode = emailCodeCache.getIfPresent(normalizedEmail);
        LocalDateTime now = LocalDateTime.now();
        if (existingCode != null
            && Duration.between(existingCode.getSentAt(), now).getSeconds() < resendIntervalSeconds) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "验证码发送过于频繁，请稍后再试");
        }

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        emailCodeCache.put(normalizedEmail, new RegisterEmailCode(code, now));
        try {
            emailQueueAppService.enqueueRegisterCode(normalizedEmail, code);
        } catch (ApplicationException exception) {
            emailCodeCache.invalidate(normalizedEmail);
            throw exception;
        } catch (RuntimeException exception) {
            emailCodeCache.invalidate(normalizedEmail);
            throw new ApplicationException(ErrorCode.SYSTEM_ERROR, "验证码发送失败，请稍后重试");
        }
    }

    /**
     * 校验并消费注册验证码。
     *
     * @param email 邮箱
     * @param code 验证码
     */
    public void verifyAndConsume(String email, String code) {
        String normalizedEmail = normalizeEmail(email);
        RegisterEmailCode cachedCode = emailCodeCache.getIfPresent(normalizedEmail);
        if (cachedCode == null) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "验证码无效或已过期，请重新获取");
        }

        String submittedCode = code == null ? "" : code.trim();
        if (!cachedCode.getCode().equals(submittedCode)) {
            int failCount = cachedCode.increaseFailCount();
            if (failCount >= MAX_VERIFY_FAIL_COUNT) {
                emailCodeCache.invalidate(normalizedEmail);
                throw new ApplicationException(ErrorCode.PARAM_ERROR, "验证码错误次数过多，请重新获取");
            }
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "验证码不正确");
        }

        emailCodeCache.invalidate(normalizedEmail);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "邮箱不能为空");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static class RegisterEmailCode {

        private final String code;
        private final LocalDateTime sentAt;
        private int failCount;

        RegisterEmailCode(String code, LocalDateTime sentAt) {
            this.code = code;
            this.sentAt = sentAt;
        }

        String getCode() {
            return code;
        }

        LocalDateTime getSentAt() {
            return sentAt;
        }

        int increaseFailCount() {
            this.failCount++;
            return this.failCount;
        }
    }
}
