package com.myblog.growth.infrastructure.repository.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 邀请关系 MyBatis Mapper.
 * <p>对应表 {@code invite_relation}，XML 在 {@code mapper/growth/InviteRelationMapper.xml}。</p>
 */
@Mapper
public interface InviteRelationMapper {

    /**
     * 检查受邀人是否已存在邀请记录.
     *
     * @param inviteeUserId 受邀人用户 ID
     * @return 记录数（0 或 1）
     */
    int countByInviteeUserId(@Param("inviteeUserId") Long inviteeUserId);

    /**
     * INSERT IGNORE：uk_invitee 冲突时静默忽略.
     *
     * @param inviterUserId 邀请人用户 ID
     * @param inviteeUserId 受邀人用户 ID
     * @param inviteCode    使用的邀请码
     * @return 插入行数（1=成功，0=重复）
     */
    int insertIgnore(@Param("inviterUserId") Long inviterUserId,
                     @Param("inviteeUserId") Long inviteeUserId,
                     @Param("inviteCode") String inviteCode);

    /**
     * 将指定受邀人的邀请奖励状态更新为 GRANTED.
     *
     * @param inviteeUserId 受邀人用户 ID
     * @param triggerBizNo  触发奖励的业务单号
     */
    void markGranted(@Param("inviteeUserId") Long inviteeUserId,
                     @Param("triggerBizNo") String triggerBizNo);

    /**
     * 统计邀请人累计获得奖励次数.
     *
     * @param inviterUserId 邀请人用户 ID
     * @return 获得奖励次数
     */
    int countGrantedByInviterId(@Param("inviterUserId") Long inviterUserId);

    /**
     * 统计邀请人今日已获得奖励次数（用于每日频次风控）.
     *
     * @param inviterUserId 邀请人用户 ID
     * @return 今日获得奖励次数
     */
    int countGrantedTodayByInviterId(@Param("inviterUserId") Long inviterUserId);

    /**
     * 查询被邀请用户列表.
     *
     * @param inviterUserId 邀请人用户 ID
     * @return 被邀请用户列表（userId, username, invitedAt）
     */
    List<Map<String, Object>> selectInvitedUsersByInviterId(@Param("inviterUserId") Long inviterUserId);

    /**
     * 管理端统计邀请记录总数.
     *
     * @param keyword      搜索关键词（用户名/邀请码）
     * @param rewardStatus 奖励状态筛选
     * @return 记录总数
     */
    long countForAdmin(@Param("keyword") String keyword,
                       @Param("rewardStatus") String rewardStatus);

    /**
     * 管理端分页查询邀请记录.
     *
     * @param keyword      搜索关键词
     * @param rewardStatus 奖励状态筛选
     * @param offset       偏移量
     * @param limit        每页条数
     * @return 邀请记录列表
     */
    List<Map<String, Object>> selectPageForAdmin(@Param("keyword") String keyword,
                                                 @Param("rewardStatus") String rewardStatus,
                                                 @Param("offset") int offset,
                                                 @Param("limit") int limit);
}

