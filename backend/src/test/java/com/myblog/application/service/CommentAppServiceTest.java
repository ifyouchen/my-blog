package com.myblog.application.service;

import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.Comment;
import com.myblog.domain.model.aggregate.CommentLike;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.CommentId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.CommentLikeRepository;
import com.myblog.domain.repository.CommentRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 评论应用服务测试。
 *
 * @author Codex
 * @since 1.0.0
 */
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

    @InjectMocks
    private CommentAppService commentAppService;

    /**
     * 点赞恢复已删除记录。
     */
    @Test
    @DisplayName("再次点赞时应恢复已逻辑删除的点赞记录")
    void shouldReactivateDeletedLikeRecordWhenLikingAgain() {
        Comment comment = Comment.restore(
            701L,
            new ArticleId(101L),
            new UserId(1001L),
            701L,
            null,
            "评论内容",
            Comment.CommentStatus.PUBLISHED,
            2,
            false,
            null,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().minusDays(1)
        );
        CommentLike deletedLike = CommentLike.restore(
            801L,
            new CommentId(701L),
            new UserId(1002L),
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().minusHours(1),
            1
        );

        when(commentRepository.findById(new CommentId(701L))).thenReturn(Optional.of(comment));
        when(commentLikeRepository.findAnyByCommentAndUser(new CommentId(701L), new UserId(1002L)))
            .thenReturn(Optional.of(deletedLike));

        commentAppService.likeComment(701L, 1002L);

        assertThat(deletedLike.isDeleted()).isFalse();
        assertThat(comment.getLikeCount()).isEqualTo(3);
        verify(commentLikeRepository).save(deletedLike);
        verify(commentRepository).save(comment);
    }

    /**
     * 删除一级评论时应整串删除。
     */
    @Test
    @DisplayName("删除一级评论时应删除整串楼中楼并同步评论数")
    void shouldDeleteWholeThreadWhenDeletingRootComment() {
        Article article = Article.restore(
            101L,
            new UserId(1001L),
            "标题",
            "摘要",
            "正文",
            null,
            "后端",
            Arrays.asList("Spring Boot"),
            ArticleStatus.PUBLISHED,
            10,
            2,
            1,
            2,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().minusDays(1),
            0
        );
        Comment rootComment = Comment.restore(
            701L,
            new ArticleId(101L),
            new UserId(1002L),
            701L,
            null,
            "一级评论",
            Comment.CommentStatus.PUBLISHED,
            0,
            false,
            null,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().minusDays(1)
        );
        Comment replyComment = Comment.restore(
            702L,
            new ArticleId(101L),
            new UserId(1003L),
            701L,
            701L,
            "楼中楼回复",
            Comment.CommentStatus.PUBLISHED,
            0,
            false,
            null,
            LocalDateTime.now().minusHours(20),
            LocalDateTime.now().minusHours(20)
        );

        when(commentRepository.findById(new CommentId(701L))).thenReturn(Optional.of(rootComment));
        when(articleRepository.findById(new ArticleId(101L))).thenReturn(Optional.of(article));
        when(commentRepository.findThreadByRootCommentId(new CommentId(701L)))
            .thenReturn(Arrays.asList(rootComment, replyComment));

        commentAppService.deleteComment(701L, 1001L, "USER");

        assertThat(rootComment.isDeleted()).isTrue();
        assertThat(replyComment.isDeleted()).isTrue();
        assertThat(article.getCommentCount()).isEqualTo(0);
        verify(commentRepository, times(2)).save(org.mockito.ArgumentMatchers.any(Comment.class));
        verify(articleRepository).save(article);
    }

    /**
     * 删除楼中楼时仅删除当前回复。
     */
    @Test
    @DisplayName("删除楼中楼时应只删除当前回复并同步评论数")
    void shouldDeleteSingleReplyWhenDeletingReply() {
        Article article = Article.restore(
            101L,
            new UserId(1001L),
            "标题",
            "摘要",
            "正文",
            null,
            "后端",
            Arrays.asList("Spring Boot"),
            ArticleStatus.PUBLISHED,
            10,
            2,
            1,
            2,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().minusDays(1),
            0
        );
        Comment replyComment = Comment.restore(
            702L,
            new ArticleId(101L),
            new UserId(1003L),
            701L,
            701L,
            "楼中楼回复",
            Comment.CommentStatus.PUBLISHED,
            0,
            false,
            null,
            LocalDateTime.now().minusHours(20),
            LocalDateTime.now().minusHours(20)
        );

        when(commentRepository.findById(new CommentId(702L))).thenReturn(Optional.of(replyComment));
        when(articleRepository.findById(new ArticleId(101L))).thenReturn(Optional.of(article));

        commentAppService.deleteComment(702L, 1003L, "USER");

        assertThat(replyComment.isDeleted()).isTrue();
        assertThat(article.getCommentCount()).isEqualTo(1);
        verify(commentRepository).save(replyComment);
        verify(articleRepository).save(article);
    }

    /**
     * 置顶一级评论时应清空旧置顶。
     */
    @Test
    @DisplayName("文章作者置顶一级评论时应清空旧置顶并保存当前评论")
    void shouldPinRootCommentForArticleAuthor() {
        Article article = Article.restore(
            101L,
            new UserId(1001L),
            "标题",
            "摘要",
            "正文",
            null,
            "后端",
            Arrays.asList("Spring Boot"),
            ArticleStatus.PUBLISHED,
            10,
            2,
            1,
            2,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().minusDays(1),
            0
        );
        Comment rootComment = Comment.restore(
            701L,
            new ArticleId(101L),
            new UserId(1002L),
            701L,
            null,
            "一级评论",
            Comment.CommentStatus.PUBLISHED,
            0,
            false,
            null,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().minusDays(1)
        );

        when(commentRepository.findById(new CommentId(701L))).thenReturn(Optional.of(rootComment));
        when(articleRepository.findById(new ArticleId(101L))).thenReturn(Optional.of(article));

        commentAppService.pinComment(701L, 1001L, "USER");

        assertThat(rootComment.isPinned()).isTrue();
        verify(commentRepository).clearPinnedByArticleId(new ArticleId(101L));
        verify(commentRepository).save(rootComment);
    }

    /**
     * 回复评论不允许置顶。
     */
    @Test
    @DisplayName("楼中楼回复不允许被置顶")
    void shouldRejectPinningReplyComment() {
        Article article = Article.restore(
            101L,
            new UserId(1001L),
            "标题",
            "摘要",
            "正文",
            null,
            "后端",
            Arrays.asList("Spring Boot"),
            ArticleStatus.PUBLISHED,
            10,
            2,
            1,
            2,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().minusDays(1),
            0
        );
        Comment replyComment = Comment.restore(
            702L,
            new ArticleId(101L),
            new UserId(1003L),
            701L,
            701L,
            "楼中楼回复",
            Comment.CommentStatus.PUBLISHED,
            0,
            false,
            null,
            LocalDateTime.now().minusHours(20),
            LocalDateTime.now().minusHours(20)
        );

        when(commentRepository.findById(new CommentId(702L))).thenReturn(Optional.of(replyComment));
        when(articleRepository.findById(new ArticleId(101L))).thenReturn(Optional.of(article));

        assertThatThrownBy(() -> commentAppService.pinComment(702L, 1001L, "USER"))
            .isInstanceOf(com.myblog.shared.exception.ApplicationException.class)
            .hasMessageContaining("无权限置顶");
    }
}
