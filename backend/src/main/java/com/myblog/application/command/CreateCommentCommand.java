package com.myblog.application.command;

/**
 * 创建评论命令。
 *
 * @author Codex
 * @since 1.0.0
 */
public class CreateCommentCommand {

    private Long articleId;
    private Long userId;
    private Long rootCommentId;
    private Long parentId;
    private String content;

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public Long getUserId() {
        return userId;
    }

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

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
