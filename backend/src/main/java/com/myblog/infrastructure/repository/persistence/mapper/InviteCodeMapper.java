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

    /**
     * 根据 ID 查询邀请码。
     *
     * @param id 邀请码 ID
     * @return 邀请码数据对象
     */
    InviteCodeDO selectById(@Param("id") Long id);

    /**
     * 根据邀请码查询邀请码记录。
     *
     * @param code 邀请码
     * @return 邀请码数据对象
     */
    InviteCodeDO selectByCode(@Param("code") String code);

    /**
     * 根据创建者 ID 查询邀请码列表。
     *
     * @param creatorId 创建者用户 ID
     * @return 邀请码列表
     */
    List<InviteCodeDO> selectByCreatorId(@Param("creatorId") Long creatorId);

    /**
     * 统计创建者的有效邀请码数量。
     *
     * @param creatorId 创建者用户 ID
     * @return 有效邀请码数量
     */
    long countActiveByCreatorId(@Param("creatorId") Long creatorId);

    /**
     * 分页查询邀请码列表。
     *
     * @param keyword 关键字
     * @param offset  偏移量
     * @param limit   限制数量
     * @return 邀请码列表
     */
    List<InviteCodeDO> selectPage(@Param("keyword") String keyword,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit);

    /**
     * 统计邀请码数量。
     *
     * @param keyword 关键字
     * @return 邀请码数量
     */
    long countPage(@Param("keyword") String keyword);

    /**
     * 插入邀请码记录。
     *
     * @param inviteCodeDO 邀请码数据对象
     * @return 影响行数
     */
    int insert(InviteCodeDO inviteCodeDO);

    /**
     * 使用邀请码（乐观锁更新）。
     *
     * @param id      邀请码 ID
     * @param usedBy  使用者用户 ID
     * @param version 乐观锁版本号
     * @return 影响行数
     */
    int useCode(@Param("id") Long id,
                @Param("usedBy") Long usedBy,
                @Param("version") Integer version);

    /**
     * 软删除邀请码。
     *
     * @param id 邀请码 ID
     * @return 影响行数
     */
    int softDelete(@Param("id") Long id);
}

