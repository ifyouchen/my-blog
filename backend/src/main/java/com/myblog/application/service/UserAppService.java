package com.myblog.application.service;

import com.myblog.application.assembler.ArticleAssembler;
import com.myblog.application.assembler.UserAssembler;
import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.UserDTO;
import com.myblog.application.dto.UserProfileDTO;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.shared.enums.ArticleStatus;
import com.myblog.shared.enums.UserRole;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class UserAppService {

    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;

    /**
     * 创建用户应用服务。
     *
     * @param userRepository 用户仓储
     * @param articleRepository 文章仓储
     */
    public UserAppService(UserRepository userRepository, ArticleRepository articleRepository) {
        this.userRepository = userRepository;
        this.articleRepository = articleRepository;
    }

    /**
     * 更新个人资料。
     *
     * @param userId 用户 ID
     * @param nickname 昵称
     * @param avatarUrl 头像地址
     * @param bio 个人简介
     * @return 用户信息
     */
    @Transactional(rollbackFor = Exception.class)
    public UserDTO updateProfile(Long userId, String nickname, String avatarUrl, String bio) {
        User user = userRepository.findById(new UserId(userId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));
        user.updateProfile(nickname, avatarUrl, bio);
        userRepository.save(user);
        return UserAssembler.toDTO(user);
    }

    /**
     * 获取用户主页信息。
     *
     * @param userId 用户 ID
     * @return 用户主页信息
     */
    public UserProfileDTO getUserProfile(Long userId) {
        User user = userRepository.findById(new UserId(userId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));
        List<Article> publishedArticles = articleRepository.findPublishedByAuthorId(userId);
        UserProfileDTO profileDTO = new UserProfileDTO();
        profileDTO.setUser(UserAssembler.toDTO(user));
        profileDTO.setArticleCount(publishedArticles.size());
        profileDTO.setTotalViewCount(publishedArticles.stream().mapToLong(Article::getViewCount).sum());
        profileDTO.setTotalLikeCount(publishedArticles.stream().mapToLong(Article::getLikeCount).sum());
        return profileDTO;
    }

    /**
     * 分页查询用户已发布文章。
     *
     * @param userId 用户 ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 文章分页
     */
    public PageResult<ArticleDTO> pagePublishedArticles(Long userId, int page, int pageSize) {
        List<Article> publishedArticles = articleRepository.findPublishedByAuthorId(userId);
        return buildArticlePage(publishedArticles, page, pageSize);
    }

    /**
     * 分页查询当前用户自己的文章。
     *
     * @param userId 用户 ID
     * @param page 页码
     * @param pageSize 每页数量
     * @param status 状态筛选
     * @return 文章分页
     */
    public PageResult<ArticleDTO> pageMyArticles(Long userId, int page, int pageSize, String status) {
        List<Article> ownArticles = articleRepository.findByAuthorId(userId);
        List<Article> filtered = new ArrayList<Article>();
        for (Article article : ownArticles) {
            if (status == null || status.trim().isEmpty() || status.equals(article.getStatus().name())) {
                filtered.add(article);
            }
        }
        return buildArticlePage(filtered, page, pageSize);
    }

    private PageResult<ArticleDTO> buildArticlePage(List<Article> source, int page, int pageSize) {
        int fromIndex = Math.min((page - 1) * pageSize, source.size());
        int toIndex = Math.min(fromIndex + pageSize, source.size());
        List<ArticleDTO> items = new ArrayList<ArticleDTO>();
        for (Article article : source.subList(fromIndex, toIndex)) {
            User author = userRepository.findById(article.getAuthorId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章作者不存在"));
            items.add(ArticleAssembler.toDTO(article, author));
        }
        return new PageResult<ArticleDTO>(items, page, pageSize, source.size());
    }

    /**
     * 判断当前用户是否为管理员。
     *
     * @param role 角色字符串
     * @return 是否管理员
     */
    public boolean isAdmin(String role) {
        return UserRole.ADMIN.name().equals(role);
    }
}
