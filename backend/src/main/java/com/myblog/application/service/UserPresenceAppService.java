package com.myblog.application.service;

import com.myblog.application.dto.UserDTO;
import com.myblog.application.event.UserPresenceChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Set;

/**
 * 用户在线状态应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class UserPresenceAppService {

    private static final Duration ONLINE_TTL = Duration.ofSeconds(75);
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String ONLINE_KEY_PREFIX = "presence:online:";
    private static final String LAST_SEEN_KEY_PREFIX = "presence:last-seen:";
    private static final String ONLINE_USERS_KEY = "presence:online-users";

    private final StringRedisTemplate redisTemplate;
    private final ApplicationEventPublisher eventPublisher;

    public UserPresenceAppService(StringRedisTemplate redisTemplate,
                                  ApplicationEventPublisher eventPublisher) {
        this.redisTemplate = redisTemplate;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 标记用户在线并刷新最后活跃时间。
     *
     * @param userId 用户 ID
     * @return 标记前是否已经在线
     */
    public boolean markOnline(Long userId) {
        if (userId == null) {
            return false;
        }
        boolean wasOnline = isOnline(userId);
        String now = LocalDateTime.now().format(DTF);
        redisTemplate.opsForValue().set(onlineKey(userId), now, ONLINE_TTL);
        redisTemplate.opsForValue().set(lastSeenKey(userId), now);
        redisTemplate.opsForSet().add(ONLINE_USERS_KEY, String.valueOf(userId));
        return wasOnline;
    }

    /**
     * 刷新在线 TTL。
     *
     * @param userId 用户 ID
     */
    public void refresh(Long userId) {
        markOnline(userId);
    }

    /**
     * 判断用户当前是否在线。
     *
     * @param userId 用户 ID
     * @return 是否在线
     */
    public boolean isOnline(Long userId) {
        if (userId == null) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(onlineKey(userId)));
    }

    /**
     * 获取用户最后活跃时间。
     *
     * @param userId 用户 ID
     * @return 最后活跃时间字符串
     */
    public String getLastSeenAt(Long userId) {
        if (userId == null) {
            return null;
        }
        return Optional.ofNullable(redisTemplate.opsForValue().get(lastSeenKey(userId)))
            .orElse(redisTemplate.opsForValue().get(onlineKey(userId)));
    }

    /**
     * 填充用户在线状态字段。
     *
     * @param user 用户 DTO
     */
    public void fillPresence(UserDTO user) {
        if (user == null || user.getId() == null) {
            return;
        }
        user.setOnline(isOnline(user.getId()));
        user.setLastSeenAt(getLastSeenAt(user.getId()));
    }

    /**
     * 获取在线 TTL。
     *
     * @return 在线 TTL
     */
    public Duration getOnlineTtl() {
        return ONLINE_TTL;
    }

    /**
     * 如果在线 TTL 已过期，则发布离线事件。
     *
     * @param userId 用户 ID
     * @return 是否发布离线事件
     */
    public boolean publishOfflineIfExpired(Long userId) {
        if (userId == null || isOnline(userId)) {
            return false;
        }
        Long removed = redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, String.valueOf(userId));
        if (removed != null && removed > 0) {
            eventPublisher.publishEvent(new UserPresenceChangedEvent(userId, false, getLastSeenAt(userId)));
            return true;
        }
        return false;
    }

    /**
     * 扫描已过期的在线用户并广播离线。
     */
    @Scheduled(fixedDelay = 15000, initialDelay = 15000)
    public void scanExpiredOnlineUsers() {
        Set<String> userIds = redisTemplate.opsForSet().members(ONLINE_USERS_KEY);
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        for (String userId : userIds) {
            try {
                publishOfflineIfExpired(Long.valueOf(userId));
            } catch (NumberFormatException e) {
                redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, userId);
            }
        }
    }

    private String onlineKey(Long userId) {
        return ONLINE_KEY_PREFIX + userId;
    }

    private String lastSeenKey(Long userId) {
        return LAST_SEEN_KEY_PREFIX + userId;
    }
}
