package com.myblog.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * JWT Token 工具。
 *
 * @author Codex
 * @since 1.0.0
 */
@Component
public class JwtTokenProvider {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final String secret;
    private final long expireSeconds;
    private final ObjectMapper objectMapper;

    /**
     * 创建 JWT Token 工具。
     *
     * @param secret 签名密钥
     * @param expireSeconds 过期秒数
     * @param objectMapper JSON 转换器
     */
    public JwtTokenProvider(@Value("${my-blog.jwt.secret}") String secret,
                            @Value("${my-blog.jwt.expire-seconds}") long expireSeconds,
                            ObjectMapper objectMapper) {
        this.secret = secret;
        this.expireSeconds = expireSeconds;
        this.objectMapper = objectMapper;
    }

    /**
     * 创建 Token。
     *
     * @param userId 用户 ID
     * @param username 用户名
     * @param role 用户角色
     * @return JWT Token
     */
    public String createToken(Long userId, String username, String role) {
        try {
            String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
            JwtPayload payload = new JwtPayload();
            payload.setUserId(userId);
            payload.setUsername(username);
            payload.setRole(role);
            payload.setExpireAt(System.currentTimeMillis() + expireSeconds * 1000L);
            String body = base64Url(objectMapper.writeValueAsBytes(payload));
            String signature = sign(header + "." + body);
            return header + "." + body + "." + signature;
        } catch (Exception e) {
            throw new ApplicationException(ErrorCode.SYSTEM_ERROR, "Token生成失败");
        }
    }

    /**
     * 解析并校验 Token。
     *
     * @param token JWT Token
     * @return Token 载荷
     */
    public JwtPayload parseToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new ApplicationException(ErrorCode.UNAUTHORIZED, "Token格式错误");
            }
            String expectedSignature = sign(parts[0] + "." + parts[1]);
            if (!expectedSignature.equals(parts[2])) {
                throw new ApplicationException(ErrorCode.UNAUTHORIZED, "Token签名无效");
            }
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            JwtPayload payload = objectMapper.readValue(payloadBytes, JwtPayload.class);
            if (payload.getExpireAt() < System.currentTimeMillis()) {
                throw new ApplicationException(ErrorCode.UNAUTHORIZED, "Token已过期");
            }
            return payload;
        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "Token解析失败");
        }
    }

    private String sign(String content) throws Exception {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        mac.init(keySpec);
        return base64Url(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
