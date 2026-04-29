package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.Column;
import com.myblog.domain.model.valueobject.ColumnId;

import java.util.List;
import java.util.Optional;

/**
 * 专栏仓储接口。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface ColumnRepository {

    Optional<Column> findById(ColumnId columnId);

    List<Column> findPublished(int page, int pageSize);

    long countPublished();

    List<Column> findRecommended(int limit);

    Column save(Column column);

    Long nextId();

    List<Long> findArticleIds(ColumnId columnId);

    void bindArticle(ColumnId columnId, Long articleId, int sortOrder);

    List<Column> searchPublished(String keyword, String sort, int page, int pageSize);

    long countSearchPublished(String keyword);

    /** 后台管理：分页查询所有专栏（含草稿、已删除除外） */
    List<Column> findAll(String keyword, int page, int pageSize);

    long countAll(String keyword);

    /** 后台管理：软删除专栏 */
    void softDelete(ColumnId columnId);
}
