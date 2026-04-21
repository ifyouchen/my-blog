package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.AdminLog;

import java.util.List;

/**
 * 管理员操作日志仓储接口。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface AdminLogRepository {

    /**
     * 保存管理员操作日志。
     *
     * @param adminLog 管理员操作日志
     * @return 保存后的日志
     */
    AdminLog save(AdminLog adminLog);

    /**
     * 分页查询管理员操作日志。
     *
     * @param page 页码
     * @param pageSize 每页数量
     * @return 日志列表
     */
    List<AdminLog> findPage(int page, int pageSize);

    /**
     * 统计管理员操作日志数量。
     *
     * @return 日志数量
     */
    int countAll();

    /**
     * 获取下一个日志 ID。
     *
     * @return 日志 ID
     */
    Long nextId();
}
