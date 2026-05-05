package com.myblog.application.service;

import com.myblog.application.assembler.UserAssembler;
import com.myblog.application.dto.ConversationDTO;
import com.myblog.application.dto.MessageDTO;
import com.myblog.application.dto.UserDTO;
import com.myblog.domain.model.aggregate.Conversation;
import com.myblog.domain.model.aggregate.Message;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.repository.ConversationRepository;
import com.myblog.domain.repository.MessageRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.util.BizLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 私信应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class MessageAppService {

    private static final String MESSAGE_TYPE_TEXT = "TEXT";
    private static final String MESSAGE_TYPE_IMAGE = "IMAGE";
    private static final String IMAGE_MESSAGE_PREVIEW = "[图片]";
    private static final String UPLOAD_FILE_URL_PREFIX = "/api/uploads/files/";

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Logger log = LoggerFactory.getLogger(MessageAppService.class);

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public MessageAppService(ConversationRepository conversationRepository,
                             MessageRepository messageRepository,
                             UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    /**
     * 获取或创建会话。
     */
    @Transactional(rollbackFor = Exception.class)
    public ConversationDTO getOrCreateConversation(Long participantId) {
        long _start = System.currentTimeMillis();
        Long currentUserId = AuthContext.getRequiredUserId();
        if (currentUserId.equals(participantId)) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "不能和自己发起会话");
        }

        // 确保对方用户存在
        userRepository.findById(new com.myblog.domain.model.valueobject.UserId(participantId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));

        // 确定参与者排序
        long aId = Math.min(currentUserId, participantId);
        long bId = Math.max(currentUserId, participantId);

        Conversation conversation = conversationRepository.findByParticipants(aId, bId)
            .orElseGet(() -> {
                Long id = conversationRepository.nextId();
                Conversation newConv = Conversation.create(id, aId, bId);
                return conversationRepository.save(newConv);
            });

        ConversationDTO convResult = toConversationDTO(conversation, currentUserId);
        log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(currentUserId),
            "发起会话",
            BizLogHelper.params("participantId", participantId),
            BizLogHelper.result("conversationId=" + convResult.getId()),
            BizLogHelper.elapsed(_start));
        return convResult;
    }

    /**
     * 获取会话列表。
     */
    public PageResult<ConversationDTO> listConversations(int page, int pageSize) {
        Long currentUserId = AuthContext.getRequiredUserId();

        long total = conversationRepository.countByUser(currentUserId);
        List<Conversation> conversations = conversationRepository.findByUser(currentUserId, page, pageSize);
        List<ConversationDTO> items = new ArrayList<>(conversations.size());

        for (Conversation conv : conversations) {
            items.add(toConversationDTO(conv, currentUserId));
        }

        PageResult<ConversationDTO> result = new PageResult<>();
        result.setItems(items);
        result.setTotal(total);
        result.setPage(page);
        result.setPageSize(pageSize);
        return result;
    }

    /**
     * 删除会话。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteConversation(Long conversationId) {
        long _start = System.currentTimeMillis();
        Long currentUserId = AuthContext.getRequiredUserId();
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "会话不存在"));

        conversation.deleteByUser(currentUserId);
        conversationRepository.deleteByUser(conversationId, currentUserId);
        log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(currentUserId),
            "删除会话",
            BizLogHelper.params("conversationId", conversationId),
            BizLogHelper.result("deleted=true"),
            BizLogHelper.elapsed(_start));
    }

    /**
     * 获取消息历史（按时间倒序，前端反转展示）。
     */
    public PageResult<MessageDTO> listMessages(Long conversationId, int page, int pageSize) {
        Long currentUserId = AuthContext.getRequiredUserId();

        // 验证会话存在且用户是参与者
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "会话不存在"));

        long total = messageRepository.countByConversation(conversationId, currentUserId);
        List<Message> messages = messageRepository.findByConversation(conversationId, currentUserId, page, pageSize);
        List<MessageDTO> items = new ArrayList<>(messages.size());

        for (Message msg : messages) {
            items.add(toMessageDTO(msg));
        }

        PageResult<MessageDTO> result = new PageResult<>();
        result.setItems(items);
        result.setTotal(total);
        result.setPage(page);
        result.setPageSize(pageSize);
        return result;
    }

    /**
     * 发送消息。
     */
    @Transactional(rollbackFor = Exception.class)
    public MessageDTO sendMessage(Long conversationId, String content, String type) {
        long _start = System.currentTimeMillis();
        Long currentUserId = AuthContext.getRequiredUserId();

        String messageType = normalizeMessageType(type);
        String messageContent = normalizeMessageContent(content, messageType);
        if (messageContent == null || messageContent.isEmpty()) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "消息内容不能为空");
        }

        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "会话不存在"));

        Long id = messageRepository.nextId();
        Message message = Message.create(id, conversationId, currentUserId, messageContent, messageType);
        message = messageRepository.save(message);

        // 更新会话的最后消息
        String conversationPreview = MESSAGE_TYPE_IMAGE.equals(messageType) ? IMAGE_MESSAGE_PREVIEW : messageContent;
        conversation.updateLastMessage(conversationPreview);
        conversationRepository.updateLastMessage(
            conversationId, conversation.getLastMessage(), conversation.getLastMessageAt());

        log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(currentUserId),
            "发送消息",
            BizLogHelper.params("conversationId", conversationId, "type", messageType,
                "content", BizLogHelper.contentMeta(messageContent)),
            BizLogHelper.result("messageId=" + id),
            BizLogHelper.elapsed(_start));

        return toMessageDTO(message);
    }

    private String normalizeMessageType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return MESSAGE_TYPE_TEXT;
        }
        String normalized = type.trim().toUpperCase();
        if (MESSAGE_TYPE_TEXT.equals(normalized) || MESSAGE_TYPE_IMAGE.equals(normalized)) {
            return normalized;
        }
        throw new ApplicationException(ErrorCode.PARAM_ERROR, "不支持的消息类型");
    }

    private String normalizeMessageContent(String content, String type) {
        String normalized = content == null ? "" : content.trim();
        if (MESSAGE_TYPE_IMAGE.equals(type) && !normalized.startsWith(UPLOAD_FILE_URL_PREFIX)) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "图片消息地址无效");
        }
        return normalized;
    }

    /**
     * 标记会话内所有消息为已读。
     */
    @Transactional(rollbackFor = Exception.class)
    public int markAllRead(Long conversationId) {
        long _start = System.currentTimeMillis();
        Long currentUserId = AuthContext.getRequiredUserId();
        int updated = messageRepository.markAllRead(conversationId, currentUserId);
        log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(currentUserId),
            "标记已读",
            BizLogHelper.params("conversationId", conversationId),
            BizLogHelper.result("count=" + updated),
            BizLogHelper.elapsed(_start));
        return updated;
    }

    /**
     * 获取未读消息总数。
     */
    public long countUnread() {
        Long currentUserId = AuthContext.getCurrentUserId();
        if (currentUserId == null) {
            return 0L;
        }
        return countUnreadForUser(currentUserId);
    }

    /**
     * 获取指定用户的未读消息总数。
     */
    public long countUnreadForUser(Long userId) {
        if (userId == null) {
            return 0L;
        }
        return messageRepository.countTotalUnread(userId);
    }

    private ConversationDTO toConversationDTO(Conversation conversation, Long currentUserId) {
        Long otherId = conversation.getOtherParticipantId(currentUserId);
        User otherUser = userRepository.findById(new com.myblog.domain.model.valueobject.UserId(otherId)).orElse(null);

        ConversationDTO dto = new ConversationDTO();
        dto.setId(conversation.getId().getValue());
        dto.setParticipant(otherUser != null ? UserAssembler.toDTO(otherUser) : null);
        dto.setLastMessage(conversation.getLastMessage());
        dto.setLastMessageAt(conversation.getLastMessageAt() != null
            ? conversation.getLastMessageAt().format(DTF) : null);

        long unread = messageRepository.countUnreadByConversation(
            conversation.getId().getValue(), currentUserId);
        dto.setUnreadCount(unread);

        return dto;
    }

    private MessageDTO toMessageDTO(Message message) {
        User sender = userRepository.findById(
            new com.myblog.domain.model.valueobject.UserId(message.getSenderId())).orElse(null);

        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId().getValue());
        dto.setConversationId(message.getConversationId());
        dto.setSenderId(message.getSenderId());
        dto.setSenderName(sender != null ? (sender.getNickname() != null ? sender.getNickname() : sender.getUsername()) : null);
        dto.setSenderAvatar(sender != null ? sender.getAvatarUrl() : null);
        dto.setContent(message.getContent());
        dto.setType(message.getType());
        dto.setRead(message.isRead());
        dto.setCreatedAt(message.getCreatedAt() != null ? message.getCreatedAt().format(DTF) : null);
        return dto;
    }
}
