package com.myblog.interfaces.rest.controller;

import com.myblog.application.command.RecordAdminLogCommand;
import com.myblog.application.command.ResolveReportCommand;
import com.myblog.application.dto.ReportDTO;
import com.myblog.application.service.AdminLogAppService;
import com.myblog.application.service.ReportAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.interfaces.rest.dto.request.ResolveReportRequest;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.result.Result;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 后台举报管理接口。
 *
 * @author Codex
 * @since 1.0.0
 */
@Validated
@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    private final ReportAppService reportAppService;
    private final AdminLogAppService adminLogAppService;

    public AdminReportController(ReportAppService reportAppService, AdminLogAppService adminLogAppService) {
        this.reportAppService = reportAppService;
        this.adminLogAppService = adminLogAppService;
    }

    /**
     * 分页查询举报列表。
     *
     * @param page       页码
     * @param pageSize   每页数量
     * @param status     举报状态过滤
     * @param targetType 举报目标类型过滤
     * @param reasonType 举报原因类型过滤
     * @return 举报分页结果
     */
    @GetMapping
    public Result<PageResult<ReportDTO>> pageReports(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int pageSize,
                                                     @RequestParam(required = false) String status,
                                                     @RequestParam(required = false) String targetType,
                                                     @RequestParam(required = false) String reasonType) {
        ensureAdmin();
        return Result.success(reportAppService.pageReports(status, targetType, reasonType, page, pageSize));
    }

    /**
     * 获取举报详情。
     *
     * @param id 举报 ID
     * @return 举报详情
     */
    @GetMapping("/{id}")
    public Result<ReportDTO> getReport(@PathVariable Long id) {
        ensureAdmin();
        return Result.success(reportAppService.getReportDetail(id));
    }

    /**
     * 处理举报。
     *
     * @param id                 举报 ID
     * @param request            处理请求
     * @param httpServletRequest HTTP 请求
     * @return 处理后的举报信息
     */
    @PostMapping("/{id}/resolve")
    public Result<ReportDTO> resolveReport(@PathVariable Long id,
                                           @RequestBody @Valid ResolveReportRequest request,
                                           @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        ReportDTO before = reportAppService.getReportDetail(id);
        ResolveReportCommand command = new ResolveReportCommand();
        command.setReportId(id);
        command.setHandlerUserId(AuthContext.getRequiredUserId());
        command.setAction(request.getAction());
        command.setHandleNote(request.getHandleNote());
        ReportDTO after = reportAppService.resolveReport(command);
        adminLogAppService.recordOperation(buildLogCommand(
            "RESOLVE_REPORT",
            "REPORT",
            id,
            "处理举报 #" + id + "，action=" + request.getAction(),
            toReportSnapshot(before),
            toReportSnapshot(after),
            httpServletRequest
        ));
        return Result.success(after);
    }

    /**
     * 校验管理员权限，非管理员时抛出异常。
     */
    private void ensureAdmin() {
        AuthContext.getRequiredUserId();
        if (!"ADMIN".equals(AuthContext.getRole())) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "仅管理员可访问后台");
        }
    }

    /**
     * 构建举报快照。
     *
     * @param dto 举报 DTO
     * @return 快照数据
     */
    private Map<String, Object> toReportSnapshot(ReportDTO dto) {
        Map<String, Object> snapshot = new LinkedHashMap<String, Object>();
        snapshot.put("id", dto.getId());
        snapshot.put("targetType", dto.getTargetType());
        snapshot.put("targetId", dto.getTargetId());
        snapshot.put("reasonType", dto.getReasonType());
        snapshot.put("status", dto.getStatus());
        snapshot.put("handlerUserId", dto.getHandlerUserId());
        snapshot.put("handleNote", dto.getHandleNote());
        return snapshot;
    }

    /**
     * 构建管理员日志命令。
     *
     * @param operation      操作类型
     * @param targetType     目标类型
     * @param targetId       目标 ID
     * @param detail         操作详情
     * @param beforeSnapshot 变更前快照
     * @param afterSnapshot  变更后快照
     * @param request        HTTP 请求
     * @return 日志命令
     */
    private RecordAdminLogCommand buildLogCommand(String operation, String targetType, Long targetId,
                                                  String detail, Object beforeSnapshot, Object afterSnapshot,
                                                  @Nullable HttpServletRequest request) {
        RecordAdminLogCommand command = new RecordAdminLogCommand();
        command.setAdminUserId(AuthContext.getRequiredUserId());
        command.setAdminUsername(AuthContext.getUsername());
        command.setOperation(operation);
        command.setTargetType(targetType);
        command.setTargetId(targetId);
        command.setDetail(detail);
        command.setRequestMethod(request == null ? null : request.getMethod());
        command.setRequestUri(request == null ? null : request.getRequestURI());
        command.setIpAddress(resolveIpAddress(request));
        command.setUserAgent(request == null ? null : request.getHeader("User-Agent"));
        command.setResultStatus("SUCCESS");
        command.setBeforeSnapshot(beforeSnapshot);
        command.setAfterSnapshot(afterSnapshot);
        return command;
    }

    /**
     * 解析请求 IP。
     *
     * @param request HTTP 请求
     * @return 请求 IP
     */
    private String resolveIpAddress(@Nullable HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.trim().isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
