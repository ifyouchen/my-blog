package com.myblog.application.dto;

/**
 * 搜索历史数据传输对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class SearchHistoryDTO {

    private Long id;
    private String keyword;
    private Integer searchCount;
    private String lastSearchedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getSearchCount() {
        return searchCount;
    }

    public void setSearchCount(Integer searchCount) {
        this.searchCount = searchCount;
    }

    public String getLastSearchedAt() {
        return lastSearchedAt;
    }

    public void setLastSearchedAt(String lastSearchedAt) {
        this.lastSearchedAt = lastSearchedAt;
    }
}