package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.model.aggregate.ArticleFavorite;
import com.myblog.domain.model.valueobject.ArticleFavoriteId;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleFavoriteRepository;
import com.myblog.infrastructure.repository.persistence.converter.ArticleFavoritePersistenceConverter;
import com.myblog.infrastructure.repository.persistence.entity.ArticleFavoriteDO;
import com.myblog.infrastructure.repository.persistence.mapper.ArticleFavoriteMapper;
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
public class MyBatisArticleFavoriteRepository implements ArticleFavoriteRepository {

    private final ArticleFavoriteMapper articleFavoriteMapper;
    private final IdGenerator idGenerator;

    /**
     * 创建文章收藏 MyBatis 仓储。
     *
     * @param articleFavoriteMapper 文章收藏 Mapper
     * @param idGenerator 全局 ID 生成器
     */
    public MyBatisArticleFavoriteRepository(ArticleFavoriteMapper articleFavoriteMapper, IdGenerator idGenerator) {
        this.articleFavoriteMapper = articleFavoriteMapper;
        this.idGenerator = idGenerator;
    }

    /**
     * 查询有效的文章收藏记录（未取消）。
     *
     * @param articleId 文章 ID
     * @param userId    用户 ID
     * @return 文章收藏 Optional
     */
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

    /**
     * 查询文章收藏记录（包含已逻辑删除的记录）。
     *
     * @param articleId 文章 ID
     * @param userId    用户 ID
     * @return 文章收藏 Optional
     */
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

    /**
     * 判断用户是否已收藏文章。
     *
     * @param articleId 文章 ID
     * @param userId    用户 ID
     * @return 是否已收藏
     */
    @Override
    public boolean exists(ArticleId articleId, UserId userId) {
        return articleFavoriteMapper.countByArticleAndUser(
            articleId.getValue(), userId.getValue()
        ) > 0;
    }

    /**
     * 保存收藏记录。
     *
     * @param favorite 收藏聚合根
     * @return 保存后的收藏聚合根
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleFavorite save(ArticleFavorite favorite) {
        ArticleFavoriteDO favoriteDO = ArticleFavoritePersistenceConverter.toData(favorite);
        articleFavoriteMapper.insertOrUpdate(favoriteDO);
        return favorite;
    }

    /**
     * 生成下一个收藏记录 ID。
     *
     * @return 收藏记录 ID
     */
    @Override
    public Long nextId() {
        return idGenerator.nextId("blog_article_favorite");
    }

    /**
     * 分页查询用户的收藏列表。
     *
     * @param userId   用户 ID
     * @param page     页码
     * @param pageSize 每页大小
     * @return 收藏列表
     */
    @Override
    public List<ArticleFavorite> findByUserId(UserId userId, int page, int pageSize) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        int offset = (currentPage - 1) * currentPageSize;
        List<ArticleFavoriteDO> favoriteDOList = articleFavoriteMapper.selectByUserId(
            userId.getValue(), offset, currentPageSize
        );
        return toDomainList(favoriteDOList);
    }

    /**
     * 分页查询用户收藏的已发布文章列表。
     *
     * @param userId   用户 ID
     * @param page     页码
     * @param pageSize 每页大小
     * @return 收藏列表
     */
    @Override
    public List<ArticleFavorite> findPublishedByUserId(UserId userId, int page, int pageSize) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        int offset = (currentPage - 1) * currentPageSize;
        List<ArticleFavoriteDO> favoriteDOList = articleFavoriteMapper.selectPublishedByUserId(
            userId.getValue(), offset, currentPageSize
        );
        return toDomainList(favoriteDOList);
    }

    /**
     * 分页查询用户收藏的已发布文章列表（含关键字过滤）。
     *
     * @param userId   用户 ID
     * @param keyword  关键字
     * @param page     页码
     * @param pageSize 每页大小
     * @return 收藏列表
     */
    @Override
    public List<ArticleFavorite> findPublishedByUserIdAndKeyword(UserId userId, String keyword, int page, int pageSize) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        int offset = (currentPage - 1) * currentPageSize;
        List<ArticleFavoriteDO> favoriteDOList = articleFavoriteMapper.selectPublishedByUserIdAndKeyword(
            userId.getValue(), keyword, offset, currentPageSize
        );
        return toDomainList(favoriteDOList);
    }

    /**
     * 统计用户收藏的已发布文章数量。
     *
     * @param userId 用户 ID
     * @return 收藏数量
     */
    @Override
    public int countPublishedByUserId(UserId userId) {
        return articleFavoriteMapper.countPublishedByUserId(userId.getValue());
    }

    /**
     * 统计用户收藏的已发布文章数量（含关键字过滤）。
     *
     * @param userId  用户 ID
     * @param keyword 关键字
     * @return 收藏数量
     */
    @Override
    public int countPublishedByUserIdAndKeyword(UserId userId, String keyword) {
        return articleFavoriteMapper.countPublishedByUserIdAndKeyword(userId.getValue(), keyword);
    }

    /**
     * 将 DO 列表转换为领域对象列表。
     *
     * @param favoriteDOList 收藏 DO 列表
     * @return 收藏领域对象列表
     */
    private List<ArticleFavorite> toDomainList(List<ArticleFavoriteDO> favoriteDOList) {
        List<ArticleFavorite> favorites = new ArrayList<ArticleFavorite>(favoriteDOList.size());
        for (ArticleFavoriteDO favoriteDO : favoriteDOList) {
            favorites.add(ArticleFavoritePersistenceConverter.toDomain(favoriteDO));
        }
        return favorites;
    }

    /**
     * 统计用户的收藏总数。
     *
     * @param userId 用户 ID
     * @return 收藏总数
     */
    @Override
    public int countByUserId(UserId userId) {
        return articleFavoriteMapper.countByUserId(userId.getValue());
    }

    /**
     * 批量查询用户对多篇文章的收藏状态，返回已收藏的文章 ID 集合。
     *
     * @param articleIds 文章 ID 列表
     * @param userId     用户 ID
     * @return 已收藏的文章 ID 集合
     */
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
