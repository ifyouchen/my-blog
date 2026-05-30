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
import com.myblog.domain.repository.NotificationRepository;
import com.myblog.domain.repository.UserRepository;
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

    /**
     * 分页查询指定用户的通知列表。
     *
     * @param receiverUserId 接收用户 ID
     * @param page           页码
     * @param pageSize       每页数量
     * @param filter         类型筛选，可为 null
     * @return 通知分页结果
     */
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

    /**
     * 获取指定用户未读通知数量，使用缓存加速。
     *
     * @param receiverUserId 接收用户 ID
     * @return 未读通知数量
     */
    public long countUnread(Long receiverUserId) {
        Long cached = notificationUnreadCountCache.getIfPresent(receiverUserId);
        if (cached != null) {
            return cached;
        }
        long count = notificationRepository.countUnreadByReceiver(receiverUserId);
        notificationUnreadCountCache.put(receiverUserId, count);
        return count;
    }

    /**
     * 查询指定用户的最近通知列表，使用缓存加速。
     *
     * @param receiverUserId 接收用户 ID
     * @param filter         类型筛选，可为 null
     * @param limit          返回条数限制
     * @return 通知 DTO 列表
     */
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

    /**
     * 标记指定通知为已读。
     *
     * @param receiverUserId 接收用户 ID
     * @param notificationId 通知 ID
     */
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

    /**
     * 标记指定用户的全部通知为已读。
     *
     * @param receiverUserId 接收用户 ID
     */
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

    /**
     * 根据命令创建通知并失效相关缓存。
     *
     * @param command 创建通知命令
     */
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

    /**
     * 失效指定用户的通知相关缓存（未读计数缓存和最近通知缓存）。
     *
     * @param receiverUserId 接收用户 ID
     */
    private void invalidateNotificationCaches(Long receiverUserId) {
        notificationUnreadCountCache.asMap().remove(receiverUserId);
        // Invalidate all recent notification caches for this user
        recentNotificationsCache.asMap().keySet().removeIf(key -> key.startsWith(receiverUserId + ":"));
    }

    /**
     * 将通知领域对象转换为 DTO，包含操作者信息、Payload 解析和目标 URL。
     *
     * @param notification 通知领域对象
     * @param actorMap     操作者用户映射，用于避免 N+1 查询
     * @return 通知 DTO
     */
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

    /**
     * 根据通知类型生成跳转目标 URL。
     *
     * @param notification 通知领域对象
     * @return 目标 URL，未匹配类型返回 "#"
     */
    private String generateTargetUrl(Notification notification) {
        switch (notification.getType()) {
            case ARTICLE_LIKE:
            case ARTICLE_FAVORITE:
            case ARTICLE_PUBLISH:
                return "/articles/" + notification.getArticleId();
            case ARTICLE_COMMENT:
                return "/articles/" + notification.getArticleId()
                    + "?scrollTo=comments&commentId=" + notification.getCommentId()
                    + "#comments";
            case COMMENT_REPLY:
            case COMMENT_LIKE:
                return "/articles/" + notification.getArticleId()
                    + "?scrollTo=comments&commentId=" + notification.getCommentId()
                    + "#comments";
            case USER_FOLLOW:
                return "/users/" + notification.getActorUserId().getValue();
            case REPORT_SUBMITTED:
                return "/admin/reports?status=PENDING";
            case RECOMMENDATION_APPLIED:
                return "/admin/growth?tab=recommendation-applications&status=PENDING";
            default:
                return "#";
        }
    }
}
