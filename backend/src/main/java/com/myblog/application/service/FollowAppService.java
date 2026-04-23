package com.myblog.application.service;

import com.myblog.application.assembler.ArticleAssembler;
import com.myblog.application.assembler.UserAssembler;
import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.UserDTO;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 关注应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class FollowAppService {

    private final UserFollowRepository userFollowRepository;
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;

    public FollowAppService(UserFollowRepository userFollowRepository,
                            UserRepository userRepository,
                            ArticleRepository articleRepository) {
        this.userFollowRepository = userFollowRepository;
        this.userRepository = userRepository;
        this.articleRepository = articleRepository;
    }

    /**
     * 关注用户。
     *
     * @param targetUserId 目标用户 ID
     * @param currentUserId 当前用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void followUser(Long targetUserId, Long currentUserId) {
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
            return;
        }
        userFollowRepository.save(UserFollow.create(
            userFollowRepository.nextId(),
            new UserId(currentUserId),
            new UserId(targetUserId)
        ));
    }

    /**
     * 取消关注用户。
     *
     * @param targetUserId 目标用户 ID
     * @param currentUserId 当前用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void unfollowUser(Long targetUserId, Long currentUserId) {
        UserFollow follow = userFollowRepository.findByUsersIncludingDeleted(
            new UserId(currentUserId),
            new UserId(targetUserId)
        ).orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "关注关系不存在"));
        if (follow.isDeleted()) {
            throw new ApplicationException(ErrorCode.CONFLICT, "已经取消关注该作者");
        }
        follow.delete();
        userFollowRepository.save(follow);
    }

    /**
     * 查询当前用户关注作者列表。
     *
     * @param currentUserId 当前用户 ID
     * @return 作者列表
     */
    public List<UserDTO> listFollowingUsers(Long currentUserId) {
        List<Long> followingUserIds = userFollowRepository.findFollowingUserIds(new UserId(currentUserId));
        List<UserDTO> items = new ArrayList<UserDTO>(followingUserIds.size());
        for (Long userId : followingUserIds) {
            User user = userRepository.findById(new UserId(userId)).orElse(null);
            if (user != null) {
                items.add(UserAssembler.toDTO(user));
            }
        }
        return items;
    }

    /**
     * 查询关注流。
     *
     * @param currentUserId 当前用户 ID
     * @param page 页码
     * @param pageSize 每页数量
     * @param sort 排序方式
     * @return 关注流分页
     */
    public PageResult<ArticleDTO> pageFollowingFeed(Long currentUserId, int page, int pageSize, String sort) {
        List<Long> followingUserIds = userFollowRepository.findFollowingUserIds(new UserId(currentUserId));
        if (followingUserIds.isEmpty()) {
            return new PageResult<ArticleDTO>(new ArrayList<ArticleDTO>(), page, pageSize, 0);
        }
        Set<Long> authorIdSet = new HashSet<Long>(followingUserIds);
        List<Article> publishedArticles = articleRepository.findPublished(null, null, null, sort);
        List<Article> filteredArticles = new ArrayList<Article>();
        for (Article article : publishedArticles) {
            if (authorIdSet.contains(article.getAuthorId().getValue())) {
                filteredArticles.add(article);
            }
        }
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        int fromIndex = Math.min((currentPage - 1) * currentPageSize, filteredArticles.size());
        int toIndex = Math.min(fromIndex + currentPageSize, filteredArticles.size());
        List<ArticleDTO> items = new ArrayList<ArticleDTO>();
        for (Article article : filteredArticles.subList(fromIndex, toIndex)) {
            User author = userRepository.findById(article.getAuthorId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章作者不存在"));
            items.add(ArticleAssembler.toDTO(article, author));
        }
        return new PageResult<ArticleDTO>(items, currentPage, currentPageSize, filteredArticles.size());
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
