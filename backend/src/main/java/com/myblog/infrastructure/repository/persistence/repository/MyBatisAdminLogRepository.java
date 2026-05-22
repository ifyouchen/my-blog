package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.model.aggregate.AdminLog;
import com.myblog.domain.repository.AdminLogRepository;
import com.myblog.infrastructure.repository.persistence.converter.AdminLogPersistenceConverter;
import com.myblog.infrastructure.repository.persistence.entity.AdminLogDO;
import com.myblog.infrastructure.repository.persistence.mapper.AdminLogMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final IdGenerator idGenerator;

    /**
     * 创建管理员操作日志仓储。
     *
     * @param adminLogMapper 日志 Mapper
     * @param idGenerator 全局 ID 生成器
     */
    public MyBatisAdminLogRepository(AdminLogMapper adminLogMapper, IdGenerator idGenerator) {
        this.adminLogMapper = adminLogMapper;
        this.idGenerator = idGenerator;
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
    public List<AdminLog> findPage(int page, int pageSize, String operation,
                                   String resultStatus, LocalDateTime dateFrom, LocalDateTime dateTo) {
        int offset = (page - 1) * pageSize;
        List<AdminLogDO> adminLogDOList = adminLogMapper.selectPage(
            offset,
            pageSize,
            operation,
            resultStatus,
            dateFrom,
            dateTo
        );
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
    public int countAll(String operation, String resultStatus, LocalDateTime dateFrom, LocalDateTime dateTo) {
        return adminLogMapper.countAll(operation, resultStatus, dateFrom, dateTo);
    }

    /**
     * 获取下一个日志 ID。
     *
     * @return 日志 ID
     */
    @Override
    public Long nextId() {
        return idGenerator.nextId("blog_admin_log");
    }
}
