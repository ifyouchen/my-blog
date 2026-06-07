package com.myblog.growth.domain.repository;

import java.util.List;
import java.util.Map;

/**
 * 邀请关系 Repository 接口.
 */
public interface InviteRelationRepository {

    /**
     * 检查受邀人是否已有邀请记录（每人只能被邀请一次）.
     *
     * @param inviteeId 受邀人用户 ID
     * @return {@code true} 表示已存在邀请关系
     */
    boolean existsByInviteeId(Long inviteeId);

    /**
     * 保存邀请关系（INSERT IGNORE 防重）.
     *
     * @param inviterId 邀请人用户 ID
     * @param inviteeId 受邀人用户 ID
     * @param inviteCode 使用的邀请码
     * @return 插入行数（1=成功，0=已存在）
     */
    int insertIgnore(Long inviterId, Long inviteeId, String inviteCode);

    /**
     * 查询被邀请用户列表.
     *
     * @param inviterId 邀请人用户 ID
     * @return 被邀请用户列表
     */
    List<Map<String, Object>> selectInvitedUsers(Long inviterId);

    /**
     * 管理端统计邀请记录总数.
     */
    long countForAdmin(String keyword, String rewardStatus);

    /**
     * 管理端分页查询邀请记录.
     */
    List<Map<String, Object>> findPageForAdmin(String keyword, String rewardStatus, int offset, int limit);
}

