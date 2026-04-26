package com.myblog.application.service;

import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.AuthorRankingDTO;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.UserFollowRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * 排行榜应用服务测试。
 *
 * @author Codex
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class RankingAppServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserFollowRepository userFollowRepository;

    @InjectMocks
    private RankingAppService rankingAppService;

    /**
     * 作者榜排序和关注状态应正确。
     */
    @Test
    @DisplayName("作者榜应按阅读优先点赞次之排序并带上关注状态")
    void shouldRankAuthorsByViewsLikesAndArticleCount() {
        User authorA = createUser(1001L, "author_a");
        User authorB = createUser(1002L, "author_b");
        User authorC = createUser(1003L, "author_c");

        when(userRepository.findAll()).thenReturn(Arrays.asList(authorA, authorB, authorC));
        when(articleRepository.findPublished(null, null, null, "latest")).thenReturn(Arrays.asList(
            createArticle(1L, 1001L, 80, 12),
            createArticle(2L, 1001L, 40, 6),
            createArticle(3L, 1002L, 120, 10),
            createArticle(4L, 1003L, 120, 8)
        ));
        when(userFollowRepository.countFollowers(new UserId(1001L))).thenReturn(3);
        when(userFollowRepository.countFollowers(new UserId(1002L))).thenReturn(5);
        when(userFollowRepository.countFollowers(new UserId(1003L))).thenReturn(2);
        when(userFollowRepository.exists(new UserId(2000L), new UserId(1001L))).thenReturn(true);
        when(userFollowRepository.exists(new UserId(2000L), new UserId(1002L))).thenReturn(false);
        when(userFollowRepository.exists(new UserId(2000L), new UserId(1003L))).thenReturn(false);

        List<AuthorRankingDTO> result = rankingAppService.listAuthorRankings(3, 2000L);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getUser().getId()).isEqualTo(1001L);
        assertThat(result.get(0).getRank()).isEqualTo(1);
        assertThat(result.get(0).isFollowed()).isTrue();
        assertThat(result.get(1).getUser().getId()).isEqualTo(1002L);
        assertThat(result.get(2).getUser().getId()).isEqualTo(1003L);
    }

    /**
     * 文章榜应忽略缺失作者文章。
     */
    @Test
    @DisplayName("文章榜应跳过缺失作者的文章")
    void shouldSkipArticlesWithoutResolvedAuthor() {
        Article validArticle = createArticle(11L, 1001L, 200, 20);
        Article invalidArticle = createArticle(12L, 1009L, 180, 18);
        User author = createUser(1001L, "author_a");

        when(articleRepository.findPublished(null, null, null, "hot"))
            .thenReturn(Arrays.asList(validArticle, invalidArticle));
        when(userRepository.findById(new UserId(1001L))).thenReturn(Optional.of(author));
        when(userRepository.findById(new UserId(1009L))).thenReturn(Optional.empty());

        List<ArticleDTO> result = rankingAppService.listArticleRankings(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(11L);
        assertThat(result.get(0).getAuthor().getId()).isEqualTo(1001L);
    }

    private User createUser(Long id, String username) {
        return User.restore(
            id,
            username,
            username + "@example.com",
            "hashed-password",
            username,
            "https://example.com/avatar.png",
            "bio",
            UserRole.USER,
            UserStatus.NORMAL,
            LocalDateTime.now().minusDays(10),
            LocalDateTime.now().minusDays(1)
        );
    }

    private Article createArticle(Long id, Long authorId, int viewCount, int likeCount) {
        return Article.restore(
            id,
            new UserId(authorId),
            "title-" + id,
            "summary",
            "content",
            "https://example.com/cover.png",
            "Java",
            Collections.singletonList("Spring Boot"),
            ArticleStatus.PUBLISHED,
            viewCount,
            likeCount,
            1,
            0,
            LocalDateTime.now().minusDays(5),
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().minusDays(1),
            0
        );
    }
}
