package com.myblog.growth.domain.model.valueobject;

import java.time.LocalDateTime;

/**
 * 用户奖励领取记录值对象.
 * <p>
 * 记录用户已领取的奖励，防止重复发放。
 * </p>
 *
 * @author czx
 * @since 2026-05-17
 */
public class RewardGrantLog {

    private Long id;
    private Long userId;
    private String rewardType;
    private Long rewardId;
    private int pointsGranted;
    private LocalDateTime grantedAt;
    private String remark;

    /**
     * 无参构造，供持久化框架反射使用.
     */
    public RewardGrantLog() {
    }

    /**
     * 创建奖励领取记录.
     *
     * @param userId        用户ID
     * @param rewardType    奖励类型
     * @param rewardId      关联配置ID
     * @param pointsGranted 发放积分数
     * @param remark        备注
     * @return 新创建的记录
     */
    public static RewardGrantLog create(Long userId, String rewardType, Long rewardId,
                                         int pointsGranted, String remark) {
        RewardGrantLog log = new RewardGrantLog();
        log.userId = userId;
        log.rewardType = rewardType;
        log.rewardId = rewardId;
        log.pointsGranted = pointsGranted;
        log.grantedAt = LocalDateTime.now();
        log.remark = remark;
        return log;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRewardType() { return rewardType; }
    public void setRewardType(String rewardType) { this.rewardType = rewardType; }
    public Long getRewardId() { return rewardId; }
    public void setRewardId(Long rewardId) { this.rewardId = rewardId; }
    public int getPointsGranted() { return pointsGranted; }
    public void setPointsGranted(int pointsGranted) { this.pointsGranted = pointsGranted; }
    public LocalDateTime getGrantedAt() { return grantedAt; }
    public void setGrantedAt(LocalDateTime grantedAt) { this.grantedAt = grantedAt; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
