package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.myblog.growth.domain.repository.InviteRelationRepository;
import com.myblog.growth.infrastructure.repository.persistence.mapper.InviteRelationMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 邀请关系 Repository 实现.
 */
@Repository
public class InviteRelationRepositoryImpl implements InviteRelationRepository {

    private final InviteRelationMapper mapper;

    /**
     * 构造注入 Mapper.
     *
     * @param mapper 邀请关系 Mapper
     */
    public InviteRelationRepositoryImpl(InviteRelationMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByInviteeId(Long inviteeId) {
        return mapper.countByInviteeUserId(inviteeId) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int insertIgnore(Long inviterId, Long inviteeId) {
        return mapper.insertIgnore(inviterId, inviteeId);
    }

    /**
     * 将指定受邀人的邀请奖励状态更新为 GRANTED.
     *
     * @param inviteeId    受邀人用户 ID
     * @param triggerBizNo 触发奖励的业务单号
     */
    public void markGranted(Long inviteeId, String triggerBizNo) {
        mapper.markGranted(inviteeId, triggerBizNo);
    }

    /**
     * 统计邀请人今日已获得奖励次数.
     *
     * @param inviterId 邀请人用户 ID
     * @return 今日奖励次数
     */
    public int countGrantedTodayByInviterId(Long inviterId) {
        return mapper.countGrantedTodayByInviterId(inviterId);
    }

    /**
     * 统计邀请人累计获得奖励次数.
     *
     * @param inviterId 邀请人用户 ID
     * @return 累计奖励次数
     */
    public int countGrantedByInviterId(Long inviterId) {
        return mapper.countGrantedByInviterId(inviterId);
    }

    @Override
    public List<Map<String, Object>> selectInvitedUsers(Long inviterId) {
        return mapper.selectInvitedUsersByInviterId(inviterId);
    }
}

