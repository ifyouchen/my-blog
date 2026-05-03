package com.myblog.application.listener;

import com.myblog.application.event.CommentCreatedEvent;
import com.myblog.domain.model.aggregate.Comment;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.CommentId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.CommentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleStatsEventListenerTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private CommentRepository commentRepository;

    @Test
    void onCommentCreatedIncrementsCountForPublishedComment() {
        Comment comment = comment(1595L, 100L, 1L);
        comment.approve();
        when(commentRepository.findByIdForAdmin(any(CommentId.class))).thenReturn(Optional.of(comment));

        listener().onCommentCreated(new CommentCreatedEvent(1595L, 100L, 1L));

        verify(articleRepository).incrementCommentCount(100L);
    }

    @Test
    void onCommentCreatedSkipsPendingComment() {
        Comment comment = comment(1595L, 100L, 1L);
        when(commentRepository.findByIdForAdmin(any(CommentId.class))).thenReturn(Optional.of(comment));

        listener().onCommentCreated(new CommentCreatedEvent(1595L, 100L, 1L));

        verify(articleRepository, never()).incrementCommentCount(100L);
    }

    private ArticleStatsEventListener listener() {
        return new ArticleStatsEventListener(articleRepository, commentRepository);
    }

    private Comment comment(Long commentId, Long articleId, Long userId) {
        return Comment.create(
            commentId,
            new ArticleId(articleId),
            new UserId(userId),
            commentId,
            null,
            "comment"
        );
    }
}
