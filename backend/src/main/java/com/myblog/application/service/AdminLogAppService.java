package com.myblog.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myblog.application.command.RecordAdminLogCommand;
import com.myblog.domain.model.aggregate.AdminLog;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.AdminLogRepository;
import com.myblog.shared.result.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员操作日志应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class AdminLogAppService {

    private final AdminLogRepository adminLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * 创建管理员操作日志应用服务。
     *
     * @param adminLogRepository 管理员日志仓储
     * @param objectMapper Jackson 对象映射器
     */
    public AdminLogAppService(AdminLogRepository adminLogRepository, ObjectMapper objectMapper) {
        this.adminLogRepository = adminLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 记录管理员操作日志。
     *
     * @param command 记录命令
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordOperation(RecordAdminLogCommand command) {
        AdminLog adminLog = AdminLog.create(
            adminLogRepository.nextId(),
            new UserId(command.getAdminUserId()),
            command.getAdminUsername(),
            command.getOperation(),
            command.getTargetType(),
            command.getTargetId(),
            command.getDetail(),
            command.getRequestMethod(),
            command.getRequestUri(),
            command.getIpAddress(),
            command.getUserAgent(),
            command.getResultStatus(),
            serializeSnapshot(command.getBeforeSnapshot()),
            serializeSnapshot(command.getAfterSnapshot())
        );
        adminLogRepository.save(adminLog);
    }

    /**
     * 分页查询管理员操作日志。
     *
     * @param page 页码
     * @param pageSize 每页数量
     * @return 日志分页结果
     */
    public PageResult<Map<String, Object>> getLogs(int page, int pageSize) {
        List<AdminLog> adminLogs = adminLogRepository.findPage(page, pageSize);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>(adminLogs.size());
        for (AdminLog adminLog : adminLogs) {
            items.add(toMap(adminLog));
        }
        return new PageResult<Map<String, Object>>(items, page, pageSize, adminLogRepository.countAll());
    }

    /**
     * 将日志聚合转换为接口响应结构。
     *
     * @param adminLog 管理员日志
     * @return Map 结构
     */
    public Map<String, Object> toMap(AdminLog adminLog) {
        Map<String, Object> item = new HashMap<String, Object>();
        item.put("id", adminLog.getId().getValue());
        item.put("adminUserId", adminLog.getAdminUserId().getValue());
        item.put("adminUsername", adminLog.getAdminUsername());
        item.put("operation", adminLog.getOperation());
        item.put("targetType", adminLog.getTargetType());
        item.put("targetId", adminLog.getTargetId());
        item.put("detail", adminLog.getDetail());
        item.put("requestMethod", adminLog.getRequestMethod());
        item.put("requestUri", adminLog.getRequestUri());
        item.put("ipAddress", adminLog.getIpAddress());
        item.put("userAgent", adminLog.getUserAgent());
        item.put("resultStatus", adminLog.getResultStatus());
        item.put("beforeSnapshot", adminLog.getBeforeSnapshot());
        item.put("afterSnapshot", adminLog.getAfterSnapshot());
        item.put("createdAt", adminLog.getCreatedAt());
        return item;
    }

    /**
     * 序列化快照对象。
     *
     * @param snapshot 快照对象
     * @return JSON 字符串
     */
    private String serializeSnapshot(Object snapshot) {
        if (snapshot == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("管理员日志快照序列化失败", exception);
        }
    }
}
