package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.model.aggregate.ArticleLike;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleLikeRepository;
import com.myblog.infrastructure.repository.persistence.converter.ArticleLikePersistenceConverter;
import com.myblog.infrastructure.repository.persistence.entity.ArticleLikeDO;
import com.myblog.infrastructure.config.SnowflakeIdGenerator;
import com.myblog.infrastructure.repository.persistence.mapper.ArticleLikeMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 文章点赞 MyBatis 仓储实现。
 *
 * @author Codex
 * @since 1.0.0
 */
@Repository
@Profile("!memory")
public class MyBatisArticleLikeRepository implements ArticleLikeRepository {

    private final ArticleLikeMapper articleLikeMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public MyBatisArticleLikeRepository(ArticleLikeMapper articleLikeMapper,
                                        SnowflakeIdGenerator snowflakeIdGenerator) {
        this.articleLikeMapper = articleLikeMapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    /**
     * 查询当前有效的点赞记录。
     *
     * @param articleId 文章ID
     * @param userId 用户ID
     * @return 点赞聚合
     */
    @Override
    public Optional<ArticleLike> findByArticleAndUser(ArticleId articleId, UserId userId) {
        ArticleLikeDO articleLikeDO = articleLikeMapper.selectByArticleAndUser(
            articleId.getValue(), userId.getValue()
        );
        if (articleLikeDO == null) {
            return Optional.empty();
        }
        return Optional.of(ArticleLikePersistenceConverter.toDomain(articleLikeDO));
    }

    /**
     * 查询点赞记录，包含已取消的逻辑删除记录。
     *
     * @param articleId 文章ID
     * @param userId 用户ID
     * @return 点赞聚合
     */
    @Override
    public Optional<ArticleLike> findAnyByArticleAndUser(ArticleId articleId, UserId userId) {
        ArticleLikeDO articleLikeDO = articleLikeMapper.selectAnyByArticleAndUser(
            articleId.getValue(), userId.getValue()
        );
        if (articleLikeDO == null) {
            return Optional.empty();
        }
        return Optional.of(ArticleLikePersistenceConverter.toDomain(articleLikeDO));
    }

    /**
     * 判断用户是否已点赞文章。
     *
     * @param articleId 文章ID
     * @param userId 用户ID
     * @return 是否已点赞
     */
    @Override
    public boolean exists(ArticleId articleId, UserId userId) {
        return articleLikeMapper.countByArticleAndUser(
            articleId.getValue(), userId.getValue()
        ) > 0;
    }

    /**
     * 保存点赞聚合，新记录插入，已有记录更新。
     *
     * @param articleLike 点赞聚合
     * @return 保存后的点赞聚合
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleLike save(ArticleLike articleLike) {
        ArticleLikeDO articleLikeDO = ArticleLikePersistenceConverter.toData(articleLike);
        articleLikeMapper.insertOrUpdate(articleLikeDO);

        return articleLike;
    }

    /**
     * 生成下一条点赞记录ID。
     *
     * @return 点赞记录ID
     */
    @Override
    public Long nextId() {
        return snowflakeIdGenerator.nextId();
    }

    /**
     * 批量查询当前用户对多篇文章的点赞状态。
     *
     * @param articleIds 文章ID列表
     * @param userId 用户ID
     * @return 已点赞的文章ID集合
     */
    @Override
    public Set<Long> findLikedArticleIdsByUser(List<Long> articleIds, UserId userId) {
        if (articleIds == null || articleIds.isEmpty()) {
            return new HashSet<Long>();
        }
        List<Long> likedIds = articleLikeMapper.selectLikedArticleIdsByUser(
            articleIds, userId.getValue()
        );
        return new HashSet<Long>(likedIds);
    }
}
