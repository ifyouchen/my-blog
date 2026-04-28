package com.myblog.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myblog.application.command.RecordAdminLogCommand;
import com.myblog.domain.model.aggregate.AdminLog;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.AdminLogRepository;
import com.myblog.shared.result.PageResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;

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
     * 异步记录管理员操作日志（不阻塞主业务流程）。
     * 使用 REQUIRES_NEW 独立事务，避免主事务回滚导致日志丢失。
     *
     * @param command 记录命令
     */
    @Async
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
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
     * @param operation 操作类型
     * @param resultStatus 结果状态
     * @param dateFrom 开始日期
     * @param dateTo 结束日期
     * @return 日志分页结果
     */
    public PageResult<Map<String, Object>> getLogs(int page, int pageSize, String operation,
                                                   String resultStatus, String dateFrom, String dateTo) {
        LocalDateTime normalizedDateFrom = parseDateStart(dateFrom);
        LocalDateTime normalizedDateTo = parseDateEnd(dateTo);
        validateDateRange(normalizedDateFrom, normalizedDateTo);
        List<AdminLog> adminLogs = adminLogRepository.findPage(
            page,
            pageSize,
            normalizeFilter(operation),
            normalizeFilter(resultStatus),
            normalizedDateFrom,
            normalizedDateTo
        );
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>(adminLogs.size());
        for (AdminLog adminLog : adminLogs) {
            items.add(toMap(adminLog));
        }
        return new PageResult<Map<String, Object>>(
            items,
            page,
            pageSize,
            adminLogRepository.countAll(
                normalizeFilter(operation),
                normalizeFilter(resultStatus),
                normalizedDateFrom,
                normalizedDateTo
            )
        );
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

    /**
     * 规范化筛选值。
     *
     * @param value 原始值
     * @return 规范化结果
     */
    private String normalizeFilter(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    /**
     * 校验日期区间。
     *
     * @param dateFrom 开始时间
     * @param dateTo 结束时间
     */
    private void validateDateRange(LocalDateTime dateFrom, LocalDateTime dateTo) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "开始日期不能晚于结束日期");
        }
    }

    /**
     * 解析开始日期。
     *
     * @param value 日期字符串
     * @return 开始时间
     */
    private LocalDateTime parseDateStart(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim()).atStartOfDay();
        } catch (DateTimeParseException exception) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "开始日期格式不正确，应为 yyyy-MM-dd");
        }
    }

    /**
     * 解析结束日期。
     *
     * @param value 日期字符串
     * @return 结束时间
     */
    private LocalDateTime parseDateEnd(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim()).atTime(LocalTime.MAX);
        } catch (DateTimeParseException exception) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "结束日期格式不正确，应为 yyyy-MM-dd");
        }
    }
}
