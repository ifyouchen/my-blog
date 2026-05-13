package com.myblog.interfaces.rest.dto.request;

import java.util.List;

/**
 * 后台批量文章操作请求.
 * <p>
 * 管理员批量更新文章状态（如审核通过、驳回等）的请求参数.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class AdminBatchArticleRequest {

    /** 待操作的文章ID列表. */
    private List<Long> ids;

    /** 目标状态（如 PUBLISHED、REJECTED 等）. */
    private String status;

    /**
     * 获取文章ID列表.
     *
     * @return 文章ID列表
     */
    public List<Long> getIds() {
        return ids;
    }

    /**
     * 设置文章ID列表.
     *
     * @param ids 文章ID列表
     */
    public void setIds(List<Long> ids) {
        this.ids = ids;
    }

    /**
     * 获取目标状态.
     *
     * @return 目标状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置目标状态.
     *
     * @param status 目标状态
     */
    public void setStatus(String status) {
        this.status = status;
    }
}
