package com.myblog.interfaces.rest.dto.response;

import java.util.ArrayList;
import java.util.List;

/**
 * 文章推荐响应。
 */
public class ArticleRecommendationsResponse {

    private List<ArticleRecommendationSectionResponse> sections =
        new ArrayList<ArticleRecommendationSectionResponse>();

    public List<ArticleRecommendationSectionResponse> getSections() {
        return sections;
    }

    public void setSections(List<ArticleRecommendationSectionResponse> sections) {
        this.sections = sections == null ? new ArrayList<ArticleRecommendationSectionResponse>() : sections;
    }
}
