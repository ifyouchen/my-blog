package com.myblog.application.service;

import com.myblog.application.command.CreateCommentCommand;
import com.myblog.application.dto.CommentDTO;
import com.myblog.application.event.CommentCreatedEvent;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.Comment;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.CommentLikeRepository;
import com.myblog.domain.repository.CommentRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.shared.enums.ArticleStatus;
import com.myblog.shared.exception.ApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentAppServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SensitiveWordAppService sensitiveWordAppService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CommentAppService service;

    @BeforeEach
    void setUp() {
        service = new CommentAppService(
            commentRepository,
            commentLikeRepository,
            articleRepository,
            userRepository,
            sensitiveWordAppService,
            eventPublisher
        );
    }

    @Test
    void createCommentPublishesNormalCommentAndReturnsEditableDto() {
        Article article = publishedArticle(100L, 2L);
        User user = User.create(1L, "coder", "coder@example.com", "encoded-password");
        when(articleRepository.findById(any(ArticleId.class))).thenReturn(Optional.of(article));
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(user));
        when(commentRepository.nextId()).thenReturn(1595L);
        when(sensitiveWordAppService.detectBlockWords("hello")).thenReturn(Collections.<String>emptyList());
        when(sensitiveWordAppService.detectWarnWords("hello")).thenReturn(Collections.<String>emptyList());
        when(sensitiveWordAppService.maskWarnWords(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        CommentDTO dto = service.createComment(createCommand("hello"), "USER");

        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(commentCaptor.capture());
        Comment savedComment = commentCaptor.getValue();
        assertThat(savedComment.isPublished()).isTrue();
        assertThat(savedComment.getContent()).isEqualTo("hello");
        assertThat(dto.getStatus()).isEqualTo("PUBLISHED");
        assertThat(dto.getCanEdit()).isTrue();
        assertThat(dto.getId()).isEqualTo(1595L);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(CommentCreatedEvent.class);
        CommentCreatedEvent event = (CommentCreatedEvent) eventCaptor.getValue();
        assertThat(event.getCommentId()).isEqualTo(1595L);
        assertThat(event.getArticleId()).isEqualTo(100L);
        assertThat(event.getAuthorId()).isEqualTo(1L);
    }

    @Test
    void createCommentMasksWarnWordsButStillPublishes() {
        Article article = publishedArticle(100L, 2L);
        User user = User.create(1L, "coder", "coder@example.com", "encoded-password");
        when(articleRepository.findById(any(ArticleId.class))).thenReturn(Optional.of(article));
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(user));
        when(commentRepository.nextId()).thenReturn(1596L);
        when(sensitiveWordAppService.detectBlockWords("hello warn")).thenReturn(Collections.<String>emptyList());
        when(sensitiveWordAppService.detectWarnWords("hello warn")).thenReturn(Collections.singletonList("warn"));
        when(sensitiveWordAppService.maskWarnWords("hello warn")).thenReturn("hello ***");

        CommentDTO dto = service.createComment(createCommand("hello warn"), "USER");

        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(commentCaptor.capture());
        assertThat(commentCaptor.getValue().isPublished()).isTrue();
        assertThat(commentCaptor.getValue().getContent()).isEqualTo("hello ***");
        assertThat(dto.getStatus()).isEqualTo("PUBLISHED");
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    void createCommentRejectsBlockWordsWithoutSavingOrPublishingEvent() {
        Article article = publishedArticle(100L, 2L);
        User user = User.create(1L, "coder", "coder@example.com", "encoded-password");
        when(articleRepository.findById(any(ArticleId.class))).thenReturn(Optional.of(article));
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(user));
        when(sensitiveWordAppService.detectBlockWords("blocked"))
            .thenReturn(Collections.singletonList("blocked"));

        assertThatThrownBy(() -> service.createComment(createCommand("blocked"), "USER"))
            .isInstanceOf(ApplicationException.class)
            .hasMessageContaining("内容包含被禁止的敏感词");

        verify(commentRepository, never()).save(any(Comment.class));
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    private CreateCommentCommand createCommand(String content) {
        CreateCommentCommand command = new CreateCommentCommand();
        command.setArticleId(100L);
        command.setUserId(1L);
        command.setParentId(0L);
        command.setRootCommentId(0L);
        command.setContent(content);
        return command;
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
