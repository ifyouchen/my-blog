package com.myblog.application.dto;

import java.util.List;

/**
 * 搜索启动数据。
 *
 * @author Codex
 * @since 1.0.0
 */
public class SearchBootstrapDTO {

    private List<CategoryDTO> categories;
    private List<TagDTO> tags;
    private List<String> hotKeywords;
    private List<String> recentKeywords;

    public List<CategoryDTO> getCategories() {
        return categories;
    }

    public void setCategories(List<CategoryDTO> categories) {
        this.categories = categories;
    }

    public List<TagDTO> getTags() {
        return tags;
    }

    public void setTags(List<TagDTO> tags) {
        this.tags = tags;
    }

    public List<String> getHotKeywords() {
        return hotKeywords;
    }

    public void setHotKeywords(List<String> hotKeywords) {
        this.hotKeywords = hotKeywords;
    }

    public List<String> getRecentKeywords() {
        return recentKeywords;
    }

    public void setRecentKeywords(List<String> recentKeywords) {
        this.recentKeywords = recentKeywords;
    }
}