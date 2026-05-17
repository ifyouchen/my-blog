package com.myblog.application.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.myblog.application.assembler.ArticleAssembler;
import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.ArticleRecommendationsDTO;
import com.myblog.application.query.ArticlePageQuery;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.ColumnRepository;
import com.myblog.domain.repository.ColumnSubscriptionRepository;
import com.myblog.domain.repository.UserFollowRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.shared.enums.ArticleStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationAppServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ColumnRepository columnRepository;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private ColumnSubscriptionRepository columnSubscriptionRepository;

    @Mock
    private UserFollowRepository userFollowRepository;

    @Mock
    private ArticleAssembler articleAssembler;

    @Test
    void listWeeklyArticlesExcludesFocusAndRecommendFeedHead() {
        Cache<String, List<ArticleDTO>> featuredArticlesCache = Caffeine.newBuilder().build();
        Cache<String, ArticleRecommendationsDTO> articleRecommendationsCache = Caffeine.newBuilder().build();
        RecommendationAppService service = new RecommendationAppService(
            userRepository,
            columnRepository,
            articleRepository,
            columnSubscriptionRepository,
            userFollowRepository,
            articleAssembler,
            featuredArticlesCache,
            articleRecommendationsCache
        );
        when(articleRepository.findPublishedWithLimit(
            null,
            null,
            null,
            ArticlePageQuery.SORT_RECOMMEND,
            10,
            0
        )).thenReturn(Arrays.asList(article(2001L), article(2002L)));
        when(articleRepository.findWeeklyPicks(anyList(), eq(4))).thenReturn(Collections.<Article>emptyList());

        service.listWeeklyArticles(1001L, 4);

        ArgumentCaptor<List> excludeIdsCaptor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(articleRepository).findWeeklyPicks(excludeIdsCaptor.capture(), eq(4));
        assertThat(excludeIdsCaptor.getValue()).contains(1001L, 2001L, 2002L);
    }

    private Article article(Long id) {
        return Article.create(
            id,
            new UserId(1L),
            "title " + id,
            "summary",
            "content",
            "",
            "Java",
            Collections.<String>emptyList(),
            ArticleStatus.DRAFT,
            null,
            null,
            null,
            null
        );
    }
}
