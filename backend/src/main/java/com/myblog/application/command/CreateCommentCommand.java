package com.myblog.application.command;

/**
 * 创建评论命令。
 *
 * @author Codex
 * @since 1.0.0
 */
public class CreateCommentCommand {

    /** 文章 ID */
    private Long articleId;
    /** 评论用户 ID */
    private Long userId;
    /** 根评论 ID，为 null 表示本评论就是根评论 */
    private Long rootCommentId;
    /** 父评论 ID，为 null 表示一级评论 */
    private Long parentId;
    /** 评论内容 */
    private String content;

    /**
     * 获取文章 ID。
     *
     * @return 文章 ID
     */
    public Long getArticleId() {
        return articleId;
    }

    /**
     * 设置文章 ID。
     *
     * @param articleId 文章 ID
     */
    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    /**
     * 获取评论用户 ID。
     *
     * @return 评论用户 ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置评论用户 ID。
     *
     * @param userId 评论用户 ID
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 获取根评论 ID。
     *
     * @return 根评论 ID
     */
    public Long getRootCommentId() {
        return rootCommentId;
    }

    /**
     * 设置根评论 ID。
     *
     * @param rootCommentId 根评论 ID
     */
    public void setRootCommentId(Long rootCommentId) {
        this.rootCommentId = rootCommentId;
    }

    /**
     * 获取父评论 ID。
     *
     * @return 父评论 ID
     */
    public Long getParentId() {
        return parentId;
    }

    /**
     * 设置父评论 ID。
     *
     * @param parentId 父评论 ID
     */
    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    /**
     * 获取评论内容。
     *
     * @return 评论内容
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置评论内容。
     *
     * @param content 评论内容
     */
    public void setContent(String content) {
        this.content = content;
    }
}
