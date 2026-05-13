package com.myblog.interfaces.rest.dto.response;

import java.util.ArrayList;
import java.util.List;

/**
 * 文章推荐响应.
 * <p>
 * 包含多个推荐分组的文章推荐结果，用于首页或详情页的"猜你喜欢"模块.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class ArticleRecommendationsResponse {

    /** 推荐分组列表（如 "相关推荐"、"同作者推荐" 等）. */
    private List<ArticleRecommendationSectionResponse> sections =
        new ArrayList<ArticleRecommendationSectionResponse>();

    /**
     * 获取推荐分组列表.
     *
     * @return 推荐分组列表
     */
    public List<ArticleRecommendationSectionResponse> getSections() {
        return sections;
    }

    /**
     * 设置推荐分组列表.
     *
     * @param sections 推荐分组列表
     */
    public void setSections(List<ArticleRecommendationSectionResponse> sections) {
        this.sections = sections == null ? new ArrayList<ArticleRecommendationSectionResponse>() : sections;
    }
}
