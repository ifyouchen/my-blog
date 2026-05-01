package com.myblog.application.service;

import com.myblog.application.port.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 邮件队列消费者。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class EmailQueueConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailQueueConsumer.class);

    private final EmailQueueAppService emailQueueAppService;
    private final EmailSender emailSender;
    private final int maxRetry;

    public EmailQueueConsumer(EmailQueueAppService emailQueueAppService,
                              EmailSender emailSender,
                              @Value("${my-blog.mail.max-retry:3}") int maxRetry) {
        this.emailQueueAppService = emailQueueAppService;
        this.emailSender = emailSender;
        this.maxRetry = Math.max(maxRetry, 0);
    }

    @Scheduled(fixedDelayString = "${my-blog.mail.consumer-interval-ms:3000}")
    public void consumeOne() {
        EmailTask task = emailQueueAppService.poll();
        if (task == null) {
            return;
        }
        try {
            send(task);
        } catch (RuntimeException exception) {
            handleSendFailure(task, exception);
        }
    }

    private void send(EmailTask task) {
        if (EmailTaskType.REGISTER_CODE.equals(task.getType())) {
            emailSender.sendRegisterCode(task.getEmail(), task.getCode());
            return;
        }
        if (EmailTaskType.PASSWORD_RESET.equals(task.getType())) {
            emailSender.sendPasswordResetLink(task.getEmail(), task.getUsername(), task.getResetLink());
            return;
        }
        LOGGER.warn("未知邮件任务类型，type={}", task.getType());
    }

    private void handleSendFailure(EmailTask task, RuntimeException exception) {
        if (task.getRetryCount() >= maxRetry) {
            LOGGER.error("邮件发送失败且已超过最大重试次数，type={}, email={}", task.getType(), task.getEmail(), exception);
            return;
        }
        try {
            EmailTask retryTask = task.nextRetry();
            emailQueueAppService.requeue(retryTask);
            LOGGER.warn("邮件发送失败，已重新入队，type={}, email={}, retryCount={}",
                retryTask.getType(), retryTask.getEmail(), retryTask.getRetryCount(), exception);
        } catch (RuntimeException requeueException) {
            LOGGER.error("邮件发送失败且重新入队失败，type={}, email={}",
                task.getType(), task.getEmail(), requeueException);
        }
    }
}
