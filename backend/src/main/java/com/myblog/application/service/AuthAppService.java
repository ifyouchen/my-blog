package com.myblog.application.service;

import com.myblog.application.assembler.UserAssembler;
import com.myblog.application.command.LoginCommand;
import com.myblog.application.command.RegisterCommand;
import com.myblog.application.dto.AuthDTO;
import com.myblog.application.dto.UserDTO;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.UserRepository;
import com.myblog.domain.service.PasswordDomainService;
import com.myblog.infrastructure.security.JwtTokenProvider;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 认证应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class AuthAppService {

    private final UserRepository userRepository;
    private final PasswordDomainService passwordDomainService;
    private final JwtTokenProvider jwtTokenProvider;
    private InviteCodeAppService inviteCodeAppService;

    @Autowired(required = false)
    public void setInviteCodeAppService(InviteCodeAppService inviteCodeAppService) {
        this.inviteCodeAppService = inviteCodeAppService;
    }

    /**
     * 创建认证应用服务。
     *
     * @param userRepository 用户仓储
     * @param passwordDomainService 密码领域服务
     * @param jwtTokenProvider JWT 工具
     */
    public AuthAppService(UserRepository userRepository,
                          PasswordDomainService passwordDomainService,
                          JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordDomainService = passwordDomainService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 注册用户。
     *
     * @param command 注册命令
     * @return 认证结果
     */
    @Transactional(rollbackFor = Exception.class)
    public AuthDTO register(RegisterCommand command) {
        if (userRepository.existsByUsername(command.getUsername())) {
            throw new ApplicationException(ErrorCode.CONFLICT, "用户名已存在");
        }
        if (userRepository.existsByEmail(command.getEmail())) {
            throw new ApplicationException(ErrorCode.CONFLICT, "邮箱已存在");
        }
        String passwordHash = passwordDomainService.encode(command.getPassword());
        User user = User.create(userRepository.nextId(), command.getUsername(), command.getEmail(), passwordHash);
        userRepository.save(user);
        if (inviteCodeAppService != null && command.getInviteCode() != null && !command.getInviteCode().isEmpty()) {
            inviteCodeAppService.useCode(command.getInviteCode(), user.getId().getValue());
        }
        String token = jwtTokenProvider.createToken(user.getId().getValue(), user.getUsername(), user.getRole().name());
        return new AuthDTO(token, UserAssembler.toDTO(user));
    }

    /**
     * 用户登录。
     *
     * @param command 登录命令
     * @return 认证结果
     */
    public AuthDTO login(LoginCommand command) {
        Optional<User> optionalUser = userRepository.findByAccount(command.getAccount());
        if (!optionalUser.isPresent()) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }
        User user = optionalUser.get();
        user.ensureCanLogin();
        if (!passwordDomainService.matches(command.getPassword(), user.getPasswordHash())) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }
        String token = jwtTokenProvider.createToken(user.getId().getValue(), user.getUsername(), user.getRole().name());
        return new AuthDTO(token, UserAssembler.toDTO(user));
    }

    /**
     * 根据用户 ID 获取用户。
     *
     * @param userId 用户 ID
     * @return 用户应用数据
     */
    public UserDTO getUser(Long userId) {
        User user = userRepository.findById(new UserId(userId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));
        return UserAssembler.toDTO(user);
    }
}
