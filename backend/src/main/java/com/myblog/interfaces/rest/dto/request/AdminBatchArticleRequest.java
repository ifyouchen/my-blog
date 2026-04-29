package com.myblog.interfaces.rest.dto.request;

import java.util.List;

/**
 * 后台批量文章操作请求。
 *
 * @author Codex
 * @since 1.0.0
 */
public class AdminBatchArticleRequest {

    private List<Long> ids;
    private String status;

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
