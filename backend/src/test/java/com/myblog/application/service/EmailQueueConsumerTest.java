package com.myblog.application.service;

import com.myblog.application.port.EmailSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailQueueConsumerTest {

    @Test
    void consumesOnlyOneTaskEachTime() {
        EmailQueueAppService queue = new EmailQueueAppService(10, true);
        CapturingEmailSender emailSender = new CapturingEmailSender();
        EmailQueueConsumer consumer = new EmailQueueConsumer(queue, emailSender, 3);
        queue.enqueueRegisterCode("first@example.com", "111111");
        queue.enqueueRegisterCode("second@example.com", "222222");

        consumer.consumeOne();

        assertThat(emailSender.registerSendCount).isEqualTo(1);
        assertThat(emailSender.lastRegisterEmail).isEqualTo("first@example.com");
        assertThat(queue.size()).isEqualTo(1);
    }

    @Test
    void failedTaskIsRequeuedWithRetryCount() {
        EmailQueueAppService queue = new EmailQueueAppService(10, true);
        CapturingEmailSender emailSender = new CapturingEmailSender();
        emailSender.failRegister = true;
        EmailQueueConsumer consumer = new EmailQueueConsumer(queue, emailSender, 3);
        queue.enqueueRegisterCode("user@example.com", "111111");

        consumer.consumeOne();

        EmailTask retryTask = queue.poll();
        assertThat(retryTask.getEmail()).isEqualTo("user@example.com");
        assertThat(retryTask.getRetryCount()).isEqualTo(1);
    }

    @Test
    void failedTaskIsDiscardedAfterMaxRetry() {
        EmailQueueAppService queue = new EmailQueueAppService(10, true);
        CapturingEmailSender emailSender = new CapturingEmailSender();
        emailSender.failRegister = true;
        EmailQueueConsumer consumer = new EmailQueueConsumer(queue, emailSender, 1);
        queue.enqueueRegisterCode("user@example.com", "111111");

        consumer.consumeOne();
        consumer.consumeOne();

        assertThat(queue.size()).isZero();
        assertThat(emailSender.registerSendCount).isEqualTo(2);
    }

    private static class CapturingEmailSender implements EmailSender {

        private boolean failRegister;
        private int registerSendCount;
        private String lastRegisterEmail;

        @Override
        public void sendRegisterCode(String email, String code) {
            registerSendCount++;
            lastRegisterEmail = email;
            if (failRegister) {
                throw new RuntimeException("send failed");
            }
        }

        @Override
        public void sendPasswordResetLink(String email, String username, String resetLink) {
            throw new UnsupportedOperationException();
        }
    }
}
