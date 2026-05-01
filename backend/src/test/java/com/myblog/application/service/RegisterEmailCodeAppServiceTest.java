package com.myblog.application.service;

import com.myblog.application.port.EmailSender;
import com.myblog.shared.exception.ApplicationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegisterEmailCodeAppServiceTest {

    @Test
    void sendAndVerifyCodeConsumesCode() {
        CapturingEmailSender emailSender = new CapturingEmailSender();
        RegisterEmailCodeAppService service = new RegisterEmailCodeAppService(emailSender, 600000L, 0L);

        service.sendCode("User@Example.com");

        assertThat(emailSender.registerCode).matches("\\d{6}");
        service.verifyAndConsume("user@example.com", emailSender.registerCode);
        assertThatThrownBy(() -> service.verifyAndConsume("user@example.com", emailSender.registerCode))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("验证码无效或已过期");
    }

    @Test
    void resendTooSoonIsRejected() {
        RegisterEmailCodeAppService service = new RegisterEmailCodeAppService(
            new CapturingEmailSender(), 600000L, 60L
        );

        service.sendCode("user@example.com");

        assertThatThrownBy(() -> service.sendCode("user@example.com"))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("验证码发送过于频繁");
    }

    @Test
    void codeExpires() throws Exception {
        CapturingEmailSender emailSender = new CapturingEmailSender();
        RegisterEmailCodeAppService service = new RegisterEmailCodeAppService(emailSender, 1L, 0L);

        service.sendCode("user@example.com");
        Thread.sleep(5L);

        assertThatThrownBy(() -> service.verifyAndConsume("user@example.com", emailSender.registerCode))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("验证码无效或已过期");
    }

    @Test
    void tooManyWrongAttemptsInvalidatesCode() {
        CapturingEmailSender emailSender = new CapturingEmailSender();
        RegisterEmailCodeAppService service = new RegisterEmailCodeAppService(emailSender, 600000L, 0L);

        service.sendCode("user@example.com");
        String wrongCode = "000000".equals(emailSender.registerCode) ? "111111" : "000000";
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> service.verifyAndConsume("user@example.com", wrongCode))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining("验证码不正确");
        }

        assertThatThrownBy(() -> service.verifyAndConsume("user@example.com", wrongCode))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("验证码错误次数过多");
        assertThatThrownBy(() -> service.verifyAndConsume("user@example.com", emailSender.registerCode))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("验证码无效或已过期");
    }

    private static class CapturingEmailSender implements EmailSender {

        private String registerCode;

        @Override
        public void sendRegisterCode(String email, String code) {
            this.registerCode = code;
        }

        @Override
        public void sendPasswordResetLink(String email, String username, String resetLink) {
            throw new UnsupportedOperationException();
        }
    }
}
