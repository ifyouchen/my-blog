package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.valueobject.ArticleId;

import java.util.List;
import java.util.Optional;

/**
 * 文章仓储接口。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface ArticleRepository {

    /**
     * 根据文章 ID 查询文章。
     *
     * @param articleId 文章 ID
     * @return 文章 Optional
     */
    Optional<Article> findById(ArticleId articleId);

    /**
     * 查询已发布文章。
     *
     * @param keyword 关键字
     * @param category 分类
     * @param tag 标签
     * @return 已发布文章列表
     */
    List<Article> findPublished(String keyword, String category, String tag);

    /**
     * 保存文章。
     *
     * @param article 文章聚合根
     * @return 保存后的文章
     */
    Article save(Article article);

    /**
     * 生成下一个文章 ID。
     *
     * @return 文章 ID
     */
    Long nextId();
}
