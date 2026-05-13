package com.myblog.application.dto;

/**
 * 搜索历史数据传输对象。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class SearchHistoryDTO {

    /** 搜索记录 ID */
    private Long id;
    /** 搜索关键词 */
    private String keyword;
    /** 搜索次数 */
    private Integer searchCount;
    /** 最后搜索时间 */
    private String lastSearchedAt;

    /**
     * 获取搜索记录 ID。
     *
     * @return 搜索记录 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置搜索记录 ID。
     *
     * @param id 搜索记录 ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取搜索关键词。
     *
     * @return 搜索关键词
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * 设置搜索关键词。
     *
     * @param keyword 搜索关键词
     */
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    /**
     * 获取搜索次数。
     *
     * @return 搜索次数
     */
    public Integer getSearchCount() {
        return searchCount;
    }

    /**
     * 设置搜索次数。
     *
     * @param searchCount 搜索次数
     */
    public void setSearchCount(Integer searchCount) {
        this.searchCount = searchCount;
    }

    /**
     * 获取最后搜索时间。
     *
     * @return 最后搜索时间
     */
    public String getLastSearchedAt() {
        return lastSearchedAt;
    }

    /**
     * 设置最后搜索时间。
     *
     * @param lastSearchedAt 最后搜索时间
     */
    public void setLastSearchedAt(String lastSearchedAt) {
        this.lastSearchedAt = lastSearchedAt;
    }
}