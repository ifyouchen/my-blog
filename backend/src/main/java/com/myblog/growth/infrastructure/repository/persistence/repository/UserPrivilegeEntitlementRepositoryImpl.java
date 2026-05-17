package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.myblog.growth.domain.model.valueobject.UserPrivilegeEntitlement;
import com.myblog.growth.domain.repository.UserPrivilegeEntitlementRepository;
import com.myblog.growth.infrastructure.repository.persistence.mapper.UserPrivilegeEntitlementMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户权益授予记录仓储实现.
 *
 * @author Codex
 * @since 2026-05-17
 */
@Repository
public class UserPrivilegeEntitlementRepositoryImpl implements UserPrivilegeEntitlementRepository {

    private final UserPrivilegeEntitlementMapper mapper;

    public UserPrivilegeEntitlementRepositoryImpl(UserPrivilegeEntitlementMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<UserPrivilegeEntitlement> findActiveByUserId(Long userId) {
        return mapper.selectActiveByUserId(userId);
    }

    @Override
    public Map<Long, List<UserPrivilegeEntitlement>> findActiveByUserIds(List<Long> userIds) {
        Map<Long, List<UserPrivilegeEntitlement>> result = new HashMap<Long, List<UserPrivilegeEntitlement>>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }
        List<UserPrivilegeEntitlement> items = mapper.selectActiveByUserIds(userIds);
        for (UserPrivilegeEntitlement item : items) {
            List<UserPrivilegeEntitlement> list = result.get(item.getUserId());
            if (list == null) {
                list = new ArrayList<UserPrivilegeEntitlement>();
                result.put(item.getUserId(), list);
            }
            list.add(item);
        }
        return result;
    }

    @Override
    public List<UserPrivilegeEntitlement> findActiveByPrivilegeCode(String privilegeCode) {
        return mapper.selectActiveByPrivilegeCode(privilegeCode);
    }

    @Override
    public boolean existsActiveByUserIdAndPrivilegeCode(Long userId, String privilegeCode) {
        return mapper.countActiveByUserIdAndPrivilegeCode(userId, privilegeCode) > 0;
    }

    @Override
    public int insertIgnore(UserPrivilegeEntitlement entitlement) {
        return mapper.insertIgnore(entitlement);
    }
}
