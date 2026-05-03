package com.myblog.application.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myblog.application.command.CreateNotificationCommand;
import com.myblog.application.event.CommentCreatedEvent;
import com.myblog.application.service.NotificationAppService;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.Comment;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.CommentId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.CommentRepository;
import com.myblog.domain.repository.UserFollowRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.shared.enums.ArticleStatus;
import com.myblog.shared.enums.NotificationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationAppService notificationAppService;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserFollowRepository userFollowRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void onCommentCreatedCreatesNotificationForPublishedRootComment() throws Exception {
        Article article = publishedArticle(100L, 2L);
        Comment comment = publishedComment(1595L, 100L, 1L, "hello comment");
        User actor = User.create(1L, "coder", "coder@example.com", "encoded-password");
        when(commentRepository.findByIdForAdmin(any(CommentId.class))).thenReturn(Optional.of(comment));
        when(articleRepository.findById(any(ArticleId.class))).thenReturn(Optional.of(article));
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(actor));

        listener().onCommentCreated(new CommentCreatedEvent(1595L, 100L, 1L));

        ArgumentCaptor<CreateNotificationCommand> commandCaptor =
            ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(notificationAppService).createNotification(commandCaptor.capture());
        CreateNotificationCommand command = commandCaptor.getValue();
        assertThat(command.getReceiverUserId()).isEqualTo(2L);
        assertThat(command.getActorUserId()).isEqualTo(1L);
        assertThat(command.getType()).isEqualTo(NotificationType.ARTICLE_COMMENT);
        assertThat(command.getArticleId()).isEqualTo(100L);
        assertThat(command.getCommentId()).isEqualTo(1595L);

        JsonNode payload = objectMapper.readTree(command.getPayloadJson());
        assertThat(payload.get("commentExcerpt").asText()).isEqualTo("hello comment");
        assertThat(payload.get("actorName").asText()).isEqualTo("coder");
    }

    @Test
    void onCommentCreatedSkipsPendingCommentWithoutTreatingItAsMissing() {
        Comment pendingComment = Comment.create(
            1595L,
            new ArticleId(100L),
            new UserId(1L),
            1595L,
            null,
            "pending comment"
        );
        when(commentRepository.findByIdForAdmin(any(CommentId.class))).thenReturn(Optional.of(pendingComment));

        listener().onCommentCreated(new CommentCreatedEvent(1595L, 100L, 1L));

        verifyNoInteractions(notificationAppService);
        verify(articleRepository, never()).findById(any(ArticleId.class));
    }

    private NotificationEventListener listener() {
        return new NotificationEventListener(
            notificationAppService,
            articleRepository,
            commentRepository,
            userRepository,
            userFollowRepository,
            objectMapper
        );
    }

    private Comment publishedComment(Long commentId, Long articleId, Long userId, String content) {
        Comment comment = Comment.create(
            commentId,
            new ArticleId(articleId),
            new UserId(userId),
            commentId,
            null,
            content
        );
        comment.approve();
        return comment;
    }

    private Article publishedArticle(Long articleId, Long authorId) {
        return Article.create(
            articleId,
            new UserId(authorId),
            "测试文章",
            "摘要",
            "这里是文章正文",
            null,
            "Java",
            Collections.singletonList("后端"),
            ArticleStatus.PUBLISHED,
            null,
            null,
            null,
            null
        );
    }
}
