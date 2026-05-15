package com.myblog.growth.domain.service;

import com.myblog.growth.shared.exception.GrowthBusinessException;
import com.myblog.growth.shared.exception.GrowthErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 充值回调验签服务.
 *
 * <p>
 * 使用 HMAC-SHA256 对回调报文中的核心字段进行验签，防止伪造充值回调。
 * 签名密钥通过配置文件注入，禁止硬编码。
 * </p>
 *
 * <p>签名规则：</p>
 * <pre>
 * 待签字符串 = payOrderNo + "|" + userId + "|" + amountFen + "|" + notifyTime
 * sign = HMAC-SHA256(key=签名密钥, data=待签字符串)（Base64Url 编码）
 * </pre>
 */
@Service
public class RechargeSignVerifyService {

    private static final String HMAC_SHA256 = "HmacSHA256";

    /** 签名密钥，从配置文件注入. */
    private final String signSecret;

    /**
     * 构造注入签名密钥.
     *
     * @param signSecret 签名密钥（默认值仅用于开发/测试环境）
     */
    public RechargeSignVerifyService(
            @Value("${my-blog.growth.recharge.sign-secret:dev-secret-change-in-prod}") String signSecret) {
        this.signSecret = signSecret;
    }

    /**
     * 验签.
     *
     * @param payOrderNo  支付订单号
     * @param userId      用户 ID
     * @param amountFen   金额（分）
     * @param notifyTime  回调时间字符串
     * @param sign        请求中的签名
     * @throws GrowthBusinessException 验签失败时抛出
     */
    public void verify(String payOrderNo, Long userId, long amountFen,
                       String notifyTime, String sign) {
        if (sign == null || sign.trim().isEmpty()) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "签名不能为空");
        }
        String expectedSign = buildSign(payOrderNo, userId, amountFen, notifyTime);
        // 使用常量时间比较，防时序攻击
        if (!MessageDigest.isEqual(
                expectedSign.getBytes(StandardCharsets.UTF_8),
                sign.getBytes(StandardCharsets.UTF_8))) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "验签失败，签名不匹配");
        }
    }

    /**
     * 构建签名字符串.
     *
     * @param payOrderNo 支付订单号
     * @param userId     用户 ID
     * @param amountFen  金额（分）
     * @param notifyTime 回调时间
     * @return Base64Url 编码的签名
     */
    private String buildSign(String payOrderNo, Long userId, long amountFen, String notifyTime) {
        try {
            String data = payOrderNo + "|" + userId + "|" + amountFen + "|" + notifyTime;
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec keySpec = new SecretKeySpec(
                    signSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(keySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "签名计算异常：" + e.getMessage());
        }
    }
}

