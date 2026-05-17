package com.myblog.growth.infrastructure.repository.persistence.mapper;

import com.myblog.growth.domain.model.valueobject.UserPrivilegeEntitlement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户权益授予记录 MyBatis Mapper.
 *
 * @author Codex
 * @since 2026-05-17
 */
@Mapper
public interface UserPrivilegeEntitlementMapper {

    /**
     * 查询单个用户的有效权益.
     *
     * @param userId 用户ID
     * @return 权益列表
     */
    List<UserPrivilegeEntitlement> selectActiveByUserId(@Param("userId") Long userId);

    /**
     * 批量查询多个用户的有效权益.
     *
     * @param userIds 用户ID列表
     * @return 权益列表
     */
    List<UserPrivilegeEntitlement> selectActiveByUserIds(@Param("userIds") List<Long> userIds);

    /**
     * 查询拥有指定权益的有效记录.
     *
     * @param privilegeCode 权益编码
     * @return 权益记录列表
     */
    List<UserPrivilegeEntitlement> selectActiveByPrivilegeCode(@Param("privilegeCode") String privilegeCode);

    /**
     * 判断用户是否拥有指定权益.
     *
     * @param userId        用户ID
     * @param privilegeCode 权益编码
     * @return 记录数
     */
    int countActiveByUserIdAndPrivilegeCode(@Param("userId") Long userId,
                                            @Param("privilegeCode") String privilegeCode);

    /**
     * 幂等插入权益记录.
     *
     * @param entitlement 权益记录
     * @return 插入行数
     */
    int insertIgnore(UserPrivilegeEntitlement entitlement);
}
