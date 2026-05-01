package com.myblog.application.service;

import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * 邮件队列应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class EmailQueueAppService {

    private final LinkedBlockingQueue<EmailTask> queue;

    public EmailQueueAppService(@Value("${my-blog.mail.queue-capacity:1000}") int queueCapacity) {
        this.queue = new LinkedBlockingQueue<EmailTask>(Math.max(queueCapacity, 1));
    }

    EmailQueueAppService(int queueCapacity, boolean testOnly) {
        this.queue = new LinkedBlockingQueue<EmailTask>(Math.max(queueCapacity, 1));
    }

    public void enqueueRegisterCode(String email, String code) {
        enqueue(EmailTask.registerCode(email, code));
    }

    public void enqueuePasswordReset(String email, String username, String resetLink) {
        enqueue(EmailTask.passwordReset(email, username, resetLink));
    }

    public EmailTask poll() {
        return queue.poll();
    }

    public int size() {
        return queue.size();
    }

    void requeue(EmailTask task) {
        enqueue(task);
    }

    private void enqueue(EmailTask task) {
        if (!queue.offer(task)) {
            throw new ApplicationException(ErrorCode.SYSTEM_ERROR, "邮件发送队列繁忙，请稍后重试");
        }
    }
}
