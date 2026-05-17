package com.myblog.growth.interfaces.rest.dto.response;

import java.time.LocalDateTime;

/**
 * 管理端奖励领取记录展示对象.
 *
 * @author Codex
 * @since 2026-05-17
 */
public class AdminRewardGrantLogVO {

    private Long id;
    private Long userId;
    private String rewardType;
    private String rewardTypeLabel;
    private Long rewardId;
    private String rewardName;
    private int pointsGranted;
    private LocalDateTime grantedAt;
    private String remark;

    /**
     * 获取记录ID.
     *
     * @return 记录ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置记录ID.
     *
     * @param id 记录ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取用户ID.
     *
     * @return 用户ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置用户ID.
     *
     * @param userId 用户ID
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 获取奖励类型编码.
     *
     * @return 奖励类型编码
     */
    public String getRewardType() {
        return rewardType;
    }

    /**
     * 设置奖励类型编码.
     *
     * @param rewardType 奖励类型编码
     */
    public void setRewardType(String rewardType) {
        this.rewardType = rewardType;
    }

    /**
     * 获取奖励类型名称.
     *
     * @return 奖励类型名称
     */
    public String getRewardTypeLabel() {
        return rewardTypeLabel;
    }

    /**
     * 设置奖励类型名称.
     *
     * @param rewardTypeLabel 奖励类型名称
     */
    public void setRewardTypeLabel(String rewardTypeLabel) {
        this.rewardTypeLabel = rewardTypeLabel;
    }

    /**
     * 获取奖励配置ID.
     *
     * @return 奖励配置ID
     */
    public Long getRewardId() {
        return rewardId;
    }

    /**
     * 设置奖励配置ID.
     *
     * @param rewardId 奖励配置ID
     */
    public void setRewardId(Long rewardId) {
        this.rewardId = rewardId;
    }

    /**
     * 获取奖励名称.
     *
     * @return 奖励名称
     */
    public String getRewardName() {
        return rewardName;
    }

    /**
     * 设置奖励名称.
     *
     * @param rewardName 奖励名称
     */
    public void setRewardName(String rewardName) {
        this.rewardName = rewardName;
    }

    /**
     * 获取发放积分.
     *
     * @return 发放积分
     */
    public int getPointsGranted() {
        return pointsGranted;
    }

    /**
     * 设置发放积分.
     *
     * @param pointsGranted 发放积分
     */
    public void setPointsGranted(int pointsGranted) {
        this.pointsGranted = pointsGranted;
    }

    /**
     * 获取发放时间.
     *
     * @return 发放时间
     */
    public LocalDateTime getGrantedAt() {
        return grantedAt;
    }

    /**
     * 设置发放时间.
     *
     * @param grantedAt 发放时间
     */
    public void setGrantedAt(LocalDateTime grantedAt) {
        this.grantedAt = grantedAt;
    }

    /**
     * 获取备注.
     *
     * @return 备注
     */
    public String getRemark() {
        return remark;
    }

    /**
     * 设置备注.
     *
     * @param remark 备注
     */
    public void setRemark(String remark) {
        this.remark = remark;
    }
}
