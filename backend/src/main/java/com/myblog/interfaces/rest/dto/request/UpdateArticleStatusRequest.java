package com.myblog.interfaces.rest.dto.request;

/**
 * 更新文章状态请求.
 * <p>
 * 管理员或作者修改文章状态（如发布、撤回、归档等）的请求参数.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class UpdateArticleStatusRequest {

    /** 目标状态（如 DRAFT、PUBLISHED、ARCHIVED 等）. */
    private String status;

    /**
     * 获取目标文章状态.
     *
     * @return 文章状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置目标文章状态.
     *
     * @param status 文章状态
     */
    public void setStatus(String status) {
        this.status = status;
    }
}
