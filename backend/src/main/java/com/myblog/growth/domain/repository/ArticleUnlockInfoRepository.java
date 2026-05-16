package com.myblog.growth.domain.repository;

import com.myblog.growth.domain.model.valueobject.ArticleUnlockInfo;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * 文章解锁信息查询 Repository.
 */
public interface ArticleUnlockInfoRepository {

    /**
     * 根据文章 ID 查询解锁信息.
     *
     * @param articleId 文章 ID
     * @return 解锁信息，不存在时为空
     */
    Optional<ArticleUnlockInfo> findByArticleId(Long articleId);

    /**
     * 根据文章 ID 批量查询解锁信息.
     *
     * @param articleIds 文章 ID 集合
     * @return 以文章 ID 为 key 的解锁信息 Map
     */
    Map<Long, ArticleUnlockInfo> findByArticleIds(Collection<Long> articleIds);
}
