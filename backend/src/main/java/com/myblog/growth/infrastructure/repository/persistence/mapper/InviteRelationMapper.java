package com.myblog.growth.infrastructure.repository.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
     * @return 插入行数（1=成功，0=重复）
     */
    int insertIgnore(@Param("inviterUserId") Long inviterUserId,
                     @Param("inviteeUserId") Long inviteeUserId);

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
}

