package com.myblog.application.service;

import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.Comment;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.CommentId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.CommentRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.shared.enums.ArticleStatus;
import com.myblog.shared.enums.UserStatus;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminAppService {

    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;

    public AdminAppService(UserRepository userRepository,
                         ArticleRepository articleRepository,
                         CommentRepository commentRepository) {
        this.userRepository = userRepository;
        this.articleRepository = articleRepository;
        this.commentRepository = commentRepository;
    }

    public Map<String, Object> getStats() {
        List<User> allUsers = userRepository.findAll();
        List<Article> allArticles = articleRepository.findAll();
        List<Comment> allComments = commentRepository.findAll();

        long userCount = allUsers.stream().filter(u -> !u.isDeleted()).count();
        long normalUserCount = allUsers.stream()
            .filter(u -> !u.isDeleted() && UserStatus.NORMAL.equals(u.getStatus()))
            .count();
        long articleCount = allArticles.stream()
            .filter(a -> !ArticleStatus.DELETED.equals(a.getStatus()))
            .count();
        long publishedCount = allArticles.stream()
            .filter(a -> ArticleStatus.PUBLISHED.equals(a.getStatus()))
            .count();
        long commentCount = allComments.stream().filter(c -> !c.isDeleted()).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userCount);
        stats.put("normalUsers", normalUserCount);
        stats.put("totalArticles", articleCount);
        stats.put("publishedArticles", publishedCount);
        stats.put("totalComments", commentCount);
        return stats;
    }

    public PageResult<Map<String, Object>> getUsers(int page, int pageSize, String status) {
        List<User> allUsers = userRepository.findAll();
        List<User> filtered = allUsers.stream()
            .filter(u -> !u.isDeleted())
            .filter(u -> status == null || status.isEmpty() || status.equals(u.getStatus().name()))
            .collect(Collectors.toList());

        int fromIndex = Math.min((page - 1) * pageSize, filtered.size());
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        List<Map<String, Object>> items = new java.util.ArrayList<>();
        for (User user : filtered.subList(fromIndex, toIndex)) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId().getValue());
            map.put("username", user.getUsername());
            map.put("email", user.getEmail());
            map.put("nickname", user.getNickname());
            map.put("status", user.getStatus().name());
            map.put("role", user.getRole().name());
            map.put("createdAt", user.getCreatedAt());
            items.add(map);
        }
        return new PageResult<>(items, page, pageSize, filtered.size());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateUserStatus(Long userId, String status) {
        User user = userRepository.findById(new UserId(userId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));

        UserStatus newStatus = UserStatus.valueOf(status);
        UserStatus previousStatus = user.getStatus();
        user.updateStatus(newStatus);
        userRepository.save(user);

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId().getValue());
        result.put("username", user.getUsername());
        result.put("previousStatus", previousStatus.name());
        result.put("status", newStatus.name());
        return result;
    }

    public PageResult<Map<String, Object>> getArticles(int page, int pageSize, String status, String keyword) {
        List<Article> allArticles = articleRepository.findAll();
        List<Article> filtered = allArticles.stream()
            .filter(a -> !ArticleStatus.DELETED.equals(a.getStatus()))
            .filter(a -> status == null || status.isEmpty() || status.equals(a.getStatus().name()))
            .filter(a -> keyword == null || keyword.isEmpty()
                || a.getTitle().contains(keyword))
            .collect(Collectors.toList());

        int fromIndex = Math.min((page - 1) * pageSize, filtered.size());
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        List<Map<String, Object>> items = new java.util.ArrayList<>();
        for (Article article : filtered.subList(fromIndex, toIndex)) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", article.getId().getValue());
            map.put("title", article.getTitle());
            map.put("category", article.getCategory());
            map.put("status", article.getStatus().name());
            map.put("viewCount", article.getViewCount());
            map.put("likeCount", article.getLikeCount());
            map.put("favoriteCount", article.getFavoriteCount());
            map.put("commentCount", article.getCommentCount());
            map.put("createdAt", article.getCreatedAt());
            items.add(map);
        }
        return new PageResult<>(items, page, pageSize, filtered.size());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateArticleStatus(Long articleId, String status) {
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));

        ArticleStatus newStatus = ArticleStatus.valueOf(status);
        ArticleStatus previousStatus = article.getStatus();
        article.updateStatus(newStatus);
        articleRepository.save(article);

        Map<String, Object> result = new HashMap<>();
        result.put("id", article.getId().getValue());
        result.put("title", article.getTitle());
        result.put("previousStatus", previousStatus.name());
        result.put("status", newStatus.name());
        return result;
    }

    public PageResult<Map<String, Object>> getComments(int page, int pageSize, Long articleId) {
        List<Comment> allComments = commentRepository.findAll();
        List<Comment> filtered = allComments.stream()
            .filter(c -> !c.isDeleted())
            .filter(c -> articleId == null || c.getArticleId().getValue().equals(articleId))
            .collect(Collectors.toList());

        int fromIndex = Math.min((page - 1) * pageSize, filtered.size());
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        List<Map<String, Object>> items = new java.util.ArrayList<>();
        for (Comment comment : filtered.subList(fromIndex, toIndex)) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", comment.getId().getValue());
            map.put("articleId", comment.getArticleId().getValue());
            map.put("userId", comment.getUserId().getValue());
            map.put("content", comment.getContent());
            map.put("createdAt", comment.getCreatedAt());
            items.add(map);
        }
        return new PageResult<>(items, page, pageSize, filtered.size());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(new CommentId(commentId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "评论不存在"));

        comment.delete();
        commentRepository.save(comment);

        Map<String, Object> result = new HashMap<>();
        result.put("id", comment.getId().getValue());
        result.put("articleId", comment.getArticleId().getValue());
        result.put("userId", comment.getUserId().getValue());
        result.put("content", comment.getContent());
        result.put("deleted", true);
        return result;
    }
}
