package com.myblog.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.myblog.application.command.CreateNotificationCommand;
import com.myblog.application.dto.NotificationDTO;
import com.myblog.application.dto.UserDTO;
import com.myblog.domain.model.aggregate.Notification;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.NotificationRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.shared.enums.NotificationType;
import com.myblog.shared.enums.UserStatus;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.util.BizLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class NotificationAppService {

    private static final Logger log = LoggerFactory.getLogger(NotificationAppService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final Cache<Long, Long> notificationUnreadCountCache;
    private final Cache<String, List<NotificationDTO>> recentNotificationsCache;

    public NotificationAppService(NotificationRepository notificationRepository,
                                 UserRepository userRepository,
                                 ObjectMapper objectMapper,
                                 Cache<Long, Long> notificationUnreadCountCache,
                                 Cache<String, List<NotificationDTO>> recentNotificationsCache) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.notificationUnreadCountCache = notificationUnreadCountCache;
        this.recentNotificationsCache = recentNotificationsCache;
    }

    public PageResult<NotificationDTO> pageNotifications(Long receiverUserId, int page, int pageSize, String filter) {
        List<Notification> notifications = notificationRepository.findByReceiver(receiverUserId, filter, page, pageSize);
        long total = notificationRepository.countByReceiver(receiverUserId, filter);

        // Batch load actors to avoid N+1
        List<Long> actorIds = new ArrayList<>(notifications.size());
        for (Notification notification : notifications) {
            actorIds.add(notification.getActorUserId().getValue());
        }
        Map<Long, User> actorMap = new HashMap<>();
        if (!actorIds.isEmpty()) {
            List<User> actors = userRepository.findByIds(actorIds);
            for (User user : actors) {
                actorMap.put(user.getId().getValue(), user);
            }
        }

        List<NotificationDTO> dtos = new ArrayList<>(notifications.size());
        for (Notification notification : notifications) {
            dtos.add(toDTO(notification, actorMap));
        }

        return new PageResult<>(dtos, page, pageSize, total);
    }

    public long countUnread(Long receiverUserId) {
        Long cached = notificationUnreadCountCache.getIfPresent(receiverUserId);
        if (cached != null) {
            return cached;
        }
        long count = notificationRepository.countUnreadByReceiver(receiverUserId);
        notificationUnreadCountCache.put(receiverUserId, count);
        return count;
    }

    public List<NotificationDTO> listRecent(Long receiverUserId, String filter, int limit) {
        String cacheKey = receiverUserId + ":" + filter + ":" + limit;
        List<NotificationDTO> cached = recentNotificationsCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<Notification> notifications = notificationRepository.findRecentByReceiver(receiverUserId, filter, limit);

        // Batch load actors to avoid N+1
        List<Long> actorIds = new ArrayList<>(notifications.size());
        for (Notification notification : notifications) {
            actorIds.add(notification.getActorUserId().getValue());
        }
        Map<Long, User> actorMap = new HashMap<>();
        if (!actorIds.isEmpty()) {
            List<User> actors = userRepository.findByIds(actorIds);
            for (User user : actors) {
                actorMap.put(user.getId().getValue(), user);
            }
        }

        List<NotificationDTO> dtos = new ArrayList<>(notifications.size());
        for (Notification notification : notifications) {
            dtos.add(toDTO(notification, actorMap));
        }

        recentNotificationsCache.put(cacheKey, dtos);
        return dtos;
    }

    public void markRead(Long receiverUserId, Long notificationId) {
        long _start = System.currentTimeMillis();
        notificationRepository.markRead(receiverUserId, notificationId);
        invalidateNotificationCaches(receiverUserId);
        log.info("{} | {} 标记通知已读 | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(receiverUserId),
            BizLogHelper.params("notificationId", notificationId),
            BizLogHelper.result("OK"),
            BizLogHelper.elapsed(_start));
    }

    public void markAllRead(Long receiverUserId) {
        long _start = System.currentTimeMillis();
        notificationRepository.markAllRead(receiverUserId);
        invalidateNotificationCaches(receiverUserId);
        log.info("{} | {} 标记全部已读 | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(receiverUserId),
            BizLogHelper.params(),
            BizLogHelper.result("OK"),
            BizLogHelper.elapsed(_start));
    }

    public void createNotification(CreateNotificationCommand command) {
        long _start = System.currentTimeMillis();
        Long nextId = notificationRepository.nextId();
        Notification notification = Notification.create(
            nextId,
            command.getReceiverUserId(),
            command.getActorUserId(),
            command.getType(),
            command.getArticleId(),
            command.getCommentId(),
            command.getPayloadJson()
        );
        notificationRepository.save(notification);
        log.info("{} | {} 创建通知 | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(command.getActorUserId()),
            BizLogHelper.params("type", command.getType(), "receiver", command.getReceiverUserId()),
            BizLogHelper.result("notificationId=" + nextId),
            BizLogHelper.elapsed(_start));
        invalidateNotificationCaches(command.getReceiverUserId());
    }

    private void invalidateNotificationCaches(Long receiverUserId) {
        notificationUnreadCountCache.asMap().remove(receiverUserId);
        // Invalidate all recent notification caches for this user
        recentNotificationsCache.asMap().keySet().removeIf(key -> key.startsWith(receiverUserId + ":"));
    }

    private NotificationDTO toDTO(Notification notification, Map<Long, User> actorMap) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId().getValue());
        dto.setType(notification.getType().name());
        dto.setRead(notification.isRead());
        dto.setCreatedAt(notification.getCreatedAt().format(DATE_FORMATTER));
        dto.setArticleId(notification.getArticleId());
        dto.setCommentId(notification.getCommentId());

        // Use pre-loaded actor map
        User actor = actorMap.get(notification.getActorUserId().getValue());
        if (actor != null) {
            UserDTO actorDto = new UserDTO();
            actorDto.setId(actor.getId().getValue());
            actorDto.setUsername(actor.getUsername());
            actorDto.setNickname(actor.getNickname());
            actorDto.setAvatarUrl(actor.getAvatarUrl());
            dto.setActor(actorDto);
        }

        // Parse payload
        if (notification.getPayloadJson() != null && !notification.getPayloadJson().isEmpty()) {
            try {
                Map<String, Object> payload = objectMapper.readValue(
                    notification.getPayloadJson(),
                    new TypeReference<Map<String, Object>>() {}
                );
                dto.setPayload(payload);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse payload JSON for notification {}: {}",
                    notification.getId().getValue(), e.getMessage());
            }
        }

        // Generate target URL
        dto.setTargetUrl(generateTargetUrl(notification));

        return dto;
    }

    private String generateTargetUrl(Notification notification) {
        switch (notification.getType()) {
            case ARTICLE_LIKE:
            case ARTICLE_FAVORITE:
            case ARTICLE_COMMENT:
            case ARTICLE_PUBLISH:
                return "/articles/" + notification.getArticleId();
            case COMMENT_REPLY:
            case COMMENT_LIKE:
                return "/articles/" + notification.getArticleId() + "#comment-" + notification.getCommentId();
            case USER_FOLLOW:
                return "/users/" + notification.getActorUserId().getValue();
            default:
                return "#";
        }
    }
}