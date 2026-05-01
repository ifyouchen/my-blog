package com.myblog.application.service;

import com.myblog.shared.exception.ApplicationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegisterEmailCodeAppServiceTest {

    @Test
    void sendAndVerifyCodeConsumesCode() {
        EmailQueueAppService emailQueueAppService = new EmailQueueAppService(10, true);
        RegisterEmailCodeAppService service = new RegisterEmailCodeAppService(
            emailQueueAppService, 600000L, 0L
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
        EmailQueueAppService emailQueueAppService = new EmailQueueAppService(10, true);
        RegisterEmailCodeAppService service = new RegisterEmailCodeAppService(
            emailQueueAppService, 600000L, 30L
        );

        service.sendCode("user@example.com");

        assertThatThrownBy(() -> service.sendCode("user@example.com"))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("验证码发送过于频繁");
    }

    @Test
    void codeExpires() throws Exception {
        EmailQueueAppService emailQueueAppService = new EmailQueueAppService(10, true);
        RegisterEmailCodeAppService service = new RegisterEmailCodeAppService(
            emailQueueAppService, 1L, 0L
        );

        service.sendCode("user@example.com");
        EmailTask task = emailQueueAppService.poll();
        Thread.sleep(5L);

        assertThatThrownBy(() -> service.verifyAndConsume("user@example.com", task.getCode()))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("验证码无效或已过期");
    }

    @Test
    void tooManyWrongAttemptsInvalidatesCode() {
        EmailQueueAppService emailQueueAppService = new EmailQueueAppService(10, true);
        RegisterEmailCodeAppService service = new RegisterEmailCodeAppService(
            emailQueueAppService, 600000L, 0L
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
        EmailQueueAppService emailQueueAppService = new EmailQueueAppService(1, true);
        emailQueueAppService.enqueueRegisterCode("busy@example.com", "123456");
        RegisterEmailCodeAppService service = new RegisterEmailCodeAppService(
            emailQueueAppService, 600000L, 0L
        );

        assertThatThrownBy(() -> service.sendCode("user@example.com"))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("邮件发送队列繁忙");
        assertThatThrownBy(() -> service.verifyAndConsume("user@example.com", "123456"))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("验证码无效或已过期");
    }
}
