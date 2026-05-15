package com.myblog.growth.interfaces.rest.dto.request;

/**
 * 充值回调请求体.
 * <p>对应 POST /api/open/pay/recharge/callback 接口，由支付平台异步回调。</p>
 */
public class RechargeCallbackRequest {

    /** 支付单号（幂等键，必填）. */
    private String payOrderNo;

    /** 用户 ID（必填）. */
    private Long userId;

    /** 充值金额（分，必填，必须 &gt; 0）. */
    private Long amountFen;

    /** 支付状态（SUCCESS / FAILED）. */
    private String status;

    /** 支付平台签名（必填）. */
    private String sign;

    /** 回调时间（字符串，用于验签）. */
    private String notifyTime;

    /** 原始回调报文（保存备查）. */
    private String rawPayload;

    /** 默认构造. */
    public RechargeCallbackRequest() {
    }

    public String getPayOrderNo() { return payOrderNo; }
    public void setPayOrderNo(String payOrderNo) { this.payOrderNo = payOrderNo; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getAmountFen() { return amountFen; }
    public void setAmountFen(Long amountFen) { this.amountFen = amountFen; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSign() { return sign; }
    public void setSign(String sign) { this.sign = sign; }

    public String getNotifyTime() { return notifyTime; }
    public void setNotifyTime(String notifyTime) { this.notifyTime = notifyTime; }

    public String getRawPayload() { return rawPayload; }
    public void setRawPayload(String rawPayload) { this.rawPayload = rawPayload; }
}

