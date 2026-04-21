package com.myblog.infrastructure.repository.persistence.converter;

import com.myblog.domain.model.aggregate.AdminLog;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.infrastructure.repository.persistence.entity.AdminLogDO;

/**
 * 管理员操作日志持久化转换器。
 *
 * @author Codex
 * @since 1.0.0
 */
public final class AdminLogPersistenceConverter {

    private AdminLogPersistenceConverter() {
    }

    /**
     * 转换为领域对象。
     *
     * @param adminLogDO 数据对象
     * @return 领域对象
     */
    public static AdminLog toDomain(AdminLogDO adminLogDO) {
        if (adminLogDO == null) {
            return null;
        }
        return AdminLog.restore(
            adminLogDO.getId(),
            adminLogDO.getAdminUserId(),
            adminLogDO.getAdminUsername(),
            adminLogDO.getOperation(),
            adminLogDO.getTargetType(),
            adminLogDO.getTargetId(),
            adminLogDO.getDetail(),
            adminLogDO.getRequestMethod(),
            adminLogDO.getRequestUri(),
            adminLogDO.getIpAddress(),
            adminLogDO.getUserAgent(),
            adminLogDO.getResultStatus(),
            adminLogDO.getBeforeSnapshot(),
            adminLogDO.getAfterSnapshot(),
            adminLogDO.getCreatedAt(),
            adminLogDO.getUpdatedAt(),
            adminLogDO.getDeletedAt(),
            adminLogDO.getVersion()
        );
    }

    /**
     * 转换为数据对象。
     *
     * @param adminLog 领域对象
     * @return 数据对象
     */
    public static AdminLogDO toData(AdminLog adminLog) {
        AdminLogDO adminLogDO = new AdminLogDO();
        adminLogDO.setId(adminLog.getId().getValue());
        adminLogDO.setAdminUserId(adminLog.getAdminUserId().getValue());
        adminLogDO.setAdminUsername(adminLog.getAdminUsername());
        adminLogDO.setOperation(adminLog.getOperation());
        adminLogDO.setTargetType(adminLog.getTargetType());
        adminLogDO.setTargetId(adminLog.getTargetId());
        adminLogDO.setDetail(adminLog.getDetail());
        adminLogDO.setRequestMethod(adminLog.getRequestMethod());
        adminLogDO.setRequestUri(adminLog.getRequestUri());
        adminLogDO.setIpAddress(adminLog.getIpAddress());
        adminLogDO.setUserAgent(adminLog.getUserAgent());
        adminLogDO.setResultStatus(adminLog.getResultStatus());
        adminLogDO.setBeforeSnapshot(adminLog.getBeforeSnapshot());
        adminLogDO.setAfterSnapshot(adminLog.getAfterSnapshot());
        adminLogDO.setCreatedAt(adminLog.getCreatedAt());
        adminLogDO.setUpdatedAt(adminLog.getUpdatedAt());
        adminLogDO.setDeletedAt(adminLog.getDeletedAt());
        adminLogDO.setVersion(adminLog.getVersion());
        return adminLogDO;
    }
}
