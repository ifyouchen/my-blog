package com.myblog.application.service;

import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public EmailQueueAppService(@Value("${my-blog.mail.queue-capacity:1000}") int queueCapacity) {
        this.queue = new LinkedBlockingQueue<EmailTask>(Math.max(queueCapacity, 1));
    }

    EmailQueueAppService(int queueCapacity, boolean testOnly) {
        this.queue = new LinkedBlockingQueue<EmailTask>(Math.max(queueCapacity, 1));
    }

    /**
     * 将注册验证码电子邮件任务入队。
     *
     * @param email 收件人邮筱
     * @param code  验证码
     */
    public void enqueueRegisterCode(String email, String code) {
        enqueue(EmailTask.registerCode(email, code));
    }

    /**
     * 将密码重置邮件任务入队。
     *
     * @param email     收件人邮筱
     * @param username  用户名
     * @param resetLink 密码重置链接
     */
    public void enqueuePasswordReset(String email, String username, String resetLink) {
        enqueue(EmailTask.passwordReset(email, username, resetLink));
    }

    /**
     * 从队列头部弹出一个任务，队列为空时返回 null。
     *
     * @return 邮件任务，队列为空时返回 null
     */
    public EmailTask poll() {
        return queue.poll();
    }

    /**
     * 获取队列当前长度。
     *
     * @return 队列长度
     */
    public int size() {
        return queue.size();
    }

    /**
     * 将失败任务重新入队（给消费者使用）。
     *
     * @param task 需要重新入队的任务
     */
    void requeue(EmailTask task) {
        enqueue(task);
    }

    /**
     * 将任务入队，队列满时抛出异常。
     *
     * @param task 邮件任务
     */
    private void enqueue(EmailTask task) {
        if (!queue.offer(task)) {
            throw new ApplicationException(ErrorCode.SYSTEM_ERROR, "邮件发送队列繁忙，请稍后重试");
        }
    }
}
