package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.model.aggregate.ArticleFavorite;
import com.myblog.domain.model.valueobject.ArticleFavoriteId;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleFavoriteRepository;
import com.myblog.infrastructure.repository.persistence.converter.ArticleFavoritePersistenceConverter;
import com.myblog.infrastructure.repository.persistence.entity.ArticleFavoriteDO;
import com.myblog.infrastructure.repository.persistence.mapper.ArticleFavoriteMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    public MyBatisArticleFavoriteRepository(ArticleFavoriteMapper articleFavoriteMapper) {
        this.articleFavoriteMapper = articleFavoriteMapper;
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
        if (articleFavoriteMapper.countById(favorite.getId().getValue()) > 0) {
            articleFavoriteMapper.update(favoriteDO);
        } else {
            articleFavoriteMapper.insert(favoriteDO);
        }
        return favorite;
    }

    @Override
    public Long nextId() {
        Long nextId = articleFavoriteMapper.selectNextId();
        return nextId == null ? 101L : nextId;
    }

    @Override
    public List<ArticleFavorite> findByUserId(UserId userId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<ArticleFavoriteDO> favoriteDOList = articleFavoriteMapper.selectByUserId(
            userId.getValue(), offset, pageSize
        );
        List<ArticleFavorite> favorites = new ArrayList<>(favoriteDOList.size());
        for (ArticleFavoriteDO favoriteDO : favoriteDOList) {
            favorites.add(ArticleFavoritePersistenceConverter.toDomain(favoriteDO));
        }
        return favorites;
    }

    /**
     * 统计用户收藏数量。
     *
     * @param userId 用户 ID
     * @return 收藏数量
     */
    @Override
    public int countByUserId(UserId userId) {
        return articleFavoriteMapper.countByUserId(userId.getValue());
    }
}
