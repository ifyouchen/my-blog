package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.model.readmodel.ArticleVersionSnapshot;
import com.myblog.domain.repository.ArticleVersionRepository;
import com.myblog.infrastructure.repository.persistence.entity.ArticleVersionDO;
import com.myblog.infrastructure.repository.persistence.mapper.ArticleVersionMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 文章版本历史 MyBatis 仓储实现。
 *
 * @author Codex
 * @since 1.0.0
 */
@Repository
public class MyBatisArticleVersionRepository implements ArticleVersionRepository {

    private final ArticleVersionMapper mapper;

    /**
     * 创建文章版本历史 MyBatis 仓储。
     *
     * @param mapper 文章版本历史 Mapper
     */
    public MyBatisArticleVersionRepository(ArticleVersionMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 查询文章版本列表（按版本号降序）。
     *
     * @param articleId 文章 ID
     * @return 版本列表
     */
    @Override
    public List<ArticleVersionSnapshot> findByArticleId(Long articleId) {
        List<ArticleVersionDO> rows = mapper.selectByArticleId(articleId);
        List<ArticleVersionSnapshot> result = new ArrayList<ArticleVersionSnapshot>(rows.size());
        for (ArticleVersionDO row : rows) {
            result.add(toSnapshot(row));
        }
        return result;
    }

    /**
     * 查询指定版本（含 content）。
     *
     * @param articleId 文章 ID
     * @param versionNo 版本号
     * @return 版本详情 Optional
     */
    @Override
    public Optional<ArticleVersionSnapshot> findByArticleIdAndVersionNo(Long articleId, Integer versionNo) {
        return Optional.ofNullable(toSnapshot(mapper.selectByArticleIdAndVersionNo(articleId, versionNo)));
    }

    /**
     * 查询文章最新版本号，不存在时返回 0。
     *
     * @param articleId 文章 ID
     * @return 最新版本号
     */
    @Override
    public int findMaxVersionNo(Long articleId) {
        Integer max = mapper.selectMaxVersionNo(articleId);
        return max == null ? 0 : max;
    }

    /**
     * 插入版本快照。
     *
     * @param version 版本数据对象
     */
    @Override
    public void save(ArticleVersionSnapshot version) {
        mapper.insert(toDO(version));
    }

    /**
     * 删除最旧版本（保留最近 keepCount 条）。
     *
     * @param articleId 文章 ID
     * @param keepCount 保留数量
     */
    @Override
    public void deleteOldestVersions(Long articleId, int keepCount) {
        if (keepCount <= 0) {
            return;
        }
        mapper.deleteOldest(articleId, keepCount);
    }

    /**
     * 统计文章版本数量。
     *
     * @param articleId 文章 ID
     * @return 版本数量
     */
    @Override
    public int countByArticleId(Long articleId) {
        return mapper.countByArticleId(articleId);
    }

    /**
     * 转换为文章版本快照。
     *
     * @param row 持久化行
     * @return 文章版本快照
     */
    private ArticleVersionSnapshot toSnapshot(ArticleVersionDO row) {
        if (row == null) {
            return null;
        }
        ArticleVersionSnapshot snapshot = new ArticleVersionSnapshot();
        snapshot.setId(row.getId());
        snapshot.setArticleId(row.getArticleId());
        snapshot.setVersionNo(row.getVersionNo());
        snapshot.setTitle(row.getTitle());
        snapshot.setContent(row.getContent());
        snapshot.setSummary(row.getSummary());
        snapshot.setSavedBy(row.getSavedBy());
        snapshot.setCreatedAt(row.getCreatedAt());
        return snapshot;
    }

    /**
     * 转换为持久化行。
     *
     * @param snapshot 文章版本快照
     * @return 持久化行
     */
    private ArticleVersionDO toDO(ArticleVersionSnapshot snapshot) {
        ArticleVersionDO row = new ArticleVersionDO();
        row.setId(snapshot.getId());
        row.setArticleId(snapshot.getArticleId());
        row.setVersionNo(snapshot.getVersionNo());
        row.setTitle(snapshot.getTitle());
        row.setContent(snapshot.getContent());
        row.setSummary(snapshot.getSummary());
        row.setSavedBy(snapshot.getSavedBy());
        row.setCreatedAt(snapshot.getCreatedAt());
        return row;
    }
}

