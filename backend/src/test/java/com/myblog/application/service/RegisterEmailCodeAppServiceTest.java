package com.myblog.application.service;

import com.myblog.shared.exception.ApplicationException;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegisterEmailCodeAppServiceTest {

    private RMapCache<String, RegisterEmailCodeAppService.RegisterEmailCode> createTestCache() {
        RMapCache<String, RegisterEmailCodeAppService.RegisterEmailCode> cache = mock(RMapCache.class);
        Map<String, RegisterEmailCodeAppService.RegisterEmailCode> store = new ConcurrentHashMap<>();

        when(cache.get(anyString())).thenAnswer(new Answer<RegisterEmailCodeAppService.RegisterEmailCode>() {
            @Override
            public RegisterEmailCodeAppService.RegisterEmailCode answer(InvocationOnMock inv) {
                return store.get(inv.getArgument(0));
            }
        });
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock inv) {
                store.put(inv.getArgument(0), inv.getArgument(1));
                return null;
            }
        }).when(cache).put(anyString(), any(), anyLong(), any(TimeUnit.class));
        when(cache.remove(anyString())).thenAnswer(new Answer<RegisterEmailCodeAppService.RegisterEmailCode>() {
            @Override
            public RegisterEmailCodeAppService.RegisterEmailCode answer(InvocationOnMock inv) {
                return store.remove(inv.getArgument(0));
            }
        });

        return cache;
    }

    @Test
    void sendAndVerifyCodeConsumesCode() {
        RMapCache<String, RegisterEmailCodeAppService.RegisterEmailCode> testCache = createTestCache();
        EmailQueueAppService emailQueueAppService = new EmailQueueAppService(10, true);
        RegisterEmailCodeAppService service = new RegisterEmailCodeAppService(
            emailQueueAppService, 600000L, 0L, testCache
        );

        service.sendCode("User@Example.com");

        EmailTask task = emailQueueAppService.poll();
        assertThat(task.getType()).isEqualTo(EmailTaskType.REGISTER_CODE);
        assertThat(task.getEmail()).isEqualTo("user@example.com");
        assertThat(task.getCode()).matches("\\d{6}");
        service.verifyAndConsume("user@example.com", task.getCode());
        assertThatThrownBy(() -> service.verifyAndConsume("user@example.com", task.getCode()))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("验证码无效或已过期");
    }

    @Test
    void resendTooSoonIsRejected() {
        RMapCache<String, RegisterEmailCodeAppService.RegisterEmailCode> testCache = createTestCache();
        EmailQueueAppService emailQueueAppService = new EmailQueueAppService(10, true);
        RegisterEmailCodeAppService service = new RegisterEmailCodeAppService(
            emailQueueAppService, 600000L, 30L, testCache
        );

        service.sendCode("user@example.com");

        assertThatThrownBy(() -> service.sendCode("user@example.com"))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("验证码发送过于频繁");
    }

    @Test
    void codeExpires() {
        RMapCache<String, RegisterEmailCodeAppService.RegisterEmailCode> testCache = createTestCache();
        EmailQueueAppService emailQueueAppService = new EmailQueueAppService(10, true);
        RegisterEmailCodeAppService service = new RegisterEmailCodeAppService(
            emailQueueAppService, 1L, 0L, testCache
        );

        service.sendCode("user@example.com");
        EmailTask task = emailQueueAppService.poll();
        testCache.remove("user@example.com");

        assertThatThrownBy(() -> service.verifyAndConsume("user@example.com", task.getCode()))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("验证码无效或已过期");
    }

    @Test
    void tooManyWrongAttemptsInvalidatesCode() {
        RMapCache<String, RegisterEmailCodeAppService.RegisterEmailCode> testCache = createTestCache();
        EmailQueueAppService emailQueueAppService = new EmailQueueAppService(10, true);
        RegisterEmailCodeAppService service = new RegisterEmailCodeAppService(
            emailQueueAppService, 600000L, 0L, testCache
        );

        service.sendCode("user@example.com");
        EmailTask task = emailQueueAppService.poll();
        String wrongCode = "000000".equals(task.getCode()) ? "111111" : "000000";
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> service.verifyAndConsume("user@example.com", wrongCode))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining("验证码不正确");
        }

        assertThatThrownBy(() -> service.verifyAndConsume("user@example.com", wrongCode))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("验证码错误次数过多");
        assertThatThrownBy(() -> service.verifyAndConsume("user@example.com", task.getCode()))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("验证码无效或已过期");
    }

    @Test
    void enqueueFailureInvalidatesCode() {
        RMapCache<String, RegisterEmailCodeAppService.RegisterEmailCode> testCache = createTestCache();
        EmailQueueAppService emailQueueAppService = new EmailQueueAppService(1, true);
        emailQueueAppService.enqueueRegisterCode("busy@example.com", "123456");
        RegisterEmailCodeAppService service = new RegisterEmailCodeAppService(
            emailQueueAppService, 600000L, 0L, testCache
        );

        assertThatThrownBy(() -> service.sendCode("user@example.com"))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("邮件发送队列繁忙");
        assertThatThrownBy(() -> service.verifyAndConsume("user@example.com", "123456"))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("验证码无效或已过期");
    }
}
