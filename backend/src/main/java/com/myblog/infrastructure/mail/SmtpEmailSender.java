package com.myblog.infrastructure.mail;

import com.myblog.application.port.EmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * SMTP 邮件发送实现。
 *
 * @author Codex
 * @since 1.0.0
 */
@Component
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final String mailFrom;

    public SmtpEmailSender(JavaMailSender mailSender,
                           @Value("${my-blog.mail.from:no-reply@my-blog.local}") String mailFrom) {
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
    }

    @Override
    public void sendRegisterCode(String email, String code) {
        String text = "你正在注册 my-blog，验证码为：" + code + "\n\n"
            + "验证码 10 分钟内有效。如非本人操作，请忽略本邮件。";
        sendSimpleMail(email, "my-blog 注册验证码", text);
    }

    @Override
    public void sendPasswordResetLink(String email, String username, String resetLink) {
        String text = "你好，" + username + "：\n\n"
            + "你正在重置 my-blog 账号密码，请在 30 分钟内打开以下链接完成设置：\n"
            + resetLink + "\n\n"
            + "如非本人操作，请忽略本邮件。";
        sendSimpleMail(email, "my-blog 密码重置", text);
    }

    private void sendSimpleMail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
