package com.myblog.domain.repository;

import com.myblog.infrastructure.repository.persistence.entity.ArticleVersionDO;

import java.util.List;
import java.util.Optional;

/**
 * 文章版本历史仓储接口。
 *
 * @author my-blog
 * @since 1.0.0
 */
public interface ArticleVersionRepository {

    /**
     * 查询文章版本列表（按版本号降序）。
     *
     * @param articleId 文章 ID
     * @return 版本列表
     */
    List<ArticleVersionDO> findByArticleId(Long articleId);

    /**
     * 查询指定版本详情。
     *
     * @param articleId 文章 ID
     * @param versionNo 版本号
     * @return 版本详情
     */
    Optional<ArticleVersionDO> findByArticleIdAndVersionNo(Long articleId, Integer versionNo);

    /**
     * 查询文章最新版本号。
     *
     * @param articleId 文章 ID
     * @return 最新版本号，不存在时返回 0
     */
    int findMaxVersionNo(Long articleId);

    /**
     * 保存版本快照。
     *
     * @param version 版本数据对象
     */
    void save(ArticleVersionDO version);

    /**
     * 删除最旧的版本（保留最近 N 条）。
     *
     * @param articleId 文章 ID
     * @param keepCount 保留数量
     */
    void deleteOldestVersions(Long articleId, int keepCount);

    /**
     * 统计文章版本数量。
     *
     * @param articleId 文章 ID
     * @return 版本数量
     */
    int countByArticleId(Long articleId);
}

