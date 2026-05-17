package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.model.valueobject.ArticleRecommendationApplication;
import com.myblog.domain.repository.ArticleRecommendationApplicationRepository;
import com.myblog.infrastructure.repository.persistence.mapper.ArticleRecommendationApplicationMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 首页推荐申请仓储实现.
 *
 * @author Codex
 * @since 2026-05-17
 */
@Repository
public class ArticleRecommendationApplicationRepositoryImpl implements ArticleRecommendationApplicationRepository {

    private final ArticleRecommendationApplicationMapper mapper;

    public ArticleRecommendationApplicationRepositoryImpl(ArticleRecommendationApplicationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(ArticleRecommendationApplication application) {
        mapper.insert(application);
    }

    @Override
    public boolean update(ArticleRecommendationApplication application) {
        return mapper.updateById(application) > 0;
    }

    @Override
    public Optional<ArticleRecommendationApplication> findPendingByArticleId(Long articleId) {
        return mapper.selectPendingByArticleId(articleId);
    }

    @Override
    public Optional<ArticleRecommendationApplication> findById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<ArticleRecommendationApplication> findByArticleIds(List<Long> articleIds) {
        return mapper.selectByArticleIds(articleIds);
    }

    @Override
    public long countForAdmin(String status) {
        return mapper.countForAdmin(status);
    }

    @Override
    public List<ArticleRecommendationApplication> findPageForAdmin(String status, int offset, int limit) {
        return mapper.selectPageForAdmin(status, offset, limit);
    }
}
