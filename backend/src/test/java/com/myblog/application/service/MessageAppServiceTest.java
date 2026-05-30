package com.myblog.application.service;

import com.myblog.domain.model.aggregate.Conversation;
import com.myblog.domain.repository.ConversationRepository;
import com.myblog.domain.repository.MessageRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.infrastructure.security.JwtPayload;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.result.PageResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 私信应用服务测试。
 *
 * @author Codex
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class MessageAppServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserLevelAppService userLevelAppService;

    @Mock
    private UserPresenceAppService userPresenceAppService;

    private MessageAppService service;

    @BeforeEach
    void setUp() {
        service = new MessageAppService(
            conversationRepository,
            messageRepository,
            userRepository,
            userLevelAppService, userPresenceAppService
        );
        setCurrentUser(9L);
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void listMessagesRejectsNonParticipantConversation() {
        when(conversationRepository.findById(100L)).thenReturn(Optional.of(Conversation.create(100L, 1L, 2L)));

        assertThatThrownBy(() -> service.listMessages(100L, 1, 20))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("无权访问该会话");

        verify(messageRepository, never()).countByConversation(100L, 9L);
        verify(messageRepository, never()).findByConversation(100L, 9L, 1, 20);
    }

    @Test
    void participantCanListMessages() {
        setCurrentUser(1L);
        when(conversationRepository.findById(100L)).thenReturn(Optional.of(Conversation.create(100L, 1L, 2L)));
        when(messageRepository.findByConversation(100L, 1L, 1, 20)).thenReturn(Collections.emptyList());
        when(messageRepository.countByConversation(100L, 1L)).thenReturn(0L);

        PageResult<?> result = service.listMessages(100L, 1, 20);

        assertThat(result.getItems()).isEmpty();
        assertThat(result.getTotal()).isEqualTo(0L);
    }

    @Test
    void sendMessageRejectsNonParticipantConversationBeforeSaving() {
        when(conversationRepository.findById(100L)).thenReturn(Optional.of(Conversation.create(100L, 1L, 2L)));

        assertThatThrownBy(() -> service.sendMessage(100L, "hello", "TEXT", null))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("无权访问该会话");

        verify(messageRepository, never()).nextId();
    }

    @Test
    void markAllReadRejectsNonParticipantConversationBeforeUpdating() {
        when(conversationRepository.findById(100L)).thenReturn(Optional.of(Conversation.create(100L, 1L, 2L)));

        assertThatThrownBy(() -> service.markAllRead(100L))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("无权访问该会话");

        verify(messageRepository, never()).markAllRead(100L, 9L);
    }

    @Test
    void deleteConversationRejectsNonParticipantConversationBeforeUpdating() {
        when(conversationRepository.findById(100L)).thenReturn(Optional.of(Conversation.create(100L, 1L, 2L)));

        assertThatThrownBy(() -> service.deleteConversation(100L))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("无权访问该会话");

        verify(conversationRepository, never()).deleteByUser(100L, 9L);
    }

    private void setCurrentUser(Long userId) {
        JwtPayload payload = new JwtPayload();
        payload.setUserId(userId);
        payload.setUsername("user-" + userId);
        payload.setRole("USER");
        AuthContext.set(payload);
    }
}
