package com.myblog.interfaces.rest.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.util.List;

/**
 * 评论响应.
 * <p>
 * 评论详情信息，包含评论内容、状态、权限标识、作者信息及回复预览等.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class CommentResponse {

    /** 评论ID. */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 所属文章ID. */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long articleId;

    /** 评论者用户ID. */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /** 根评论ID（用于嵌套回复结构）. */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long rootCommentId;

    /** 父评论ID（直接回复的目标评论）. */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    /** 评论内容. */
    private String content;

    /** 评论状态（如 PUBLISHED、PENDING、REJECTED）. */
    private String status;

    /** 创建时间（ISO格式）. */
    private String createdAt;

    /** 最后编辑时间（ISO格式）. */
    private String editedAt;

    /** 编辑次数. */
    private Integer editCount;

    /** 当前用户是否可编辑该评论. */
    private Boolean canEdit;

    /** 回复数量. */
    private Integer replyCount;

    /** 点赞数. */
    private Integer likeCount;

    /** 当前用户是否已点赞. */
    private Boolean liked;

    /** 是否置顶.* */
    private Boolean pinned;

    /** 当前用户是否可删除该评论. */
    private Boolean canDelete;

    /** 当前用户是否可置顶该评论. */
    private Boolean canPin;

    /** 是否为文章作者的评论. */
    private Boolean author;

    /** 评论者用户信息. */
    private UserResponse user;

    /** 被回复的用户信息（仅回复评论时有值）. */
    private UserResponse replyToUser;

    /** 回复预览列表（前N条子评论）. */
    private List<CommentResponse> replyPreview;

    /**
     * 获取评论ID.
     *
     * @return 评论ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置评论ID.
     *
     * @param id 评论ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取所属文章ID.
     *
     * @return 文章ID
     */
    public Long getArticleId() {
        return articleId;
    }

    /**
     * 设置所属文章ID.
     *
     * @param articleId 文章ID
     */
    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    /**
     * 获取评论者用户ID.
     *
     * @return 用户ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置评论者用户ID.
     *
     * @param userId 用户ID
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 获取根评论ID.
     *
     * @return 根评论ID
     */
    public Long getRootCommentId() {
        return rootCommentId;
    }

    /**
     * 设置根评论ID.
     *
     * @param rootCommentId 根评论ID
     */
    public void setRootCommentId(Long rootCommentId) {
        this.rootCommentId = rootCommentId;
    }

    /**
     * 获取父评论ID.
     *
     * @return 父评论ID
     */
    public Long getParentId() {
        return parentId;
    }

    /**
     * 设置父评论ID.
     *
     * @param parentId 父评论ID
     */
    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    /**
     * 获取评论内容.
     *
     * @return 评论内容
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置评论内容.
     *
     * @param content 评论内容
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 获取评论状态.
     *
     * @return 评论状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置评论状态.
     *
     * @param status 评论状态
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 获取创建时间.
     *
     * @return 创建时间（ISO格式）
     */
    public String getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间.
     *
     * @param createdAt 创建时间（ISO格式）
     */
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取最后编辑时间.
     *
     * @return 编辑时间（ISO格式）
     */
    public String getEditedAt() {
        return editedAt;
    }

    /**
     * 设置最后编辑时间.
     *
     * @param editedAt 编辑时间（ISO格式）
     */
    public void setEditedAt(String editedAt) {
        this.editedAt = editedAt;
    }

    /**
     * 获取编辑次数.
     *
     * @return 编辑次数
     */
    public Integer getEditCount() {
        return editCount;
    }

    /**
     * 设置编辑次数.
     *
     * @param editCount 编辑次数
     */
    public void setEditCount(Integer editCount) {
        this.editCount = editCount;
    }

    /**
     * 当前用户是否可编辑该评论.
     *
     * @return 是否可编辑
     */
    public Boolean getCanEdit() {
        return canEdit;
    }

    /**
     * 设置是否可编辑.
     *
     * @param canEdit 是否可编辑
     */
    public void setCanEdit(Boolean canEdit) {
        this.canEdit = canEdit;
    }

    /**
     * 获取回复数量.
     *
     * @return 回复数
     */
    public Integer getReplyCount() {
        return replyCount;
    }

    /**
     * 设置回复数量.
     *
     * @param replyCount 回复数
     */
    public void setReplyCount(Integer replyCount) {
        this.replyCount = replyCount;
    }

    /**
     * 获取点赞数.
     *
     * @return 点赞数
     */
    public Integer getLikeCount() {
        return likeCount;
    }

    /**
     * 设置点赞数.
     *
     * @param likeCount 点赞数
     */
    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    /**
     * 当前用户是否已点赞.
     *
     * @return 是否已点赞
     */
    public Boolean getLiked() {
        return liked;
    }

    /**
     * 设置是否已点赞.
     *
     * @param liked 是否已点赞
     */
    public void setLiked(Boolean liked) {
        this.liked = liked;
    }

    /**
     * 是否置顶.
     *
     * @return 是否置顶
     */
    public Boolean getPinned() {
        return pinned;
    }

    /**
     * 设置是否置顶.
     *
     * @param pinned 是否置顶
     */
    public void setPinned(Boolean pinned) {
        this.pinned = pinned;
    }

    /**
     * 当前用户是否可删除该评论.
     *
     * @return 是否可删除
     */
    public Boolean getCanDelete() {
        return canDelete;
    }

    /**
     * 设置是否可删除.
     *
     * @param canDelete 是否可删除
     */
    public void setCanDelete(Boolean canDelete) {
        this.canDelete = canDelete;
    }

    /**
     * 当前用户是否可置顶该评论.
     *
     * @return 是否可置顶
     */
    public Boolean getCanPin() {
        return canPin;
    }

    /**
     * 设置是否可置顶.
     *
     * @param canPin 是否可置顶
     */
    public void setCanPin(Boolean canPin) {
        this.canPin = canPin;
    }

    /**
     * 是否为文章作者的评论.
     *
     * @return 是否为作者评论
     */
    public Boolean getAuthor() {
        return author;
    }

    /**
     * 设置是否为文章作者的评论.
     *
     * @param author 是否为作者评论
     */
    public void setAuthor(Boolean author) {
        this.author = author;
    }

    /**
     * 获取评论者用户信息.
     *
     * @return 用户信息
     */
    public UserResponse getUser() {
        return user;
    }

    /**
     * 设置评论者用户信息.
     *
     * @param user 用户信息
     */
    public void setUser(UserResponse user) {
        this.user = user;
    }

    /**
     * 获取被回复的用户信息.
     *
     * @return 被回复用户信息
     */
    public UserResponse getReplyToUser() {
        return replyToUser;
    }

    /**
     * 设置被回复的用户信息.
     *
     * @param replyToUser 被回复用户信息
     */
    public void setReplyToUser(UserResponse replyToUser) {
        this.replyToUser = replyToUser;
    }

    /**
     * 获取回复预览列表.
     *
     * @return 子评论列表
     */
    public List<CommentResponse> getReplyPreview() {
        return replyPreview;
    }

    /**
     * 设置回复预览列表.
     *
     * @param replyPreview 子评论列表
     */
    public void setReplyPreview(List<CommentResponse> replyPreview) {
        this.replyPreview = replyPreview;
    }
}
