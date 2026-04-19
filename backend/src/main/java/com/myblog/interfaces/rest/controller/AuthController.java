package com.myblog.interfaces.rest.controller;

import com.myblog.application.dto.AuthDTO;
import com.myblog.application.service.AuthAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.interfaces.rest.dto.request.LoginRequest;
import com.myblog.interfaces.rest.dto.request.RegisterRequest;
import com.myblog.interfaces.rest.dto.response.AuthResponse;
import com.myblog.interfaces.rest.dto.response.UserResponse;
import com.myblog.interfaces.rest.mapper.RestDtoMapper;
import com.myblog.shared.result.Result;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 认证 REST 接口。
 *
 * @author Codex
 * @since 1.0.0
 */
@Validated
@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthAppService authAppService;
    private final RestDtoMapper restDtoMapper;

    /**
     * 创建认证接口。
     *
     * @param authAppService 认证应用服务
     * @param restDtoMapper REST DTO 转换器
     */
    public AuthController(AuthAppService authAppService, RestDtoMapper restDtoMapper) {
        this.authAppService = authAppService;
        this.restDtoMapper = restDtoMapper;
    }

    /**
     * 用户注册。
     *
     * @param request 注册请求
     * @return 认证响应
     */
    @PostMapping("/auth/register")
    public Result<AuthResponse> register(@RequestBody @Valid RegisterRequest request) {
        AuthDTO authDTO = authAppService.register(restDtoMapper.toCommand(request));
        return Result.success(restDtoMapper.toResponse(authDTO));
    }

    /**
     * 用户登录。
     *
     * @param request 登录请求
     * @return 认证响应
     */
    @PostMapping("/auth/login")
    public Result<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        AuthDTO authDTO = authAppService.login(restDtoMapper.toCommand(request));
        return Result.success(restDtoMapper.toResponse(authDTO));
    }

    /**
     * 用户退出登录。
     *
     * @return 成功响应
     */
    @PostMapping("/auth/logout")
    public Result<Void> logout() {
        return Result.success();
    }

    /**
     * 获取当前登录用户。
     *
     * @return 当前用户响应
     */
    @GetMapping("/users/me")
    public Result<UserResponse> me() {
        return Result.success(restDtoMapper.toResponse(authAppService.getUser(AuthContext.getRequiredUserId())));
    }
}
