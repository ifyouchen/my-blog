package com.myblog.application.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 文章推荐应用数据。
 */
public class ArticleRecommendationsDTO {

    private List<ArticleRecommendationSectionDTO> sections = new ArrayList<ArticleRecommendationSectionDTO>();

    public ArticleRecommendationsDTO() {
    }

    public ArticleRecommendationsDTO(List<ArticleRecommendationSectionDTO> sections) {
        this.sections = sections == null ? new ArrayList<ArticleRecommendationSectionDTO>() : sections;
    }

    public List<ArticleRecommendationSectionDTO> getSections() {
        return sections;
    }

    public void setSections(List<ArticleRecommendationSectionDTO> sections) {
        this.sections = sections == null ? new ArrayList<ArticleRecommendationSectionDTO>() : sections;
    }
}
