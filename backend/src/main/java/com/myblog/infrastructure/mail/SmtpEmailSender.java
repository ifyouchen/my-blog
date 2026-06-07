package com.myblog.infrastructure.mail;

import com.myblog.application.port.EmailSender;
import javax.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * SMTP 邮件发送实现（HTML 卡片样式）。
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
        String html = buildRegisterCodeHtml(code);
        sendHtmlMail(email, "小蓝书 注册验证码", html);
    }

    @Override
    public void sendPasswordResetLink(String email, String username, String resetLink) {
        String html = buildPasswordResetHtml(username, resetLink);
        sendHtmlMail(email, "小蓝书 密码重置", html);
    }

    private void sendHtmlMail(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("发送邮件失败", e);
        }
    }

    private String buildRegisterCodeHtml(String code) {
        return "<!DOCTYPE html>\n"
            + "<html>\n"
            + "<head>\n"
            + "<meta charset=\"UTF-8\">\n"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "<style>\n"
            + "  @media only screen and (max-width: 600px) {\n"
            + "    .container { padding: 12px !important; }\n"
            + "    .card { max-width: 100% !important; }\n"
            + "    .body-cell { padding: 28px 20px !important; }\n"
            + "    .code-text { font-size: 30px !important; letter-spacing: 6px !important; }\n"
            + "    .brand-title { font-size: 18px !important; }\n"
            + "  }\n"
            + "</style>\n"
            + "</head>\n"
            + "<body style=\"margin:0;padding:0;background:#f0f5ff;font-family:-apple-system,BlinkMacSystemFont,'Helvetica Neue',Arial,sans-serif;\">\n"
            + "<table role=\"presentation\" class=\"container\" style=\"width:100%;padding:20px;\" bgcolor=\"#f0f5ff\">\n"
            + "<tr><td align=\"center\">\n"
            + "<table class=\"card\" style=\"width:100%;max-width:480px;background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 24px rgba(37,99,235,0.08);\">\n"
            // ── Header ──
            + "<tr><td style=\"background:linear-gradient(135deg,#2563eb,#1d4ed8);padding:18px 20px;text-align:center;\">\n"
            + "  <svg xmlns=\"http://www.w3.org/2000/svg\" width=\"28\" height=\"28\" viewBox=\"0 0 96 96\" style=\"display:block;margin:0 auto 6px;\">\n"
            + "    <rect width=\"96\" height=\"96\" rx=\"18\" fill=\"#ffffff\"/>\n"
            + "    <path d=\"M48 76 L12 64 L20 22 L48 34 Z\" fill=\"#2563eb\" opacity=\"0.9\"/>\n"
            + "    <path d=\"M48 76 L84 64 L76 22 L48 34 Z\" fill=\"#2563eb\" opacity=\"0.9\"/>\n"
            + "    <line x1=\"48\" y1=\"34\" x2=\"48\" y2=\"76\" stroke=\"#bfdbfe\" stroke-width=\"3\" stroke-linecap=\"round\"/>\n"
            + "    <line x1=\"22\" y1=\"30\" x2=\"44\" y2=\"40\" stroke=\"#e2e8f0\" stroke-width=\"2.5\" stroke-linecap=\"round\"/>\n"
            + "    <line x1=\"20\" y1=\"44\" x2=\"44\" y2=\"52\" stroke=\"#e2e8f0\" stroke-width=\"2.5\" stroke-linecap=\"round\"/>\n"
            + "    <line x1=\"52\" y1=\"40\" x2=\"74\" y2=\"30\" stroke=\"#e2e8f0\" stroke-width=\"2.5\" stroke-linecap=\"round\"/>\n"
            + "    <line x1=\"52\" y1=\"52\" x2=\"76\" y2=\"44\" stroke=\"#e2e8f0\" stroke-width=\"2.5\" stroke-linecap=\"round\"/>\n"
            + "  </svg>\n"
            + "  <h1 class=\"brand-title\" style=\"margin:0;color:#ffffff;font-size:20px;font-weight:700;letter-spacing:0.5px;\">小蓝书</h1>\n"
            + "</td></tr>\n"
            // ── Body ──
            + "<tr><td class=\"body-cell\" style=\"padding:36px 28px;text-align:center;\">\n"
            + "  <p style=\"margin:0 0 16px;color:#475569;font-size:15px;line-height:1.6;\">\n"
            + "    你正在注册小蓝书，以下是您的验证码：\n"
            + "  </p>\n"
            + "  <div style=\"margin:0 auto 20px;padding:16px;background:#f0f5ff;border-radius:10px;display:inline-block;\">\n"
            + "    <span class=\"code-text\" style=\"font-size:36px;font-weight:800;letter-spacing:8px;color:#2563eb;font-family:Consolas,'Courier New',monospace;\">"
            + code + "</span>\n"
            + "  </div>\n"
            + "  <p style=\"margin:0;color:#94a3b8;font-size:13px;line-height:1.5;\">\n"
            + "    验证码 10 分钟内有效，请尽快完成注册。\n"
            + "  </p>\n"
            + "</td></tr>\n"
            // ── Footer ──
            + "<tr><td style=\"padding:14px 28px;background:#f8fafc;border-top:1px solid #e2e8f0;text-align:center;\">\n"
            + "  <p style=\"margin:0;color:#94a3b8;font-size:12px;line-height:1.5;\">\n"
            + "    如非本人操作，请忽略本邮件\n"
            + "  </p>\n"
            + "</td></tr>\n"
            + "</table>\n"
            + "</td></tr>\n"
            + "</table>\n"
            + "</body>\n"
            + "</html>";
    }

    private String buildPasswordResetHtml(String username, String resetLink) {
        return "<!DOCTYPE html>\n"
            + "<html>\n"
            + "<head>\n"
            + "<meta charset=\"UTF-8\">\n"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "<style>\n"
            + "  @media only screen and (max-width: 600px) {\n"
            + "    .container { padding: 12px !important; }\n"
            + "    .card { max-width: 100% !important; }\n"
            + "    .body-cell { padding: 28px 20px !important; }\n"
            + "    .btn { padding: 14px 20px !important; font-size: 14px !important; }\n"
            + "    .brand-title { font-size: 18px !important; }\n"
            + "  }\n"
            + "</style>\n"
            + "</head>\n"
            + "<body style=\"margin:0;padding:0;background:#f0f5ff;font-family:-apple-system,BlinkMacSystemFont,'Helvetica Neue',Arial,sans-serif;\">\n"
            + "<table role=\"presentation\" class=\"container\" style=\"width:100%;padding:20px;\" bgcolor=\"#f0f5ff\">\n"
            + "<tr><td align=\"center\">\n"
            + "<table class=\"card\" style=\"width:100%;max-width:480px;background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 24px rgba(37,99,235,0.08);\">\n"
            // ── Header ──
            + "<tr><td style=\"background:linear-gradient(135deg,#2563eb,#1d4ed8);padding:18px 20px;text-align:center;\">\n"
            + "  <svg xmlns=\"http://www.w3.org/2000/svg\" width=\"28\" height=\"28\" viewBox=\"0 0 96 96\" style=\"display:block;margin:0 auto 6px;\">\n"
            + "    <rect width=\"96\" height=\"96\" rx=\"18\" fill=\"#ffffff\"/>\n"
            + "    <path d=\"M48 76 L12 64 L20 22 L48 34 Z\" fill=\"#2563eb\" opacity=\"0.9\"/>\n"
            + "    <path d=\"M48 76 L84 64 L76 22 L48 34 Z\" fill=\"#2563eb\" opacity=\"0.9\"/>\n"
            + "    <line x1=\"48\" y1=\"34\" x2=\"48\" y2=\"76\" stroke=\"#bfdbfe\" stroke-width=\"3\" stroke-linecap=\"round\"/>\n"
            + "    <line x1=\"22\" y1=\"30\" x2=\"44\" y2=\"40\" stroke=\"#e2e8f0\" stroke-width=\"2.5\" stroke-linecap=\"round\"/>\n"
            + "    <line x1=\"20\" y1=\"44\" x2=\"44\" y2=\"52\" stroke=\"#e2e8f0\" stroke-width=\"2.5\" stroke-linecap=\"round\"/>\n"
            + "    <line x1=\"52\" y1=\"40\" x2=\"74\" y2=\"30\" stroke=\"#e2e8f0\" stroke-width=\"2.5\" stroke-linecap=\"round\"/>\n"
            + "    <line x1=\"52\" y1=\"52\" x2=\"76\" y2=\"44\" stroke=\"#e2e8f0\" stroke-width=\"2.5\" stroke-linecap=\"round\"/>\n"
            + "  </svg>\n"
            + "  <h1 class=\"brand-title\" style=\"margin:0;color:#ffffff;font-size:20px;font-weight:700;letter-spacing:0.5px;\">小蓝书</h1>\n"
            + "</td></tr>\n"
            // ── Body ──
            + "<tr><td class=\"body-cell\" style=\"padding:36px 28px;\">\n"
            + "  <p style=\"margin:0 0 12px;color:#475569;font-size:15px;line-height:1.6;\">\n"
            + "    你好，<strong style=\"color:#1e293b;\">" + escapeHtml(username) + "</strong>：\n"
            + "  </p>\n"
            + "  <p style=\"margin:0 0 24px;color:#475569;font-size:15px;line-height:1.6;\">\n"
            + "    你正在重置小蓝书账号密码，请在 30 分钟内点击下方按钮完成设置：\n"
            + "  </p>\n"
            + "  <table role=\"presentation\" style=\"margin:0 auto 28px;\">\n"
            + "    <tr>\n"
            + "      <td align=\"center\" style=\"border-radius:10px;box-shadow:0 4px 14px rgba(37,99,235,0.35);background-color:#1d4ed8;\">\n"
            + "        <a class=\"btn\" href=\"" + escapeHtml(resetLink) + "\""
            + " style=\"display:inline-block;padding:15px 44px;font-size:16px;font-weight:700;text-decoration:none;white-space:nowrap;background:linear-gradient(135deg,#3b82f6,#1d4ed8);border-radius:10px;border:1px solid rgba(255,255,255,0.15);\">\n"
            + "          <span style=\"color:#ffffff;\">重置密码</span>\n"
            + "        </a>\n"
            + "      </td>\n"
            + "    </tr>\n"
            + "  </table>\n"
            + "  <p style=\"margin:0 0 4px;color:#94a3b8;font-size:13px;line-height:1.5;\">\n"
            + "    如果按钮无法点击，请复制以下链接在浏览器中打开：\n"
            + "  </p>\n"
            + "  <p style=\"margin:0;color:#2563eb;font-size:12px;line-height:1.5;word-break:break-all;\">\n"
            + "    " + escapeHtml(resetLink) + "\n"
            + "  </p>\n"
            + "</td></tr>\n"
            // ── Footer ──
            + "<tr><td style=\"padding:14px 28px;background:#f8fafc;border-top:1px solid #e2e8f0;text-align:center;\">\n"
            + "  <p style=\"margin:0;color:#94a3b8;font-size:12px;line-height:1.5;\">\n"
            + "    如非本人操作，请忽略本邮件\n"
            + "  </p>\n"
            + "</td></tr>\n"
            + "</table>\n"
            + "</td></tr>\n"
            + "</table>\n"
            + "</body>\n"
            + "</html>";
    }

    /**
     * HTML 转义，防止注入.
     */
    private static String escapeHtml(String input) {
        if (input == null) return "";
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
