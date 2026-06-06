package com.myblog.application.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.myblog.application.assembler.ArticleAssembler;
import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.AuthorRankingDTO;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.readmodel.AuthorArticleStats;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.UserFollowRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.shared.enums.ArticleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RankingAppServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserFollowRepository userFollowRepository;

    @Mock
    private UserLevelAppService userLevelAppService;

    private Cache<String, List<ArticleDTO>> articleRankingsCache;
    private Cache<String, List<AuthorRankingDTO>> authorRankingsCache;
    private RankingAppService service;

    @BeforeEach
    void setUp() {
        articleRankingsCache = Caffeine.newBuilder().build();
        authorRankingsCache = Caffeine.newBuilder().build();
        service = new RankingAppService(
            articleRepository,
            userRepository,
            userFollowRepository,
            new ArticleAssembler(""),
            userLevelAppService,
            Runnable::run,
            articleRankingsCache,
            authorRankingsCache
        );
    }

    @Test
    void listArticleRankingsNormalizesPeriodCategoryAndLimit() {
        when(articleRepository.findRankingArticles(eq("Go"), any(LocalDateTime.class), eq(10)))
            .thenReturn(Collections.<Article>emptyList());

        List<ArticleDTO> result = service.listArticleRankings(0, "invalid", " Go ");

        verify(articleRepository).findRankingArticles(eq("Go"), ArgumentMatchers.<LocalDateTime>notNull(), eq(10));
        assertThat(result).isEmpty();
    }

    @Test
    void listAuthorRankingsIncludesFollowStatusAndTopArticle() {
        AuthorArticleStats stats = new AuthorArticleStats();
        stats.setAuthorId(2L);
        stats.setArticleCount(3);
        stats.setTotalViews(1200L);
        stats.setTotalLikes(88L);
        User author = User.create(2L, "writer", "writer@example.com", "encoded-password");
        Article topArticle = Article.restore(
            100L,
            new UserId(2L),
            "代表作标题",
            "summary",
            "content",
            "",
            "Go",
            null,
            Collections.<String>emptyList(),
            ArticleStatus.PUBLISHED,
            100,
            20,
            0,
            4,
            false,
            false,
            null,
            0,
            "top-article",
            "",
            "",
            null,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().minusDays(1),
            0
        );
        Map<Long, Integer> followerCounts = new HashMap<Long, Integer>();
        followerCounts.put(2L, 7);

        when(articleRepository.findAuthorArticleStats(eq(10), isNull(), isNull()))
            .thenReturn(Collections.singletonList(stats));
        when(userRepository.findByIds(Collections.singletonList(2L))).thenReturn(Collections.singletonList(author));
        when(userFollowRepository.countFollowersBatch(Collections.singletonList(2L))).thenReturn(followerCounts);
        when(userFollowRepository.findFollowingUserIdsIn(any(UserId.class), eq(Collections.singletonList(2L))))
            .thenReturn(Collections.singletonList(2L));
        when(articleRepository.findTopRankingArticlesByAuthorIds(
                eq(Collections.singletonList(2L)),
                isNull(),
                isNull()))
            .thenReturn(Collections.singletonList(topArticle));

        List<AuthorRankingDTO> result = service.listAuthorRankings(10, "all", "", 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isFollowed()).isTrue();
        assertThat(result.get(0).getFollowerCount()).isEqualTo(7);
        assertThat(result.get(0).getTopArticle().getId()).isEqualTo(100L);
        assertThat(result.get(0).getTopArticle().getTitle()).isEqualTo("代表作标题");
        assertThat(result.get(0).getTopArticle().getSlug()).isEqualTo("top-article");
    }

    @Test
    void refreshRankingCachesRecomputesAndReplacesCachedEntries() {
        ArticleDTO oldArticle = new ArticleDTO();
        oldArticle.setTitle("旧文章榜");
        articleRankingsCache.put("10:all:", Collections.singletonList(oldArticle));
        authorRankingsCache.put("10:all:", Collections.<AuthorRankingDTO>emptyList());

        Article freshArticle = createArticle(101L, 2L, "新文章榜", "fresh-article");
        User author = User.create(2L, "writer", "writer@example.com", "encoded-password");
        AuthorArticleStats stats = new AuthorArticleStats();
        stats.setAuthorId(2L);
        stats.setArticleCount(1);
        stats.setTotalViews(300L);
        stats.setTotalLikes(20L);
        Map<Long, Integer> followerCounts = new HashMap<Long, Integer>();
        followerCounts.put(2L, 9);

        when(articleRepository.findRankingArticles(isNull(), isNull(), eq(10)))
            .thenReturn(Collections.singletonList(freshArticle));
        when(articleRepository.findAuthorArticleStats(eq(10), isNull(), isNull()))
            .thenReturn(Collections.singletonList(stats));
        when(userRepository.findByIds(Collections.singletonList(2L))).thenReturn(Collections.singletonList(author));
        when(userFollowRepository.countFollowersBatch(Collections.singletonList(2L))).thenReturn(followerCounts);
        when(articleRepository.findTopRankingArticlesByAuthorIds(
                eq(Collections.singletonList(2L)),
                isNull(),
                isNull()))
            .thenReturn(Collections.singletonList(freshArticle));

        service.refreshRankingCaches();

        List<ArticleDTO> refreshedArticles = articleRankingsCache.getIfPresent("10:all:");
        List<AuthorRankingDTO> refreshedAuthors = authorRankingsCache.getIfPresent("10:all:");
        assertThat(refreshedArticles).hasSize(1);
        assertThat(refreshedArticles.get(0).getTitle()).isEqualTo("新文章榜");
        assertThat(refreshedAuthors).hasSize(1);
        assertThat(refreshedAuthors.get(0).getFollowerCount()).isEqualTo(9);
        assertThat(refreshedAuthors.get(0).isFollowed()).isFalse();
        assertThat(refreshedAuthors.get(0).getTopArticle().getTitle()).isEqualTo("新文章榜");
    }

    private Article createArticle(Long articleId, Long authorId, String title, String slug) {
        return Article.restore(
            articleId,
            new UserId(authorId),
            title,
            "summary",
            "content",
            "",
            "Go",
            null,
            Collections.<String>emptyList(),
            ArticleStatus.PUBLISHED,
            100,
            20,
            0,
            4,
            false,
            false,
            null,
            0,
            slug,
            "",
            "",
            null,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().minusDays(1),
            0
        );
    }
}
