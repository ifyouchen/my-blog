package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.model.aggregate.ArticleFavorite;
import com.myblog.domain.model.valueobject.ArticleFavoriteId;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleFavoriteRepository;
import com.myblog.infrastructure.repository.persistence.converter.ArticleFavoritePersistenceConverter;
import com.myblog.infrastructure.repository.persistence.entity.ArticleFavoriteDO;
import com.myblog.infrastructure.config.SnowflakeIdGenerator;
import com.myblog.infrastructure.repository.persistence.mapper.ArticleFavoriteMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 文章收藏 MyBatis 仓储实现。
 *
 * @author Codex
 * @since 1.0.0
 */
@Repository
@Profile("!memory")
public class MyBatisArticleFavoriteRepository implements ArticleFavoriteRepository {

    private final ArticleFavoriteMapper articleFavoriteMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public MyBatisArticleFavoriteRepository(ArticleFavoriteMapper articleFavoriteMapper,
                                            SnowflakeIdGenerator snowflakeIdGenerator) {
        this.articleFavoriteMapper = articleFavoriteMapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    @Override
    public Optional<ArticleFavorite> findByArticleAndUser(ArticleId articleId, UserId userId) {
        ArticleFavoriteDO favoriteDO = articleFavoriteMapper.selectByArticleAndUser(
            articleId.getValue(), userId.getValue()
        );
        if (favoriteDO == null) {
            return Optional.empty();
        }
        return Optional.of(ArticleFavoritePersistenceConverter.toDomain(favoriteDO));
    }

    @Override
    public Optional<ArticleFavorite> findByArticleAndUserIncludingDeleted(ArticleId articleId, UserId userId) {
        ArticleFavoriteDO favoriteDO = articleFavoriteMapper.selectByArticleAndUserIncludingDeleted(
            articleId.getValue(), userId.getValue()
        );
        if (favoriteDO == null) {
            return Optional.empty();
        }
        return Optional.of(ArticleFavoritePersistenceConverter.toDomain(favoriteDO));
    }

    @Override
    public boolean exists(ArticleId articleId, UserId userId) {
        return articleFavoriteMapper.countByArticleAndUser(
            articleId.getValue(), userId.getValue()
        ) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleFavorite save(ArticleFavorite favorite) {
        ArticleFavoriteDO favoriteDO = ArticleFavoritePersistenceConverter.toData(favorite);
        articleFavoriteMapper.insertOrUpdate(favoriteDO);
        return favorite;
    }

    @Override
    public Long nextId() {
        return snowflakeIdGenerator.nextId();
    }

    @Override
    public List<ArticleFavorite> findByUserId(UserId userId, int page, int pageSize) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        int offset = (currentPage - 1) * currentPageSize;
        List<ArticleFavoriteDO> favoriteDOList = articleFavoriteMapper.selectByUserId(
            userId.getValue(), offset, currentPageSize
        );
        List<ArticleFavorite> favorites = new ArrayList<ArticleFavorite>(favoriteDOList.size());
        for (ArticleFavoriteDO favoriteDO : favoriteDOList) {
            favorites.add(ArticleFavoritePersistenceConverter.toDomain(favoriteDO));
        }
        return favorites;
    }

    @Override
    public int countByUserId(UserId userId) {
        return articleFavoriteMapper.countByUserId(userId.getValue());
    }

    @Override
    public Set<Long> findFavoritedArticleIdsByUser(List<Long> articleIds, UserId userId) {
        if (articleIds == null || articleIds.isEmpty()) {
            return new HashSet<Long>();
        }
        List<Long> favoritedIds = articleFavoriteMapper.selectFavoritedArticleIdsByUser(
            articleIds, userId.getValue()
        );
        return new HashSet<Long>(favoritedIds);
    }
}
