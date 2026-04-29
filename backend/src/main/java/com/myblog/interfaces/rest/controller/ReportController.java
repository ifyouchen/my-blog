package com.myblog.interfaces.rest.controller;

import com.myblog.application.command.CreateReportCommand;
import com.myblog.application.dto.ReportDTO;
import com.myblog.application.service.ReportAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.interfaces.rest.dto.request.CreateReportRequest;
import com.myblog.shared.result.Result;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 举报 REST 接口。
 *
 * @author Codex
 * @since 1.0.0
 */
@Validated
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportAppService reportAppService;

    public ReportController(ReportAppService reportAppService) {
        this.reportAppService = reportAppService;
    }

    /**
     * 创建举报。
     *
     * @param request 创建举报请求
     * @return 举报 ID
     */
    @PostMapping
    public Result<Map<String, Object>> createReport(@RequestBody @Valid CreateReportRequest request) {
        CreateReportCommand command = new CreateReportCommand();
        command.setReporterUserId(AuthContext.getRequiredUserId());
        command.setTargetType(request.getTargetType());
        command.setTargetId(request.getTargetId());
        command.setReasonType(request.getReasonType());
        command.setReasonDetail(request.getReasonDetail());
        ReportDTO report = reportAppService.createReport(command);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("id", report.getId());
        result.put("status", report.getStatus());
        return Result.success(result);
    }
}
