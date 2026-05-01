package com.myblog.application.service;

import com.myblog.shared.exception.ApplicationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailRequestRateLimitServiceTest {

    @Test
    void sameIpIsRejectedWithinWindow() {
        EmailRequestRateLimitService service = new EmailRequestRateLimitService(20L, true);

        service.checkAndRecord("127.0.0.1");

        assertThatThrownBy(() -> service.checkAndRecord("127.0.0.1"))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("请求过于频繁");
    }

    @Test
    void differentIpsAreTrackedSeparately() {
        EmailRequestRateLimitService service = new EmailRequestRateLimitService(20L, true);

        service.checkAndRecord("127.0.0.1");

        assertThatCode(() -> service.checkAndRecord("127.0.0.2")).doesNotThrowAnyException();
    }
}
