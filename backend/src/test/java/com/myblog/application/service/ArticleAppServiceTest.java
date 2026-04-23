package com.myblog.application.service;

import com.myblog.application.command.CreateArticleCommand;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.shared.enums.ArticleStatus;
import com.myblog.shared.enums.UserRole;
import com.myblog.shared.enums.UserStatus;
import com.myblog.shared.exception.ApplicationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 文章应用服务测试。
 *
 * @author Codex
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class ArticleAppServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ArticleAppService articleAppService;

    /**
     * 游客不能访问未发布文章详情。
     */
    @Test
    @DisplayName("游客访问草稿文章详情时应抛出未找到异常")
    void shouldRejectGuestWhenGettingDraftArticleDetail() {
        Article article = buildArticle(101L, 1001L, ArticleStatus.DRAFT);
        when(articleRepository.findById(new ArticleId(101L))).thenReturn(Optional.of(article));

        assertThatThrownBy(() -> articleAppService.getArticleDetail(101L, null, null))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("文章不存在");

        verify(articleRepository, never()).save(article);
    }

    /**
     * 作者可以访问自己的未发布文章。
     */
    @Test
    @DisplayName("作者可以查看自己的草稿文章")
    void shouldAllowAuthorToGetOwnDraftArticle() {
        Article article = buildArticle(102L, 1001L, ArticleStatus.DRAFT);
        User author = buildUser(1001L, UserRole.USER);
        when(articleRepository.findById(new ArticleId(102L))).thenReturn(Optional.of(article));
        when(userRepository.findById(new UserId(1001L))).thenReturn(Optional.of(author));

        assertThat(articleAppService.getArticleDetail(102L, 1001L, UserRole.USER.name()).getId()).isEqualTo(102L);
        verify(articleRepository, never()).save(article);
    }

    /**
     * 非作者且非管理员不能编辑文章。
     */
    @Test
    @DisplayName("普通用户不能编辑他人的文章")
    void shouldRejectUpdateArticleWhenUserIsNotOwner() {
        Article article = buildArticle(103L, 1001L, ArticleStatus.PUBLISHED);
        when(articleRepository.findById(new ArticleId(103L))).thenReturn(Optional.of(article));

        CreateArticleCommand command = new CreateArticleCommand();
        command.setTitle("新标题");
        command.setSummary("摘要");
        command.setContent("正文");
        command.setCategory("后端");
        command.setTags(Arrays.asList("Java"));
        command.setStatus(ArticleStatus.PUBLISHED.name());

        assertThatThrownBy(() -> articleAppService.updateArticle(103L, command, 2002L, UserRole.USER.name()))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("无权操作这篇文章");
    }

    /**
     * 已发布文章详情会增加阅读量。
     */
    @Test
    @DisplayName("查看已发布文章详情时应增加阅读量")
    void shouldIncreaseViewCountWhenGettingPublishedArticleDetail() {
        Article article = buildArticle(104L, 1001L, ArticleStatus.PUBLISHED);
        User author = buildUser(1001L, UserRole.USER);
        when(articleRepository.findById(new ArticleId(104L))).thenReturn(Optional.of(article));
        when(userRepository.findById(new UserId(1001L))).thenReturn(Optional.of(author));

        articleAppService.getArticleDetail(104L, null, null);

        assertThat(article.getViewCount()).isEqualTo(1);
        verify(articleRepository).save(article);
    }

    private Article buildArticle(Long articleId, Long authorId, ArticleStatus status) {
        return Article.restore(
            articleId,
            new UserId(authorId),
            "测试文章",
            "摘要",
            "正文",
            "https://example.com/cover.png",
            "后端",
            Arrays.asList("Java"),
            status,
            0,
            0,
            0,
            0,
            status == ArticleStatus.PUBLISHED ? LocalDateTime.now() : null,
            LocalDateTime.now(),
            LocalDateTime.now(),
            0
        );
    }

    private User buildUser(Long userId, UserRole role) {
        return User.restore(
            userId,
            "tester",
            "tester@example.com",
            "hash",
            "tester",
            "https://example.com/avatar.png",
            "bio",
            role,
            UserStatus.NORMAL,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }
}
