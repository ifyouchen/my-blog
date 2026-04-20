package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.infrastructure.repository.persistence.converter.ArticlePersistenceConverter;
import com.myblog.infrastructure.repository.persistence.entity.ArticleDO;
import com.myblog.infrastructure.repository.persistence.mapper.ArticleMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 文章 MyBatis 仓储实现。
 *
 * @author Codex
 * @since 1.0.0
 */
@Repository
@Profile("!memory")
public class MyBatisArticleRepository implements ArticleRepository {

    private final ArticleMapper articleMapper;

    /**
     * 创建文章 MyBatis 仓储。
     *
     * @param articleMapper 文章 Mapper
     */
    public MyBatisArticleRepository(ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    /**
     * 根据文章 ID 查询文章。
     *
     * @param articleId 文章 ID
     * @return 文章 Optional
     */
    @Override
    public Optional<Article> findById(ArticleId articleId) {
        ArticleDO articleDO = articleMapper.selectById(articleId.getValue());
        if (articleDO == null) {
            return Optional.empty();
        }
        List<String> tags = articleMapper.selectTagNamesByArticleId(articleId.getValue());
        return Optional.of(ArticlePersistenceConverter.toDomain(articleDO, tags));
    }

    /**
     * 查询已发布文章。
     *
     * @param keyword 关键字
     * @param category 分类
     * @param tag 标签
     * @return 已发布文章列表
     */
    @Override
    public List<Article> findPublished(String keyword, String category, String tag) {
        List<ArticleDO> articleDOList = articleMapper.selectPublished(keyword, category, tag);
        List<Article> articles = new ArrayList<Article>(articleDOList.size());
        for (ArticleDO articleDO : articleDOList) {
            List<String> tags = articleMapper.selectTagNamesByArticleId(articleDO.getId());
            articles.add(ArticlePersistenceConverter.toDomain(articleDO, tags));
        }
        return articles;
    }

    /**
     * 保存文章。
     *
     * @param article 文章聚合根
     * @return 保存后的文章
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Article save(Article article) {
        ArticleDO articleDO = ArticlePersistenceConverter.toData(article);
        if (articleMapper.countById(article.getId().getValue()) > 0) {
            articleMapper.update(articleDO);
        } else {
            articleMapper.insert(articleDO);
        }
        saveTags(article);
        return article;
    }

    @Override
    public List<Article> findAll() {
        List<ArticleDO> articleDOList = articleMapper.selectAll();
        List<Article> articles = new ArrayList<>(articleDOList.size());
        for (ArticleDO articleDO : articleDOList) {
            List<String> tags = articleMapper.selectTagNamesByArticleId(articleDO.getId());
            articles.add(ArticlePersistenceConverter.toDomain(articleDO, tags));
        }
        return articles;
    }

    /**
     * 生成下一个文章 ID。
     *
     * @return 文章 ID
     */
    @Override
    public Long nextId() {
        Long nextId = articleMapper.selectNextId();
        return nextId == null ? 101L : nextId;
    }

    private void saveTags(Article article) {
        articleMapper.logicDeleteTagsByArticleId(article.getId().getValue());
        for (String tag : article.getTags()) {
            if (tag == null || tag.trim().length() == 0) {
                continue;
            }
            saveTag(article.getId().getValue(), tag.trim());
        }
    }

    private void saveTag(Long articleId, String tagName) {
        if (articleMapper.countTag(articleId, tagName) > 0) {
            articleMapper.restoreTag(articleId, tagName);
            return;
        }
        articleMapper.insertTag(articleId, tagName);
    }
}
