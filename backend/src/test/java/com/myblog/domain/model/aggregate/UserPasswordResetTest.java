package com.myblog.domain.model.aggregate;

import com.myblog.shared.enums.UserRole;
import com.myblog.shared.enums.UserStatus;
import com.myblog.shared.exception.DomainException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserPasswordResetTest {

    @Test
    void generatingNewTokenInvalidatesOldToken() {
        User user = User.create(1001L, "coder", "coder@example.com", "encoded-password");
        String oldToken = user.generatePasswordResetToken();
        String newToken = user.generatePasswordResetToken();

        assertThat(newToken).isNotEqualTo(oldToken);
        assertThatThrownBy(() -> user.validatePasswordResetToken(oldToken))
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("重置链接无效或已过期");
        user.validatePasswordResetToken(newToken);
    }

    @Test
    void expiredTokenIsRejected() {
        User user = User.restore(
            1001L,
            "coder",
            "coder@example.com",
            "encoded-password",
            "coder",
            null,
            "",
            null,
            null,
            null,
            null,
            null,
            "expired-token",
            LocalDateTime.now().minusMinutes(1),
            null,
            null,
            null,
            UserRole.USER,
            UserStatus.NORMAL,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().minusDays(1)
        );

        assertThatThrownBy(() -> user.validatePasswordResetToken("expired-token"))
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("重置链接已过期");
    }

    @Test
    void wrongTokenIsRejected() {
        User user = User.create(1001L, "coder", "coder@example.com", "encoded-password");
        user.generatePasswordResetToken();

        assertThatThrownBy(() -> user.validatePasswordResetToken("wrong-token"))
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("重置链接无效或已过期");
    }
}
