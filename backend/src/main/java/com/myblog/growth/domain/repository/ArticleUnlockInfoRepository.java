package com.myblog.growth.domain.repository;

import com.myblog.growth.domain.model.valueobject.ArticleUnlockInfo;

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
}
