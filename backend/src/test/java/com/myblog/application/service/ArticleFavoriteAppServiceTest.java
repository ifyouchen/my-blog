package com.myblog.application.service;

import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.ArticleFavorite;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleFavoriteRepository;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.shared.enums.ArticleStatus;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 文章收藏应用服务测试。
 *
 * @author Codex
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class ArticleFavoriteAppServiceTest {

    @Mock
    private ArticleFavoriteRepository articleFavoriteRepository;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ArticleFavoriteAppService articleFavoriteAppService;

    /**
     * 取消收藏后再次收藏应复用旧记录。
     */
    @Test
    @DisplayName("再次收藏时应恢复已逻辑删除的收藏记录")
    void shouldReactivateDeletedFavoriteInsteadOfCreatingNewOne() {
        Article article = Article.restore(
            495L,
            new UserId(1001L),
            "标题",
            "摘要",
            "正文",
            null,
            "后端",
            Arrays.asList("Java"),
            ArticleStatus.PUBLISHED,
            0,
            0,
            1,
            0,
            LocalDateTime.now(),
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        ArticleFavorite favorite = ArticleFavorite.restore(
            88L,
            new ArticleId(495L),
            new UserId(1002L),
            LocalDateTime.now().minusDays(1L),
            LocalDateTime.now().minusDays(1L),
            LocalDateTime.now().minusHours(1L),
            1
        );

        when(articleRepository.findById(new ArticleId(495L))).thenReturn(Optional.of(article));
        when(articleFavoriteRepository.findByArticleAndUserIncludingDeleted(new ArticleId(495L), new UserId(1002L)))
            .thenReturn(Optional.of(favorite));

        articleFavoriteAppService.favoriteArticle(495L, 1002L);

        assertThat(favorite.isDeleted()).isFalse();
        assertThat(article.getFavoriteCount()).isEqualTo(2);
        verify(articleFavoriteRepository).save(favorite);
        verify(articleRepository).save(article);
    }
}
