package com.myblog.interfaces.rest.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 创建评论请求.
 * <p>
 * 用户新建评论时的请求参数，支持顶级评论和回复评论.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class CreateCommentRequest {

    /** 评论内容. */
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容不能超过 1000 字符")
    private String content;

    /** 根评论ID（回复时用于标识所属根评论）. */
    private Long rootCommentId;

    /** 父评论ID（直接回复的目标评论ID）. */
    private Long parentId;

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
}
