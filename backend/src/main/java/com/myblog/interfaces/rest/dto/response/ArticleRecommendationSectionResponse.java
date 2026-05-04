package com.myblog.interfaces.rest.dto.response;

import java.util.ArrayList;
import java.util.List;

/**
 * 文章推荐分组响应。
 */
public class ArticleRecommendationSectionResponse {

    private String key;
    private String title;
    private List<ArticleResponse> items = new ArrayList<ArticleResponse>();

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<ArticleResponse> getItems() {
        return items;
    }

    public void setItems(List<ArticleResponse> items) {
        this.items = items == null ? new ArrayList<ArticleResponse>() : items;
    }
}
