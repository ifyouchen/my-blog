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

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Logger log = LoggerFactory.getLogger(MessageAppService.class);

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final UserLevelAppService userLevelAppService;

    public MessageAppService(ConversationRepository conversationRepository,
                             MessageRepository messageRepository,
                             UserRepository userRepository,
                             UserLevelAppService userLevelAppService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.userLevelAppService = userLevelAppService;
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
            items.add(toMessageDTO(msg, currentUserId));
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
    public MessageDTO sendMessage(Long conversationId, String content, String type, Long parentId) {
        long _start = System.currentTimeMillis();
        Long currentUserId = AuthContext.getRequiredUserId();

        String messageType = normalizeMessageType(type);
        String messageContent = normalizeMessageContent(content, messageType);
        if (messageContent == null || messageContent.isEmpty()) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "消息内容不能为空");
        }

        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "会话不存在"));

        // 校验被回复消息
        if (parentId != null) {
            Message parentMsg = messageRepository.findById(parentId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "被回复的消息不存在"));
            if (!parentMsg.getConversationId().equals(conversationId)) {
                throw new ApplicationException(ErrorCode.PARAM_ERROR, "被回复的消息不属于当前会话");
            }
            if (parentMsg.getDeletedAt() != null || parentMsg.getSenderDeletedAt() != null || parentMsg.getReceiverDeletedAt() != null) {
                throw new ApplicationException(ErrorCode.PARAM_ERROR, "被回复的消息已被删除");
            }
        }

        Long id = messageRepository.nextId();
        Message message = Message.create(id, conversationId, currentUserId, messageContent, messageType, parentId);
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
                "content", BizLogHelper.contentMeta(messageContent), "parentId", parentId),
            BizLogHelper.result("messageId=" + id),
            BizLogHelper.elapsed(_start));

        return toMessageDTO(message, currentUserId);
    }

    /**
     * 撤回消息（仅允许发送者在10分钟内撤回）。
     */
    @Transactional(rollbackFor = Exception.class)
    public MessageDTO recallMessage(Long messageId) {
        long _start = System.currentTimeMillis();
        Long currentUserId = AuthContext.getRequiredUserId();

        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "消息不存在"));

        if (!message.getSenderId().equals(currentUserId)) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "只能撤回自己的消息");
        }

        LocalDateTime now = LocalDateTime.now();
        if (!message.canRecall(now)) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "超过10分钟无法撤回");
        }

        message.recall(now);
        int affected = messageRepository.recallMessage(messageId, currentUserId, now);
        if (affected == 0) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "消息已被撤回");
        }

        log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(currentUserId),
            "撤回消息",
            BizLogHelper.params("messageId", messageId),
            BizLogHelper.result("recalledAt=" + now.format(DTF)),
            BizLogHelper.elapsed(_start));

        return toMessageDTO(message, currentUserId);
    }

    /**
     * 规范化消息类型，无效时抛出异常。
     *
     * @param type 原始消息类型字符串
     * @return 规范化后的消息类型
     */
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

    /**
     * 规范化消息内容，图片消息校验 URL 前缀。
     *
     * @param content 原始消息内容
     * @param type    消息类型
     * @return 规范化后的消息内容
     */
    private String normalizeMessageContent(String content, String type) {
        String normalized = content == null ? "" : content.trim();
        if (MESSAGE_TYPE_IMAGE.equals(type) && normalized.isEmpty()) {
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

    /**
     * 将会话领域对象转换为 DTO，填充对方用户信息和未读数。
     *
     * @param conversation  会话领域对象
     * @param currentUserId 当前用户 ID
     * @return 会话 DTO
     */
    private ConversationDTO toConversationDTO(Conversation conversation, Long currentUserId) {
        Long otherId = conversation.getOtherParticipantId(currentUserId);
        User otherUser = userRepository.findById(new com.myblog.domain.model.valueobject.UserId(otherId)).orElse(null);

        ConversationDTO dto = new ConversationDTO();
        dto.setId(conversation.getId().getValue());
        UserDTO participant = otherUser != null ? UserAssembler.toDTO(otherUser) : null;
        if (participant != null) {
            userLevelAppService.fillLevel(participant);
        }
        dto.setParticipant(participant);
        dto.setLastMessage(conversation.getLastMessage());
        dto.setLastMessageAt(conversation.getLastMessageAt() != null
            ? conversation.getLastMessageAt().format(DTF) : null);

        long unread = messageRepository.countUnreadByConversation(
            conversation.getId().getValue(), currentUserId);
        dto.setUnreadCount(unread);

        return dto;
    }

    /**
     * 将消息领域对象转换为 DTO，填充发送者信息。
     *
     * @param message       消息领域对象
     * @param currentUserId 当前用户 ID
     * @return 消息 DTO
     */
    private MessageDTO toMessageDTO(Message message, Long currentUserId) {
        User sender = userRepository.findById(
            new com.myblog.domain.model.valueobject.UserId(message.getSenderId())).orElse(null);

        String senderName = sender != null ? (sender.getNickname() != null ? sender.getNickname() : sender.getUsername()) : null;

        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId().getValue());
        dto.setConversationId(message.getConversationId());
        dto.setSenderId(message.getSenderId());
        dto.setParentId(message.getParentId());
        dto.setSenderName(senderName);
        dto.setSenderAvatar(sender != null ? sender.getAvatarUrl() : null);
        dto.setType(message.getType());
        dto.setRead(message.isRead());
        dto.setCreatedAt(message.getCreatedAt() != null ? message.getCreatedAt().format(DTF) : null);

        // 已撤回消息处理
        if (message.isRecalled()) {
            dto.setRecalled(true);
            dto.setRecalledAt(message.getRecalledAt().format(DTF));
            dto.setContent(currentUserId.equals(message.getSenderId())
                ? "你撤回了一条消息" : (senderName != null ? senderName : "对方") + " 撤回了一条消息");
            return dto;
        }

        dto.setContent(message.getContent());

        // 被回复消息快照
        if (message.getParentId() != null) {
            MessageDTO.ReplyMessageDTO reply = new MessageDTO.ReplyMessageDTO();
            java.util.Optional<Message> parentOpt = messageRepository.findById(message.getParentId());
            if (parentOpt.isPresent()) {
                Message parent = parentOpt.get();
                User parentSender = userRepository.findById(
                    new com.myblog.domain.model.valueobject.UserId(parent.getSenderId())).orElse(null);
                String parentSenderName = parentSender != null
                    ? (parentSender.getNickname() != null ? parentSender.getNickname() : parentSender.getUsername())
                    : null;

                reply.setSenderName(parentSenderName);
                if (parent.isRecalled() || parent.getDeletedAt() != null
                    || parent.getSenderDeletedAt() != null || parent.getReceiverDeletedAt() != null) {
                    reply.setContent("该消息已被删除");
                    reply.setType("TEXT");
                } else {
                    if ("IMAGE".equals(parent.getType())) {
                        reply.setContent("[图片]");
                        reply.setType("IMAGE");
                    } else {
                        reply.setContent(parent.getContent());
                        reply.setType(parent.getType());
                    }
                }
            } else {
                reply.setContent("该消息已被删除");
                reply.setType("TEXT");
            }
            dto.setRepliedMessage(reply);
        }

        return dto;
    }
}
