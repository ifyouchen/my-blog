package com.myblog.application.service;

import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.Comment;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.CommentRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.shared.enums.ArticleStatus;
import com.myblog.shared.enums.UserRole;
import com.myblog.shared.enums.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * 管理后台应用服务测试。
 *
 * @author Codex
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class AdminAppServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private AdminAppService adminAppService;

    /**
     * 用户列表应支持状态和关键字筛选。
     */
    @Test
    @DisplayName("用户列表应支持状态和关键字筛选")
    void shouldFilterUsersByStatusAndKeyword() {
        User demo = createUser(1001L, "demo", "演示用户", UserStatus.NORMAL, LocalDateTime.now().minusDays(1));
        when(userRepository.findAdminPage("NORMAL", "演示", 1, 10)).thenReturn(Collections.singletonList(demo));
        when(userRepository.countAdminPage("NORMAL", "演示")).thenReturn(1L);

        assertThat(adminAppService.getUsers(1, 10, "NORMAL", "演示").getItems())
            .hasSize(1)
            .extracting(item -> item.get("username"))
            .containsExactly("demo");
    }

    /**
     * 评论列表应支持评论内容和文章标题关键字搜索。
     */
    @Test
    @DisplayName("评论列表应支持评论内容和文章标题筛选")
    void shouldFilterCommentsByKeyword() {
        Comment comment = Comment.restore(
            7001L,
            new ArticleId(501L),
            new UserId(1002L),
            7001L,
            null,
            "这篇文章讲分页很清楚",
            Comment.CommentStatus.PUBLISHED,
            2,
            false,
            null,
            LocalDateTime.now().minusHours(5),
            LocalDateTime.now().minusHours(5)
        );
        Article article = createArticle(501L, 1001L, "Spring Boot 分页实战", ArticleStatus.PUBLISHED);
        User user = createUser(1002L, "lin", "林", UserStatus.NORMAL, LocalDateTime.now().minusDays(1));

        when(commentRepository.findAdminPage(null, "分页", 1, 10)).thenReturn(Collections.singletonList(comment));
        when(commentRepository.countAdminPage(null, "分页")).thenReturn(1L);
        when(articleRepository.findByIds(Collections.singletonList(501L))).thenReturn(Collections.singletonList(article));
        when(userRepository.findByIds(Collections.singletonList(1002L))).thenReturn(Collections.singletonList(user));

        assertThat(adminAppService.getComments(1, 10, null, "分页").getItems())
            .hasSize(1)
            .extracting(item -> item.get("articleTitle"))
            .containsExactly("Spring Boot 分页实战");
    }

    /**
     * 概览统计应返回新增和待处理指标。
     */
    @Test
    @DisplayName("统计面板应返回新增和待处理指标")
    void shouldBuildOverviewStats() {
        LocalDateTime now = LocalDateTime.now();
        when(userRepository.findAll()).thenReturn(Arrays.asList(
            createUser(1001L, "demo", "演示用户", UserStatus.NORMAL, now.minusHours(2)),
            createUser(1002L, "may", "产品同学", UserStatus.DISABLED, now.minusDays(10))
        ));
        when(articleRepository.findAll()).thenReturn(Arrays.asList(
            createArticle(501L, 1001L, "A", ArticleStatus.PUBLISHED, now.minusHours(3)),
            createArticle(502L, 1001L, "B", ArticleStatus.OFFLINE, now.minusDays(3))
        ));
        when(commentRepository.findAll()).thenReturn(Collections.singletonList(
            Comment.restore(
                7001L,
                new ArticleId(501L),
                new UserId(1002L),
                7001L,
                null,
                "评论",
                Comment.CommentStatus.PUBLISHED,
                0,
                false,
                null,
                now.minusHours(1),
                now.minusHours(1)
            )
        ));

        Map<String, Object> stats = adminAppService.getStats();

        assertThat(stats.get("disabledUsers")).isEqualTo(1L);
        assertThat(stats.get("offlineArticles")).isEqualTo(1L);
        assertThat(stats.get("todayNewUsers")).isEqualTo(1L);
        assertThat(stats.get("todayNewComments")).isEqualTo(1L);
        assertThat(stats.get("sevenDayArticles")).isEqualTo(2L);
    }

    private User createUser(Long id, String username, String nickname, UserStatus status, LocalDateTime createdAt) {
        return User.restore(
            id,
            username,
            username + "@example.com",
            "hashed-password",
            nickname,
            "https://example.com/avatar.png",
            "bio",
            UserRole.USER,
            status,
            createdAt,
            createdAt
        );
    }

    private Article createArticle(Long id, Long authorId, String title, ArticleStatus status) {
        return createArticle(id, authorId, title, status, LocalDateTime.now().minusDays(1));
    }

    private Article createArticle(Long id, Long authorId, String title, ArticleStatus status, LocalDateTime createdAt) {
        return Article.restore(
            id,
            new UserId(authorId),
            title,
            "summary",
            "content",
            "https://example.com/cover.png",
            "Java",
            Collections.singletonList("Spring Boot"),
            status,
            10,
            2,
            1,
            1,
            ArticleStatus.PUBLISHED.equals(status) ? createdAt : null,
            createdAt,
            createdAt,
            0
        );
    }
}
