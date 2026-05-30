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

    /** 引用原文. */
    @Size(max = 300, message = "引用原文不能超过 300 字符")
    private String quoteText;

    /** 引用前文. */
    @Size(max = 80, message = "引用前文不能超过 80 字符")
    private String quotePrefix;

    /** 引用后文. */
    @Size(max = 80, message = "引用后文不能超过 80 字符")
    private String quoteSuffix;

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
     * 获取引用原文.
     *
     * @return 引用原文
     */
    public String getQuoteText() {
        return quoteText;
    }

    /**
     * 设置引用原文.
     *
     * @param quoteText 引用原文
     */
    public void setQuoteText(String quoteText) {
        this.quoteText = quoteText;
    }

    /**
     * 获取引用前文.
     *
     * @return 引用前文
     */
    public String getQuotePrefix() {
        return quotePrefix;
    }

    /**
     * 设置引用前文.
     *
     * @param quotePrefix 引用前文
     */
    public void setQuotePrefix(String quotePrefix) {
        this.quotePrefix = quotePrefix;
    }

    /**
     * 获取引用后文.
     *
     * @return 引用后文
     */
    public String getQuoteSuffix() {
        return quoteSuffix;
    }

    /**
     * 设置引用后文.
     *
     * @param quoteSuffix 引用后文
     */
    public void setQuoteSuffix(String quoteSuffix) {
        this.quoteSuffix = quoteSuffix;
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
