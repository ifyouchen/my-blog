package com.myblog.application.service;

import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 邮件队列应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class EmailQueueAppService {

    private final BlockingQueue<EmailTask> queue;

    @Autowired
    public EmailQueueAppService(RedissonClient redissonClient) {
        this.queue = redissonClient.getBlockingQueue("email:queue");
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
        try {
            if (!queue.offer(task)) {
                throw new ApplicationException(ErrorCode.SYSTEM_ERROR, "邮件发送队列繁忙，请稍后重试");
            }
        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new ApplicationException(ErrorCode.SYSTEM_ERROR, "邮件发送队列繁忙，请稍后重试");
        }
    }
}
