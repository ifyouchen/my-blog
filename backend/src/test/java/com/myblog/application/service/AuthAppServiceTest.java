package com.myblog.application.service;

import com.myblog.application.command.RegisterCommand;
import com.myblog.application.dto.AuthDTO;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.repository.UserRepository;
import com.myblog.domain.service.PasswordDomainService;
import com.myblog.infrastructure.security.JwtTokenProvider;
import com.myblog.shared.exception.ApplicationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthAppServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordDomainService passwordDomainService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RegisterEmailCodeAppService registerEmailCodeAppService;

    @Test
    void sendRegisterEmailCodeRejectsExistingEmail() {
        AuthAppService service = new AuthAppService(
            userRepository, passwordDomainService, jwtTokenProvider, registerEmailCodeAppService
        );
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.sendRegisterEmailCode("User@Example.com"))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("邮箱已存在");
        verifyNoInteractions(registerEmailCodeAppService);
    }

    @Test
    void registerRequiresAndConsumesEmailCodeBeforeCreatingUser() {
        AuthAppService service = new AuthAppService(
            userRepository, passwordDomainService, jwtTokenProvider, registerEmailCodeAppService
        );
        when(userRepository.existsByUsername("coder")).thenReturn(false);
        when(userRepository.existsByEmail("coder@example.com")).thenReturn(false);
        when(userRepository.nextId()).thenReturn(1001L);
        when(passwordDomainService.encode("123456")).thenReturn("encoded-password");
        when(jwtTokenProvider.createToken(1001L, "coder", "USER")).thenReturn("jwt-token");

        AuthDTO result = service.register(new RegisterCommand(
            "coder", "Coder@Example.com", "123456", null, "123456"
        ));

        verify(registerEmailCodeAppService).verifyAndConsume("coder@example.com", "123456");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail().getValue()).isEqualTo("coder@example.com");
        assertThat(result.getToken()).isEqualTo("jwt-token");
    }
}
