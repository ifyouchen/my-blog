package com.myblog.infrastructure.repository.persistence.entity;

import java.time.LocalDateTime;

/**
 * 评论数据对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class CommentDO {

    /**
     * 评论 ID
     */
    private Long id;

    /**
     * 所属文章 ID
     */
    private Long articleId;

    /**
     * 评论用户 ID
     */
    private Long userId;

    /**
     * 根评论 ID，为 null 表示本评论就是根评论
     */
    private Long rootCommentId;

    /**
     * 父评论 ID，为 null 表示一级评论
     */
    private Long parentId;

    /**
     * 评论正文内容
     */
    private String content;

    /**
     * 引用原文
     */
    private String quoteText;

    /**
     * 引用前文
     */
    private String quotePrefix;

    /**
     * 引用后文
     */
    private String quoteSuffix;

    /**
     * 评论状态（PUBLISHED / DELETED 等）
     */
    private String status;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 最后编辑时间
     */
    private LocalDateTime editedAt;

    /**
     * 编辑次数
     */
    private Integer editCount;

    /**
     * 是否置顶
     */
    private Boolean pinned;

    /**
     * 置顶时间
     */
    private LocalDateTime pinnedAt;

    /**
     * 评论创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 评论最后更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 软删除时间，为 null 表示未删除
     */
    private LocalDateTime deletedAt;

    /**
     * 乐观锁版本号
     */
    private Integer version;

    /**
     * 获取评论 ID。
     *
     * @return 评论 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置评论 ID。
     *
     * @param id 评论 ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取所属文章 ID。
     *
     * @return 文章 ID
     */
    public Long getArticleId() {
        return articleId;
    }

    /**
     * 设置所属文章 ID。
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
     * 获取评论正文内容。
     *
     * @return 评论正文内容
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置评论正文内容。
     *
     * @param content 评论正文内容
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 获取引用原文。
     *
     * @return 引用原文
     */
    public String getQuoteText() {
        return quoteText;
    }

    /**
     * 设置引用原文。
     *
     * @param quoteText 引用原文
     */
    public void setQuoteText(String quoteText) {
        this.quoteText = quoteText;
    }

    /**
     * 获取引用前文。
     *
     * @return 引用前文
     */
    public String getQuotePrefix() {
        return quotePrefix;
    }

    /**
     * 设置引用前文。
     *
     * @param quotePrefix 引用前文
     */
    public void setQuotePrefix(String quotePrefix) {
        this.quotePrefix = quotePrefix;
    }

    /**
     * 获取引用后文。
     *
     * @return 引用后文
     */
    public String getQuoteSuffix() {
        return quoteSuffix;
    }

    /**
     * 设置引用后文。
     *
     * @param quoteSuffix 引用后文
     */
    public void setQuoteSuffix(String quoteSuffix) {
        this.quoteSuffix = quoteSuffix;
    }

    /**
     * 获取评论状态。
     *
     * @return 评论状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置评论状态。
     *
     * @param status 评论状态
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 获取点赞数。
     *
     * @return 点赞数
     */
    public Integer getLikeCount() {
        return likeCount;
    }

    /**
     * 设置点赞数。
     *
     * @param likeCount 点赞数
     */
    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    /**
     * 获取最后编辑时间。
     *
     * @return 最后编辑时间
     */
    public LocalDateTime getEditedAt() {
        return editedAt;
    }

    /**
     * 设置最后编辑时间。
     *
     * @param editedAt 最后编辑时间
     */
    public void setEditedAt(LocalDateTime editedAt) {
        this.editedAt = editedAt;
    }

    /**
     * 获取编辑次数。
     *
     * @return 编辑次数
     */
    public Integer getEditCount() {
        return editCount;
    }

    /**
     * 设置编辑次数。
     *
     * @param editCount 编辑次数
     */
    public void setEditCount(Integer editCount) {
        this.editCount = editCount;
    }

    /**
     * 获取是否置顶。
     *
     * @return 是否置顶
     */
    public Boolean getPinned() {
        return pinned;
    }

    /**
     * 设置是否置顶。
     *
     * @param pinned 是否置顶
     */
    public void setPinned(Boolean pinned) {
        this.pinned = pinned;
    }

    /**
     * 获取置顶时间。
     *
     * @return 置顶时间
     */
    public LocalDateTime getPinnedAt() {
        return pinnedAt;
    }

    /**
     * 设置置顶时间。
     *
     * @param pinnedAt 置顶时间
     */
    public void setPinnedAt(LocalDateTime pinnedAt) {
        this.pinnedAt = pinnedAt;
    }

    /**
     * 获取评论创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置评论创建时间。
     *
     * @param createdAt 创建时间
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取评论最后更新时间。
     *
     * @return 最后更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置评论最后更新时间。
     *
     * @param updatedAt 最后更新时间
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 获取软删除时间。
     *
     * @return 删除时间，未删除则为 null
     */
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    /**
     * 设置软删除时间。
     *
     * @param deletedAt 删除时间
     */
    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    /**
     * 获取乐观锁版本号。
     *
     * @return 版本号
     */
    public Integer getVersion() {
        return version;
    }

    /**
     * 设置乐观锁版本号。
     *
     * @param version 版本号
     */
    public void setVersion(Integer version) {
        this.version = version;
    }
}
