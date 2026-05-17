package com.myblog.growth.domain.repository;

import com.myblog.growth.domain.model.valueobject.UserPrivilegeEntitlement;

import java.util.List;
import java.util.Map;

/**
 * 用户权益授予记录仓储接口.
 *
 * @author Codex
 * @since 2026-05-17
 */
public interface UserPrivilegeEntitlementRepository {

    /**
     * 查询用户当前有效权益.
     *
     * @param userId 用户ID
     * @return 权益列表
     */
    List<UserPrivilegeEntitlement> findActiveByUserId(Long userId);

    /**
     * 批量查询多个用户的当前有效权益.
     *
     * @param userIds 用户ID列表
     * @return 用户ID到权益列表的映射
     */
    Map<Long, List<UserPrivilegeEntitlement>> findActiveByUserIds(List<Long> userIds);

    /**
     * 查询拥有指定权益的全部有效记录.
     *
     * @param privilegeCode 权益编码
     * @return 授予记录列表
     */
    List<UserPrivilegeEntitlement> findActiveByPrivilegeCode(String privilegeCode);

    /**
     * 判断用户是否拥有指定权益.
     *
     * @param userId        用户ID
     * @param privilegeCode 权益编码
     * @return true 表示拥有
     */
    boolean existsActiveByUserIdAndPrivilegeCode(Long userId, String privilegeCode);

    /**
     * 幂等保存权益记录.
     *
     * @param entitlement 权益记录
     * @return 1=插入成功，0=已存在
     */
    int insertIgnore(UserPrivilegeEntitlement entitlement);
}
