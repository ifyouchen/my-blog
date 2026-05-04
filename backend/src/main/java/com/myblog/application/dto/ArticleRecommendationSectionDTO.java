package com.myblog.application.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 文章推荐分组应用数据。
 */
public class ArticleRecommendationSectionDTO {

    private String key;
    private String title;
    private List<ArticleDTO> items = new ArrayList<ArticleDTO>();

    public ArticleRecommendationSectionDTO() {
    }

    public ArticleRecommendationSectionDTO(String key, String title, List<ArticleDTO> items) {
        this.key = key;
        this.title = title;
        this.items = items == null ? new ArrayList<ArticleDTO>() : items;
    }

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

    public List<ArticleDTO> getItems() {
        return items;
    }

    public void setItems(List<ArticleDTO> items) {
        this.items = items == null ? new ArrayList<ArticleDTO>() : items;
    }
}
