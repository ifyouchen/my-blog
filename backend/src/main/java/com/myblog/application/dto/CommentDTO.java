package com.myblog.application.dto;

import java.util.List;

/**
 * 评论数据传输对象。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class CommentDTO {

    /** 评论 ID */
    private Long id;
    /** 文章 ID */
    private Long articleId;
    /** 评论用户 ID */
    private Long userId;
    /** 根评论 ID */
    private Long rootCommentId;
    /** 父评论 ID */
    private Long parentId;
    /** 评论内容 */
    private String content;
    /** 评论状态 */
    private String status;
    /** 创建时间 */
    private String createdAt;
    /** 编辑时间 */
    private String editedAt;
    /** 编辑次数 */
    private Integer editCount;
    /** 回复数量 */
    private Integer replyCount;
    /** 点赞数量 */
    private Integer likeCount;
    /** 当前用户是否已点赞 */
    private Boolean liked;
    /** 是否置顶 */
    private Boolean pinned;
    /** 当前用户是否可编辑 */
    private Boolean canEdit;
    /** 当前用户是否可删除 */
    private Boolean canDelete;
    /** 当前用户是否可置顶 */
    private Boolean canPin;
    /** 是否为文章作者 */
    private Boolean author;
    /** 评论用户信息 */
    private UserDTO user;
    /** 被回复的用户信息 */
    private UserDTO replyToUser;
    /** 回复预览列表 */
    private List<CommentDTO> replyPreview;

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
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public String getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间。
     *
     * @param createdAt 创建时间
     */
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取编辑时间。
     *
     * @return 编辑时间
     */
    public String getEditedAt() {
        return editedAt;
    }

    /**
     * 设置编辑时间。
     *
     * @param editedAt 编辑时间
     */
    public void setEditedAt(String editedAt) {
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
     * 获取回复数量。
     *
     * @return 回复数量
     */
    public Integer getReplyCount() {
        return replyCount;
    }

    /**
     * 设置回复数量。
     *
     * @param replyCount 回复数量
     */
    public void setReplyCount(Integer replyCount) {
        this.replyCount = replyCount;
    }

    /**
     * 获取点赞数量。
     *
     * @return 点赞数量
     */
    public Integer getLikeCount() {
        return likeCount;
    }

    /**
     * 设置点赞数量。
     *
     * @param likeCount 点赞数量
     */
    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    /**
     * 获取当前用户是否已点赞。
     *
     * @return 是否已点赞
     */
    public Boolean getLiked() {
        return liked;
    }

    /**
     * 设置当前用户是否已点赞。
     *
     * @param liked 是否已点赞
     */
    public void setLiked(Boolean liked) {
        this.liked = liked;
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
     * 获取当前用户是否可编辑。
     *
     * @return 是否可编辑
     */
    public Boolean getCanEdit() {
        return canEdit;
    }

    /**
     * 设置当前用户是否可编辑。
     *
     * @param canEdit 是否可编辑
     */
    public void setCanEdit(Boolean canEdit) {
        this.canEdit = canEdit;
    }

    /**
     * 获取当前用户是否可删除。
     *
     * @return 是否可删除
     */
    public Boolean getCanDelete() {
        return canDelete;
    }

    /**
     * 设置当前用户是否可删除。
     *
     * @param canDelete 是否可删除
     */
    public void setCanDelete(Boolean canDelete) {
        this.canDelete = canDelete;
    }

    /**
     * 获取当前用户是否可置顶。
     *
     * @return 是否可置顶
     */
    public Boolean getCanPin() {
        return canPin;
    }

    /**
     * 设置当前用户是否可置顶。
     *
     * @param canPin 是否可置顶
     */
    public void setCanPin(Boolean canPin) {
        this.canPin = canPin;
    }

    /**
     * 获取是否为文章作者。
     *
     * @return 是否为文章作者
     */
    public Boolean getAuthor() {
        return author;
    }

    /**
     * 设置是否为文章作者。
     *
     * @param author 是否为文章作者
     */
    public void setAuthor(Boolean author) {
        this.author = author;
    }

    /**
     * 获取评论用户信息。
     *
     * @return 评论用户信息
     */
    public UserDTO getUser() {
        return user;
    }

    /**
     * 设置评论用户信息。
     *
     * @param user 评论用户信息
     */
    public void setUser(UserDTO user) {
        this.user = user;
    }

    /**
     * 获取被回复的用户信息。
     *
     * @return 被回复的用户信息
     */
    public UserDTO getReplyToUser() {
        return replyToUser;
    }

    /**
     * 设置被回复的用户信息。
     *
     * @param replyToUser 被回复的用户信息
     */
    public void setReplyToUser(UserDTO replyToUser) {
        this.replyToUser = replyToUser;
    }

    /**
     * 获取回复预览列表。
     *
     * @return 回复预览列表
     */
    public List<CommentDTO> getReplyPreview() {
        return replyPreview;
    }

    /**
     * 设置回复预览列表。
     *
     * @param replyPreview 回复预览列表
     */
    public void setReplyPreview(List<CommentDTO> replyPreview) {
        this.replyPreview = replyPreview;
    }
}
