package com.myblog.application.service;

import com.myblog.application.assembler.ArticleAssembler;
import com.myblog.application.assembler.UserAssembler;
import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.UserDTO;
import com.myblog.application.event.UserFollowedEvent;
import com.myblog.application.event.UserUnfollowedEvent;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.aggregate.UserFollow;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.UserFollowRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.shared.enums.UserStatus;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.util.BizLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FollowAppService {

    private static final Logger log = LoggerFactory.getLogger(FollowAppService.class);

    private final UserFollowRepository userFollowRepository;
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final ArticleAssembler articleAssembler;
    private final ApplicationEventPublisher eventPublisher;

    public FollowAppService(UserFollowRepository userFollowRepository,
                            UserRepository userRepository,
                            ArticleRepository articleRepository,
                            ArticleAssembler articleAssembler,
                            ApplicationEventPublisher eventPublisher) {
        this.userFollowRepository = userFollowRepository;
        this.userRepository = userRepository;
        this.articleRepository = articleRepository;
        this.articleAssembler = articleAssembler;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(rollbackFor = Exception.class)
    public void followUser(Long targetUserId, Long currentUserId) {
        long _start = System.currentTimeMillis();
        validateFollowTarget(targetUserId, currentUserId);
        UserFollow existing = userFollowRepository.findByUsersIncludingDeleted(
            new UserId(currentUserId),
            new UserId(targetUserId)
        ).orElse(null);
        if (existing != null) {
            if (!existing.isDeleted()) {
                throw new ApplicationException(ErrorCode.CONFLICT, "已经关注过该作者");
            }
            existing.reactivate();
            userFollowRepository.save(existing);
            eventPublisher.publishEvent(new UserFollowedEvent(currentUserId, targetUserId));
        } else {
            userFollowRepository.save(UserFollow.create(
                userFollowRepository.nextId(),
                new UserId(currentUserId),
                new UserId(targetUserId)
            ));
            eventPublisher.publishEvent(new UserFollowedEvent(currentUserId, targetUserId));
        }
        log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(currentUserId),
            "关注用户",
            BizLogHelper.params("targetUserId", targetUserId),
            BizLogHelper.result("followed=true"),
            BizLogHelper.elapsed(_start));
    }

    @Transactional(rollbackFor = Exception.class)
    public void unfollowUser(Long targetUserId, Long currentUserId) {
        long _start = System.currentTimeMillis();
        UserFollow follow = userFollowRepository.findByUsersIncludingDeleted(
            new UserId(currentUserId),
            new UserId(targetUserId)
        ).orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "关注关系不存在"));
        if (follow.isDeleted()) {
            throw new ApplicationException(ErrorCode.CONFLICT, "已经取消关注该作者");
        }
        follow.delete();
        userFollowRepository.save(follow);
        eventPublisher.publishEvent(new UserUnfollowedEvent(currentUserId, targetUserId));
        log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(currentUserId),
            "取消关注用户",
            BizLogHelper.params("targetUserId", targetUserId),
            BizLogHelper.result("followed=false"),
            BizLogHelper.elapsed(_start));
    }

    public List<UserDTO> listFollowingUsers(Long currentUserId) {
        List<Long> followingUserIds = userFollowRepository.findFollowingUserIds(new UserId(currentUserId));
        if (followingUserIds.isEmpty()) {
            return new ArrayList<UserDTO>();
        }
        Map<Long, User> userMap = userRepository.findByIds(followingUserIds).stream()
            .collect(Collectors.toMap(user -> user.getId().getValue(), user -> user));
        List<UserDTO> items = new ArrayList<UserDTO>(followingUserIds.size());
        for (Long userId : followingUserIds) {
            User user = userMap.get(userId);
            if (user != null) {
                items.add(UserAssembler.toDTO(user));
            }
        }
        return items;
    }

    /**
     * 查询某用户的粉丝列表。
     *
     * @param targetUserId 目标用户 ID
     * @return 粉丝用户 DTO 列表
     */
    public List<UserDTO> listFollowers(Long targetUserId) {
        List<Long> followerUserIds = userFollowRepository.findFollowerUserIds(new UserId(targetUserId));
        if (followerUserIds.isEmpty()) {
            return new ArrayList<UserDTO>();
        }
        Map<Long, User> userMap = userRepository.findByIds(followerUserIds).stream()
            .collect(Collectors.toMap(user -> user.getId().getValue(), user -> user));
        List<UserDTO> items = new ArrayList<UserDTO>(followerUserIds.size());
        for (Long userId : followerUserIds) {
            User user = userMap.get(userId);
            if (user != null) {
                items.add(UserAssembler.toDTO(user));
            }
        }
        return items;
    }

    /**
     * 查询某用户的关注列表（公开可见）。
     *
     * @param targetUserId 目标用户 ID
     * @return 关注用户 DTO 列表
     */
    public List<UserDTO> listUserFollowing(Long targetUserId) {
        List<Long> followingUserIds = userFollowRepository.findFollowingUserIds(new UserId(targetUserId));
        if (followingUserIds.isEmpty()) {
            return new ArrayList<UserDTO>();
        }
        Map<Long, User> userMap = userRepository.findByIds(followingUserIds).stream()
            .collect(Collectors.toMap(user -> user.getId().getValue(), user -> user));
        List<UserDTO> items = new ArrayList<UserDTO>(followingUserIds.size());
        for (Long userId : followingUserIds) {
            User user = userMap.get(userId);
            if (user != null) {
                items.add(UserAssembler.toDTO(user));
            }
        }
        return items;
    }

    /**
     * 获取当前用户对目标用户的关注状态（含互关判断）。
     *
     * @param targetUserId 目标用户 ID
     * @param currentUserId 当前用户 ID
     * @return 关注状态 map: followed=是否关注, followedBack=对方是否回关
     */
    public Map<String, Boolean> getFollowStatus(Long targetUserId, Long currentUserId) {
        boolean followed = currentUserId != null
            && userFollowRepository.exists(new UserId(currentUserId), new UserId(targetUserId));
        boolean followedBack = currentUserId != null
            && userFollowRepository.exists(new UserId(targetUserId), new UserId(currentUserId));
        Map<String, Boolean> result = new java.util.HashMap<String, Boolean>();
        result.put("followed", followed);
        result.put("followedBack", followedBack);
        result.put("mutual", followed && followedBack);
        return result;
    }

    public PageResult<ArticleDTO> pageFollowingFeed(Long currentUserId, int page, int pageSize, String sort) {
        List<Long> followingUserIds = userFollowRepository.findFollowingUserIds(new UserId(currentUserId));
        if (followingUserIds.isEmpty()) {
            return new PageResult<ArticleDTO>(new ArrayList<ArticleDTO>(), page, pageSize, 0);
        }

        List<Article> publishedArticles = articleRepository.findPublishedByAuthorIds(followingUserIds, sort, page, pageSize);
        long total = articleRepository.countPublishedByAuthorIds(followingUserIds);
        List<Long> authorIds = publishedArticles.stream()
            .map(article -> article.getAuthorId().getValue())
            .distinct()
            .collect(Collectors.toList());
        Map<Long, User> authorMap = userRepository.findByIds(authorIds).stream()
            .collect(Collectors.toMap(user -> user.getId().getValue(), user -> user));

        List<ArticleDTO> items = new ArrayList<ArticleDTO>(publishedArticles.size());
        for (Article article : publishedArticles) {
            User author = authorMap.get(article.getAuthorId().getValue());
            if (author != null) {
                items.add(articleAssembler.toDTO(article, author));
            }
        }
        return new PageResult<ArticleDTO>(items, Math.max(page, 1), Math.max(pageSize, 1), total);
    }

    private void validateFollowTarget(Long targetUserId, Long currentUserId) {
        if (targetUserId == null || currentUserId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        if (targetUserId.equals(currentUserId)) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "不能关注自己");
        }
        User targetUser = userRepository.findById(new UserId(targetUserId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "作者不存在"));
        if (!UserStatus.NORMAL.equals(targetUser.getStatus())) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "当前作者暂时无法关注");
        }
    }
}
