package com.myblog.application.service;

import com.myblog.shared.exception.ApplicationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailQueueAppServiceTest {

    @Test
    void enqueueRegisterCodeAddsTask() {
        EmailQueueAppService service = new EmailQueueAppService(10, true);

        service.enqueueRegisterCode("user@example.com", "123456");

        EmailTask task = service.poll();
        assertThat(task.getType()).isEqualTo(EmailTaskType.REGISTER_CODE);
        assertThat(task.getEmail()).isEqualTo("user@example.com");
        assertThat(task.getCode()).isEqualTo("123456");
    }

    @Test
    void enqueuePasswordResetAddsTask() {
        EmailQueueAppService service = new EmailQueueAppService(10, true);

        service.enqueuePasswordReset("user@example.com", "coder", "http://localhost/reset");

        EmailTask task = service.poll();
        assertThat(task.getType()).isEqualTo(EmailTaskType.PASSWORD_RESET);
        assertThat(task.getEmail()).isEqualTo("user@example.com");
        assertThat(task.getUsername()).isEqualTo("coder");
        assertThat(task.getResetLink()).isEqualTo("http://localhost/reset");
    }

    @Test
    void queueFullRejectsNewTask() {
        EmailQueueAppService service = new EmailQueueAppService(1, true);

        service.enqueueRegisterCode("user@example.com", "123456");

        assertThatThrownBy(() -> service.enqueueRegisterCode("other@example.com", "654321"))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("邮件发送队列繁忙");
    }
}
