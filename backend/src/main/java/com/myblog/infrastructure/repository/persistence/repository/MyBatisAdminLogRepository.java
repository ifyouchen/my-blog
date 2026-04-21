package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.model.aggregate.AdminLog;
import com.myblog.domain.repository.AdminLogRepository;
import com.myblog.infrastructure.repository.persistence.converter.AdminLogPersistenceConverter;
import com.myblog.infrastructure.repository.persistence.entity.AdminLogDO;
import com.myblog.infrastructure.repository.persistence.mapper.AdminLogMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理员操作日志 MyBatis 仓储实现。
 *
 * @author Codex
 * @since 1.0.0
 */
@Repository
public class MyBatisAdminLogRepository implements AdminLogRepository {

    private final AdminLogMapper adminLogMapper;

    /**
     * 创建管理员操作日志仓储。
     *
     * @param adminLogMapper 日志 Mapper
     */
    public MyBatisAdminLogRepository(AdminLogMapper adminLogMapper) {
        this.adminLogMapper = adminLogMapper;
    }

    /**
     * 保存管理员操作日志。
     *
     * @param adminLog 管理员操作日志
     * @return 保存后的日志
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminLog save(AdminLog adminLog) {
        adminLogMapper.insert(AdminLogPersistenceConverter.toData(adminLog));
        return adminLog;
    }

    /**
     * 分页查询管理员操作日志。
     *
     * @param page 页码
     * @param pageSize 每页数量
     * @return 日志列表
     */
    @Override
    public List<AdminLog> findPage(int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<AdminLogDO> adminLogDOList = adminLogMapper.selectPage(offset, pageSize);
        List<AdminLog> logs = new ArrayList<AdminLog>(adminLogDOList.size());
        for (AdminLogDO adminLogDO : adminLogDOList) {
            logs.add(AdminLogPersistenceConverter.toDomain(adminLogDO));
        }
        return logs;
    }

    /**
     * 统计管理员操作日志数量。
     *
     * @return 日志数量
     */
    @Override
    public int countAll() {
        return adminLogMapper.countAll();
    }

    /**
     * 获取下一个日志 ID。
     *
     * @return 日志 ID
     */
    @Override
    public Long nextId() {
        Long nextId = adminLogMapper.selectNextId();
        return nextId == null ? 101L : nextId;
    }
}
