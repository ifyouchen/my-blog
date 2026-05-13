package com.myblog.application.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 文章推荐应用数据。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class ArticleRecommendationsDTO {

    /** 推荐分组列表 */
    private List<ArticleRecommendationSectionDTO> sections = new ArrayList<ArticleRecommendationSectionDTO>();

    /**
     * 默认构造函数。
     */
    public ArticleRecommendationsDTO() {
    }

    /**
     * 使用推荐分组列表构造。
     *
     * @param sections 推荐分组列表
     */
    public ArticleRecommendationsDTO(List<ArticleRecommendationSectionDTO> sections) {
        this.sections = sections == null ? new ArrayList<ArticleRecommendationSectionDTO>() : sections;
    }

    /**
     * 获取推荐分组列表。
     *
     * @return 推荐分组列表
     */
    public List<ArticleRecommendationSectionDTO> getSections() {
        return sections;
    }

    /**
     * 设置推荐分组列表。
     *
     * @param sections 推荐分组列表
     */
    public void setSections(List<ArticleRecommendationSectionDTO> sections) {
        this.sections = sections == null ? new ArrayList<ArticleRecommendationSectionDTO>() : sections;
    }
}
