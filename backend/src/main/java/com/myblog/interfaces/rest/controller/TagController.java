package com.myblog.interfaces.rest.controller;

import com.myblog.application.dto.TagDTO;
import com.myblog.application.command.RecordAdminLogCommand;
import com.myblog.application.service.AdminLogAppService;
import com.myblog.application.service.TagAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.Result;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagAppService tagAppService;
    private final AdminLogAppService adminLogAppService;

    public TagController(TagAppService tagAppService, AdminLogAppService adminLogAppService) {
        this.tagAppService = tagAppService;
        this.adminLogAppService = adminLogAppService;
    }

    /**
     * 校验管理员权限。
     */
    private void ensureAdmin() {
        if (AuthContext.getRequiredUserId() == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        if (!"ADMIN".equals(AuthContext.getRole())) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "仅管理员可管理标签");
        }
    }

    @GetMapping
    public Result<List<TagDTO>> getTags(@RequestParam(required = false) Boolean enabled) {
        return Result.success(tagAppService.getTags(enabled));
    }

    @GetMapping("/{id}")
    public Result<TagDTO> getTag(@PathVariable Long id) {
        return Result.success(tagAppService.getTag(id));
    }

    /**
     * 创建标签。
     *
     * @param request 创建参数
     * @param httpServletRequest HTTP 请求
     * @return 标签信息
     */
    @PostMapping
    public Result<TagDTO> createTag(@RequestBody Map<String, Object> request,
                                    @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        TagDTO tagDTO = tagAppService.createTag(name, description);
        adminLogAppService.recordOperation(buildLogCommand(
            "CREATE_TAG",
            "TAG",
            tagDTO.getId(),
            "创建标签 " + tagDTO.getName(),
            null,
            toTagSnapshot(tagDTO),
            httpServletRequest
        ));
        return Result.success(tagDTO);
    }

    /**
     * 更新标签。
     *
     * @param id 标签 ID
     * @param request 更新参数
     * @param httpServletRequest HTTP 请求
     * @return 标签信息
     */
    @PutMapping("/{id}")
    public Result<TagDTO> updateTag(@PathVariable Long id, @RequestBody Map<String, Object> request,
                                    @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        TagDTO beforeTag = tagAppService.getTag(id);
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        Boolean enabled = request.get("enabled") != null ? (Boolean) request.get("enabled") : true;
        TagDTO tagDTO = tagAppService.updateTag(id, name, description, enabled);
        adminLogAppService.recordOperation(buildLogCommand(
            "UPDATE_TAG",
            "TAG",
            tagDTO.getId(),
            "更新标签 " + tagDTO.getName() + "，enabled=" + tagDTO.getEnabled(),
            toTagSnapshot(beforeTag),
            toTagSnapshot(tagDTO),
            httpServletRequest
        ));
        return Result.success(tagDTO);
    }

    /**
     * 删除标签。
     *
     * @param id 标签 ID
     * @param httpServletRequest HTTP 请求
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<Map<String, Object>> deleteTag(@PathVariable Long id,
                                                 @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        TagDTO beforeTag = tagAppService.getTag(id);
        tagAppService.deleteTag(id);
        adminLogAppService.recordOperation(buildLogCommand(
            "DELETE_TAG",
            "TAG",
            id,
            "删除标签 " + beforeTag.getName(),
            toTagSnapshot(beforeTag),
            null,
            httpServletRequest
        ));
        Map<String, Object> result = new HashMap<>();
        result.put("deleted", true);
        return Result.success(result);
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
            String[] parts = forwardedFor.split(",");
            return parts[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 构建管理员日志命令。
     *
     * @param operation 操作类型
     * @param targetType 目标类型
     * @param targetId 目标 ID
     * @param detail 操作详情
     * @param beforeSnapshot 变更前快照
     * @param afterSnapshot 变更后快照
     * @param request HTTP 请求
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
     * 转换标签快照。
     *
     * @param tagDTO 标签 DTO
     * @return 快照数据
     */
    private Map<String, Object> toTagSnapshot(TagDTO tagDTO) {
        Map<String, Object> snapshot = new LinkedHashMap<String, Object>();
        snapshot.put("id", tagDTO.getId());
        snapshot.put("name", tagDTO.getName());
        snapshot.put("description", tagDTO.getDescription());
        snapshot.put("enabled", tagDTO.getEnabled());
        return snapshot;
    }
}
