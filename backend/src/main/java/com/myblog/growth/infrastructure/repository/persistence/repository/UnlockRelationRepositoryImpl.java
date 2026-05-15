package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.myblog.growth.domain.repository.UnlockRelationRepository;
import com.myblog.growth.infrastructure.repository.persistence.mapper.ArticleUnlockRelationMapper;
import org.springframework.stereotype.Repository;

/**
 * 文章解锁关系 Repository 实现.
 */
@Repository
public class UnlockRelationRepositoryImpl implements UnlockRelationRepository {

    private final ArticleUnlockRelationMapper mapper;

    /**
     * 构造注入 Mapper.
     *
     * @param mapper 文章解锁关系 Mapper
     */
    public UnlockRelationRepositoryImpl(ArticleUnlockRelationMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByUserIdAndArticleId(Long userId, Long articleId) {
        return mapper.countByUserIdAndArticleId(userId, articleId) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int insertIgnore(Long userId, Long articleId, String orderNo) {
        return mapper.insertIgnore(userId, articleId, orderNo);
    }
}

