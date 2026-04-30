package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.repository.ArticleVersionRepository;
import com.myblog.infrastructure.repository.persistence.entity.ArticleVersionDO;
import com.myblog.infrastructure.repository.persistence.mapper.ArticleVersionMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 文章版本历史 MyBatis 仓储实现。
 *
 * @author Codex
 * @since 1.0.0
 */
@Repository
public class MyBatisArticleVersionRepository implements ArticleVersionRepository {

    private final ArticleVersionMapper mapper;

    public MyBatisArticleVersionRepository(ArticleVersionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<ArticleVersionDO> findByArticleId(Long articleId) {
        return mapper.selectByArticleId(articleId);
    }

    @Override
    public Optional<ArticleVersionDO> findByArticleIdAndVersionNo(Long articleId, Integer versionNo) {
        return Optional.ofNullable(mapper.selectByArticleIdAndVersionNo(articleId, versionNo));
    }

    @Override
    public int findMaxVersionNo(Long articleId) {
        Integer max = mapper.selectMaxVersionNo(articleId);
        return max == null ? 0 : max;
    }

    @Override
    public void save(ArticleVersionDO version) {
        mapper.insert(version);
    }

    @Override
    public void deleteOldestVersions(Long articleId, int keepCount) {
        if (keepCount <= 0) {
            return;
        }
        mapper.deleteOldest(articleId, keepCount);
    }

    @Override
    public int countByArticleId(Long articleId) {
        return mapper.countByArticleId(articleId);
    }
}

