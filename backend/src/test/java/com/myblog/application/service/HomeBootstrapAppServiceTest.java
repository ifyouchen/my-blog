package com.myblog.application.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.AuthorRankingDTO;
import com.myblog.application.dto.CategoryDTO;
import com.myblog.application.dto.ColumnDTO;
import com.myblog.application.dto.HomeBootstrapDTO;
import com.myblog.application.dto.TagDTO;
import com.myblog.application.dto.TopicDTO;
import com.myblog.application.service.HomeStatsAppService.HomeStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeBootstrapAppServiceTest {

    @Mock
    private HomeStatsAppService homeStatsAppService;

    @Mock
    private CategoryAppService categoryAppService;

    @Mock
    private ColumnAppService columnAppService;

    @Mock
    private RankingAppService rankingAppService;

    @Mock
    private RecommendationAppService recommendationAppService;

    @Mock
    private TopicAppService topicAppService;

    @Mock
    private TagAppService tagAppService;

    @Mock
    private SearchHistoryAppService searchHistoryAppService;

    private HomeBootstrapAppService service;

    @BeforeEach
    void setUp() {
        Cache<String, HomeBootstrapDTO> homeBootstrapCache = Caffeine.newBuilder().build();
        service = new HomeBootstrapAppService(
            homeBootstrapCache,
            homeStatsAppService,
            categoryAppService,
            columnAppService,
            rankingAppService,
            recommendationAppService,
            topicAppService,
            tagAppService,
            searchHistoryAppService,
            Runnable::run
        );
    }

    @Test
    void getBootstrapCachesLoadedDataByKey() {
        HomeStats stats = new HomeStats(1, 2, 3);
        List<CategoryDTO> categories = Collections.singletonList(new CategoryDTO());
        List<ColumnDTO> columns = Collections.singletonList(new ColumnDTO());
        List<AuthorRankingDTO> rankings = Collections.singletonList(new AuthorRankingDTO());
        ArticleDTO focusArticle = new ArticleDTO();
        focusArticle.setId(1L);
        List<ArticleDTO> featuredArticles = Collections.singletonList(focusArticle);
        List<ArticleDTO> weeklyArticles = Collections.singletonList(new ArticleDTO());
        List<TopicDTO> hotTopics = Collections.singletonList(new TopicDTO());
        List<TagDTO> hotTags = Collections.singletonList(new TagDTO());
        List<String> hotKeywords = Collections.singletonList("Java");

        when(homeStatsAppService.getStats()).thenReturn(stats);
        when(categoryAppService.getCategories(true)).thenReturn(categories);
        when(columnAppService.listRecommendedColumns(3, null)).thenReturn(columns);
        when(rankingAppService.listAuthorRankings(3, null)).thenReturn(rankings);
        when(recommendationAppService.listFeaturedArticles(1, 5)).thenReturn(featuredArticles);
        when(recommendationAppService.listWeeklyArticles(1L, 4)).thenReturn(weeklyArticles);
        when(topicAppService.listHotTopicsByActivity(7, 6)).thenReturn(hotTopics);
        when(topicAppService.listHotTopics(6)).thenReturn(hotTopics);
        when(tagAppService.getHotTags(12)).thenReturn(hotTags);
        when(searchHistoryAppService.getHotKeywords(10)).thenReturn(hotKeywords);

        HomeBootstrapDTO first = service.getBootstrap();
        HomeBootstrapDTO second = service.getBootstrap();

        assertThat(second).isSameAs(first);
        assertThat(first.getStats()).isSameAs(stats);
        assertThat(first.getCategories()).isSameAs(categories);
        assertThat(first.getRecommendedColumns()).isSameAs(columns);
        assertThat(first.getAuthorRankings()).isSameAs(rankings);
        assertThat(first.getFeaturedArticles()).isSameAs(featuredArticles);
        assertThat(first.getTodayFocus()).isSameAs(featuredArticles.get(0));
        assertThat(first.getWeeklyArticles()).isSameAs(weeklyArticles);
        assertThat(first.getHotTopics()).isSameAs(hotTopics);
        assertThat(first.getLearningTopics()).isSameAs(hotTopics);
        assertThat(first.getHotTags()).isSameAs(hotTags);
        assertThat(first.getHotKeywords()).isSameAs(hotKeywords);
        verify(homeStatsAppService, times(1)).getStats();
        verify(categoryAppService, times(1)).getCategories(true);
        verify(columnAppService, times(1)).listRecommendedColumns(3, null);
        verify(rankingAppService, times(1)).listAuthorRankings(3, null);
        verify(recommendationAppService, times(1)).listFeaturedArticles(1, 5);
        verify(recommendationAppService, times(1)).listWeeklyArticles(1L, 4);
        verify(topicAppService, times(1)).listHotTopicsByActivity(7, 6);
        verify(topicAppService, times(1)).listHotTopics(6);
        verify(tagAppService, times(1)).getHotTags(12);
        verify(searchHistoryAppService, times(1)).getHotKeywords(10);
    }
}
