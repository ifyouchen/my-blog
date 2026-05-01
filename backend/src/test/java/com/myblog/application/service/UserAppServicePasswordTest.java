package com.myblog.application.service;

import com.myblog.application.assembler.ArticleAssembler;
import com.myblog.application.port.EmailSender;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.UserFollowRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.domain.service.PasswordDomainService;
import com.myblog.shared.exception.ApplicationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAppServicePasswordTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private UserFollowRepository userFollowRepository;

    @Mock
    private ArticleAssembler articleAssembler;

    @Mock
    private PasswordDomainService passwordDomainService;

    @Mock
    private EmailSender emailSender;

    @Test
    void changePasswordRejectsSamePassword() {
        User user = User.create(1001L, "coder", "coder@example.com", "encoded-password");
        UserAppService service = createService();
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(user));
        when(passwordDomainService.matches("current-password", "encoded-password")).thenReturn(true);

        assertThatThrownBy(() -> service.changePassword(1001L, "current-password", "current-password"))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("新密码不能与当前密码相同");
    }

    @Test
    void forgotPasswordGeneratesTokenAndSendsResetMail() {
        User user = User.create(1001L, "coder", "coder@example.com", "encoded-password");
        UserAppService service = createService();
        when(userRepository.findByEmail("coder@example.com")).thenReturn(Optional.of(user));

        String token = service.forgotPassword("coder@example.com");

        assertThat(token).isNotBlank();
        assertThat(user.getPasswordResetToken()).isEqualTo(token);
        assertThat(user.getPasswordResetExpire()).isNotNull();
        verify(userRepository).save(user);
        verify(emailSender).sendPasswordResetLink(
            eq("coder@example.com"),
            eq("coder"),
            contains("/auth/reset-password?token=" + token)
        );
    }

    @Test
    void resetPasswordClearsTokenAfterSuccess() {
        User user = User.create(1001L, "coder", "coder@example.com", "encoded-password");
        String token = user.generatePasswordResetToken();
        UserAppService service = createService();
        when(userRepository.findByPasswordResetToken(token)).thenReturn(Optional.of(user));
        when(passwordDomainService.encode("new-password")).thenReturn("new-hash");

        service.resetPassword(token, "new-password");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("new-hash");
        assertThat(userCaptor.getValue().getPasswordResetToken()).isNull();
        assertThat(userCaptor.getValue().getPasswordResetExpire()).isNull();
    }

    @Test
    void resetPasswordRejectsMissingToken() {
        UserAppService service = createService();
        when(userRepository.findByPasswordResetToken("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword("missing", "new-password"))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("重置链接无效或已过期");
    }

    private UserAppService createService() {
        return new UserAppService(
            userRepository,
            articleRepository,
            userFollowRepository,
            articleAssembler,
            passwordDomainService,
            emailSender,
            "http://localhost:5173/"
        );
    }
}
