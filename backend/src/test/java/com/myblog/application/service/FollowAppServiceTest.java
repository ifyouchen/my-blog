package com.myblog.application.service;

import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.UserDTO;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.aggregate.UserFollow;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.UserFollowRepository;
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
 * 关注应用服务测试。
 *
 * @author Codex
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class FollowAppServiceTest {

    @Mock
    private UserFollowRepository userFollowRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ArticleRepository articleRepository;

    @InjectMocks
    private FollowAppService followAppService;

    /**
     * 再次关注时恢复逻辑删除关系。
     */
    @Test
    @DisplayName("再次关注时应恢复已逻辑删除的关注关系")
    void shouldReactivateDeletedFollowWhenFollowingAgain() {
        User targetUser = createUser(2002L, "author_b");
        UserFollow deletedFollow = UserFollow.restore(
            9001L,
            new UserId(1001L),
            new UserId(2002L),
            LocalDateTime.now().minusDays(2),
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().minusHours(1),
            1
        );

        when(userRepository.findById(new UserId(2002L))).thenReturn(Optional.of(targetUser));
        when(userFollowRepository.findByUsersIncludingDeleted(new UserId(1001L), new UserId(2002L)))
            .thenReturn(Optional.of(deletedFollow));

        followAppService.followUser(2002L, 1001L);

        assertThat(deletedFollow.isDeleted()).isFalse();
        verify(userFollowRepository).save(deletedFollow);
    }

    /**
     * 关注流只返回已关注作者文章并按页截取。
     */
    @Test
    @DisplayName("关注流应只返回已关注作者的已发布文章")
    void shouldPageOnlyArticlesFromFollowedAuthors() {
        Article firstFollowed = createArticle(501L, 2003L, "A");
        Article ignored = createArticle(502L, 3001L, "B");
        Article secondFollowed = createArticle(503L, 2002L, "C");
        User firstAuthor = createUser(2003L, "author_c");

        when(userFollowRepository.findFollowingUserIds(new UserId(1001L)))
            .thenReturn(Arrays.asList(2002L, 2003L));
        when(articleRepository.findPublished(null, null, null, "hot"))
            .thenReturn(Arrays.asList(firstFollowed, ignored, secondFollowed));
        when(userRepository.findById(new UserId(2003L))).thenReturn(Optional.of(firstAuthor));

        PageResult<ArticleDTO> result = followAppService.pageFollowingFeed(1001L, 1, 1, "hot");

        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getId()).isEqualTo(501L);
        assertThat(result.getItems().get(0).getAuthor().getId()).isEqualTo(2003L);
    }

    /**
     * 关注作者列表返回 DTO。
     */
    @Test
    @DisplayName("我的关注列表应返回真实作者信息")
    void shouldReturnFollowingUsers() {
        User followedUser = createUser(2002L, "author_b");
        when(userFollowRepository.findFollowingUserIds(new UserId(1001L))).thenReturn(Collections.singletonList(2002L));
        when(userRepository.findById(new UserId(2002L))).thenReturn(Optional.of(followedUser));

        java.util.List<UserDTO> result = followAppService.listFollowingUsers(1001L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(2002L);
        assertThat(result.get(0).getUsername()).isEqualTo("author_b");
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

    private Article createArticle(Long id, Long authorId, String title) {
        return Article.restore(
            id,
            new UserId(authorId),
            title,
            "summary",
            "content",
            "https://example.com/cover.png",
            "Java",
            Collections.singletonList("Spring Boot"),
            ArticleStatus.PUBLISHED,
            100,
            10,
            5,
            2,
            LocalDateTime.now().minusDays(3),
            LocalDateTime.now().minusDays(2),
            LocalDateTime.now().minusDays(2),
            0
        );
    }
}
