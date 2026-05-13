package com.myblog.interfaces.rest.controller;

import com.myblog.application.dto.AuthDTO;
import com.myblog.application.service.AuthAppService;
import com.myblog.application.service.EmailRequestRateLimitService;
import com.myblog.application.service.UserAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.interfaces.rest.dto.request.ForgotPasswordRequest;
import com.myblog.interfaces.rest.dto.request.LoginRequest;
import com.myblog.interfaces.rest.dto.request.RegisterRequest;
import com.myblog.interfaces.rest.dto.request.ResetPasswordRequest;
import com.myblog.interfaces.rest.dto.request.SendRegisterEmailCodeRequest;
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

import javax.servlet.http.HttpServletRequest;
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
    private final UserAppService userAppService;
    private final EmailRequestRateLimitService emailRequestRateLimitService;
    private final RestDtoMapper restDtoMapper;

    /**
     * 创建认证接口。
     *
     * @param authAppService 认证应用服务
     * @param userAppService 用户应用服务
     * @param restDtoMapper REST DTO 转换器
     */
    public AuthController(AuthAppService authAppService,
                          UserAppService userAppService,
                          EmailRequestRateLimitService emailRequestRateLimitService,
                          RestDtoMapper restDtoMapper) {
        this.authAppService = authAppService;
        this.userAppService = userAppService;
        this.emailRequestRateLimitService = emailRequestRateLimitService;
        this.restDtoMapper = restDtoMapper;
    }

    /**
     * 发送注册邮箱验证码。
     *
     * @param request 发送验证码请求
     * @return 成功响应
     */
    @PostMapping("/auth/register/email-code")
    public Result<Void> sendRegisterEmailCode(@RequestBody @Valid SendRegisterEmailCodeRequest request,
                                              HttpServletRequest servletRequest) {
        emailRequestRateLimitService.checkAndRecord(resolveClientIp(servletRequest));
        authAppService.sendRegisterEmailCode(request.getEmail());
        return Result.success();
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
    public Result<AuthResponse> login(@RequestBody @Valid LoginRequest request,
                                      HttpServletRequest servletRequest) {
        AuthDTO authDTO = authAppService.login(
            restDtoMapper.toCommand(request, resolveClientIp(servletRequest))
        );
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
     * 忘记密码 - 发送重置链接。
     *
     * @param request 忘记密码请求
     * @return 成功响应
     */
    @PostMapping("/auth/password/forgot")
    public Result<Void> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request,
                                       HttpServletRequest servletRequest) {
        emailRequestRateLimitService.checkAndRecord(resolveClientIp(servletRequest));
        userAppService.forgotPassword(request.getEmail());
        return Result.success();
    }

    /**
     * 重置密码。
     *
     * @param request 重置密码请求
     * @return 成功响应
     */
    @PostMapping("/auth/password/reset")
    public Result<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        userAppService.resetPassword(request.getToken(), request.getNewPassword());
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

    /**
     * 解析客户端真实 IP 地址，优先从 X-Forwarded-For 、X-Real-IP 头取。
     *
     * @param request HTTP 请求
     * @return 客户端 IP 地址字符串
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.trim().isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.trim().isEmpty()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
