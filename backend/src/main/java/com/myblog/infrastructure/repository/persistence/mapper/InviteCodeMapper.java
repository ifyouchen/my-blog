package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.InviteCodeDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 邀请码 MyBatis Mapper。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface InviteCodeMapper {

    InviteCodeDO selectById(@Param("id") Long id);

    InviteCodeDO selectByCode(@Param("code") String code);

    List<InviteCodeDO> selectByCreatorId(@Param("creatorId") Long creatorId);

    long countActiveByCreatorId(@Param("creatorId") Long creatorId);

    List<InviteCodeDO> selectPage(@Param("keyword") String keyword,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit);

    long countPage(@Param("keyword") String keyword);

    int insert(InviteCodeDO inviteCodeDO);

    int useCode(@Param("id") Long id,
                @Param("usedBy") Long usedBy,
                @Param("version") Integer version);

    int softDelete(@Param("id") Long id);
}

