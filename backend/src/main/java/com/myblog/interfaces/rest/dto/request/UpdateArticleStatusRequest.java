package com.myblog.interfaces.rest.dto.request;

/**
 * 更新文章状态请求。
 *
 * @author Codex
 * @since 1.0.0
 */
public class UpdateArticleStatusRequest {

    private String status;

    /**
     * 获取文章状态。
     *
     * @return 文章状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置文章状态。
     *
     * @param status 文章状态
     */
    public void setStatus(String status) {
        this.status = status;
    }
}
