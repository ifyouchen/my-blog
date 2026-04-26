package com.myblog.application.service;

import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.ColumnDTO;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.Column;
import com.myblog.domain.model.aggregate.ColumnSubscription;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.ColumnId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.ColumnRepository;
import com.myblog.domain.repository.ColumnSubscriptionRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.shared.enums.ArticleStatus;
import com.myblog.shared.enums.UserRole;
import com.myblog.shared.enums.UserStatus;
import com.myblog.shared.result.PageResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 专栏应用服务测试。
 *
 * @author Codex
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class ColumnAppServiceTest {

    @Mock
    private ColumnRepository columnRepository;

    @Mock
    private ColumnSubscriptionRepository columnSubscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ArticleRepository articleRepository;

    @InjectMocks
    private ColumnAppService columnAppService;

    /**
     * 订阅时恢复已删除关系。
     */
    @Test
    @DisplayName("再次订阅时应恢复已逻辑删除的订阅关系")
    void shouldReactivateDeletedSubscriptionWhenSubscribingAgain() {
        Column column = createColumn(3001L, 1002L, 2, 4);
        ColumnSubscription deletedSubscription = ColumnSubscription.restore(
            8101L,
            new ColumnId(3001L),
            new UserId(1001L),
            LocalDateTime.now().minusDays(3),
            LocalDateTime.now().minusDays(2),
            LocalDateTime.now().minusHours(1),
            1
        );

        when(columnRepository.findById(new ColumnId(3001L))).thenReturn(Optional.of(column));
        when(columnSubscriptionRepository.findByColumnAndUserIncludingDeleted(new ColumnId(3001L), new UserId(1001L)))
            .thenReturn(Optional.of(deletedSubscription));

        columnAppService.subscribeColumn(3001L, 1001L);

        assertThat(deletedSubscription.isDeleted()).isFalse();
        assertThat(column.getSubscriberCount()).isEqualTo(3);
        verify(columnSubscriptionRepository).save(deletedSubscription);
        verify(columnRepository).save(column);
    }

    /**
     * 专栏分页应带订阅状态。
     */
    @Test
    @DisplayName("专栏分页应返回当前用户订阅状态")
    void shouldReturnSubscribedStateInColumnPage() {
        Column column = createColumn(3001L, 1002L, 5, 6);
        User author = createUser(1002L, "column_author");

        when(columnRepository.findPublished(1, 9)).thenReturn(Collections.singletonList(column));
        when(columnRepository.countPublished()).thenReturn(1L);
        when(userRepository.findById(new UserId(1002L))).thenReturn(Optional.of(author));
        when(columnSubscriptionRepository.exists(new ColumnId(3001L), new UserId(1001L))).thenReturn(true);

        PageResult<ColumnDTO> result = columnAppService.pageColumns(1, 9, 1001L);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).isSubscribed()).isTrue();
        assertThat(result.getItems().get(0).getAuthor().getId()).isEqualTo(1002L);
    }

    /**
     * 专栏文章分页只返回已发布文章。
     */
    @Test
    @DisplayName("专栏文章分页应忽略未发布文章")
    void shouldOnlyReturnPublishedArticlesForColumnDetail() {
        Column column = createColumn(3001L, 1002L, 5, 2);
        Article published = createArticle(7001L, 1002L, "published", ArticleStatus.PUBLISHED);
        Article draft = createArticle(7002L, 1002L, "draft", ArticleStatus.DRAFT);
        User author = createUser(1002L, "column_author");

        when(columnRepository.findById(new ColumnId(3001L))).thenReturn(Optional.of(column));
        when(columnRepository.findArticleIds(new ColumnId(3001L))).thenReturn(Arrays.asList(7001L, 7002L));
        when(articleRepository.findById(new ArticleId(7001L))).thenReturn(Optional.of(published));
        when(articleRepository.findById(new ArticleId(7002L))).thenReturn(Optional.of(draft));
        when(userRepository.findById(new UserId(1002L))).thenReturn(Optional.of(author));

        PageResult<ArticleDTO> result = columnAppService.pageColumnArticles(3001L, 1, 10);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getId()).isEqualTo(7001L);
    }

    private Column createColumn(Long id, Long authorId, int subscriberCount, int articleCount) {
        return Column.restore(
            id,
            new UserId(authorId),
            "column",
            "summary",
            "https://example.com/column-cover.png",
            "PUBLISHED",
            10,
            subscriberCount,
            articleCount,
            LocalDateTime.now().minusDays(5),
            LocalDateTime.now().minusDays(1),
            null,
            0
        );
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

    private Article createArticle(Long id, Long authorId, String title, ArticleStatus status) {
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
            30,
            5,
            2,
            0,
            LocalDateTime.now().minusDays(3),
            LocalDateTime.now().minusDays(2),
            status == ArticleStatus.PUBLISHED ? LocalDateTime.now().minusDays(2) : null,
            0
        );
    }
}
