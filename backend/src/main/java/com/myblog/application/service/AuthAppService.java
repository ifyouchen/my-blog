package com.myblog.application.service;

import com.myblog.application.assembler.UserAssembler;
import com.myblog.application.command.LoginCommand;
import com.myblog.application.command.RegisterCommand;
import com.myblog.application.dto.AuthDTO;
import com.myblog.application.dto.UserDTO;
import com.myblog.growth.application.service.InviteRewardAppService;
import com.myblog.growth.application.service.PointAppService;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.UserRepository;
import com.myblog.domain.service.PasswordDomainService;
import com.myblog.infrastructure.security.JwtTokenProvider;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.util.BizLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

/**
 * 认证应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class AuthAppService {

    private static final Logger log = LoggerFactory.getLogger(AuthAppService.class);

    private final UserRepository userRepository;
    private final PasswordDomainService passwordDomainService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RegisterEmailCodeAppService registerEmailCodeAppService;
    private final PointAppService pointAppService;
    private final UserLevelAppService userLevelAppService;
    private InviteCodeAppService inviteCodeAppService;
    private InviteRewardAppService inviteRewardAppService;

    @Autowired(required = false)
    public void setInviteCodeAppService(InviteCodeAppService inviteCodeAppService) {
        this.inviteCodeAppService = inviteCodeAppService;
    }

    @Autowired(required = false)
    public void setInviteRewardAppService(InviteRewardAppService inviteRewardAppService) {
        this.inviteRewardAppService = inviteRewardAppService;
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
                          JwtTokenProvider jwtTokenProvider,
                          RegisterEmailCodeAppService registerEmailCodeAppService,
                          PointAppService pointAppService,
                          UserLevelAppService userLevelAppService) {
        this.userRepository = userRepository;
        this.passwordDomainService = passwordDomainService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.registerEmailCodeAppService = registerEmailCodeAppService;
        this.pointAppService = pointAppService;
        this.userLevelAppService = userLevelAppService;
    }

    /**
     * 发送注册邮箱验证码。
     *
     * @param email 邮箱
     */
    public void sendRegisterEmailCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ApplicationException(ErrorCode.CONFLICT, "邮箱已存在");
        }
        registerEmailCodeAppService.sendCode(normalizedEmail);
    }

    /**
     * 注册用户。
     *
     * @param command 注册命令
     * @return 认证结果
     */
    @Transactional(rollbackFor = Exception.class)
    public AuthDTO register(RegisterCommand command) {
        long _start = System.currentTimeMillis();
        String normalizedEmail = normalizeEmail(command.getEmail());
        if (userRepository.existsByUsername(command.getUsername())) {
            throw new ApplicationException(ErrorCode.CONFLICT, "用户名已存在");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ApplicationException(ErrorCode.CONFLICT, "邮箱已存在");
        }
        registerEmailCodeAppService.verifyAndConsume(normalizedEmail, command.getEmailCode());
        String passwordHash = passwordDomainService.encode(command.getPassword());
        User user = User.create(userRepository.nextId(), command.getUsername(), normalizedEmail, passwordHash);
        userRepository.save(user);
        if (inviteCodeAppService != null && command.getInviteCode() != null && !command.getInviteCode().isEmpty()) {
            Long inviterUserId = inviteCodeAppService.useCode(command.getInviteCode(), user.getId().getValue());
            if (inviterUserId != null && inviteRewardAppService != null) {
                inviteRewardAppService.triggerReward(inviterUserId, user.getId().getValue());
            }
        }
        pointAppService.addPoints(
            user.getId().getValue(),
            10,
            "REGISTER_BONUS",
            "REGISTER:" + user.getId().getValue(),
            "注册奖励",
            "SYSTEM"
        );
        String token = jwtTokenProvider.createToken(user.getId().getValue(), user.getUsername(), user.getRole().name());
        UserDTO userDTO = UserAssembler.toDTO(user);
        userLevelAppService.fillLevel(userDTO);
        AuthDTO result = new AuthDTO(token, userDTO);
        log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(user.getId().getValue(), user.getNickname()),
            "注册账号",
            BizLogHelper.params("username", command.getUsername(), "email", command.getEmail()),
            BizLogHelper.created("userId", user.getId().getValue()),
            BizLogHelper.elapsed(_start));
        return result;
    }

    /**
     * 用户登录。
     *
     * @param command 登录命令
     * @return 认证结果
     */
    @Transactional(rollbackFor = Exception.class)
    public AuthDTO login(LoginCommand command) {
        long _start = System.currentTimeMillis();
        Optional<User> optionalUser = userRepository.findByAccount(command.getAccount());
        if (!optionalUser.isPresent()) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }
        User user = optionalUser.get();
        user.ensureCanLogin();
        if (!passwordDomainService.matches(command.getPassword(), user.getPasswordHash())) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }
        user.recordLogin(command.getLoginIp());
        userRepository.save(user);
        String token = jwtTokenProvider.createToken(user.getId().getValue(), user.getUsername(), user.getRole().name());
        UserDTO userDTO = UserAssembler.toDTO(user);
        userLevelAppService.fillLevel(userDTO);
        AuthDTO result = new AuthDTO(token, userDTO);
        log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(user.getId().getValue(), user.getNickname()),
            "登录博客",
            BizLogHelper.params("account", command.getAccount()),
            BizLogHelper.loggedIn(user.getId().getValue()),
            BizLogHelper.elapsed(_start));
        return result;
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
        UserDTO dto = UserAssembler.toDTO(user);
        userLevelAppService.fillLevel(dto);
        return dto;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "邮箱不能为空");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
