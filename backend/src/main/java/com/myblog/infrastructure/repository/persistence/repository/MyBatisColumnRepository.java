package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.model.aggregate.Column;
import com.myblog.domain.model.valueobject.ColumnId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ColumnRepository;
import com.myblog.infrastructure.repository.persistence.entity.ColumnDO;
import com.myblog.infrastructure.repository.persistence.mapper.ColumnMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 专栏 MyBatis 仓储实现。
 *
 * @author Codex
 * @since 1.0.0
 */
@Repository
public class MyBatisColumnRepository implements ColumnRepository {

    private final ColumnMapper columnMapper;

    public MyBatisColumnRepository(ColumnMapper columnMapper) {
        this.columnMapper = columnMapper;
    }

    @Override
    public Optional<Column> findById(ColumnId columnId) {
        ColumnDO columnDO = columnMapper.selectById(columnId.getValue());
        return columnDO == null ? Optional.<Column>empty() : Optional.of(toDomain(columnDO));
    }

    @Override
    public List<Column> findPublished(int page, int pageSize) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        List<ColumnDO> columnDOList = columnMapper.selectPublished((currentPage - 1) * currentPageSize, currentPageSize);
        List<Column> columns = new ArrayList<Column>(columnDOList.size());
        for (ColumnDO columnDO : columnDOList) {
            columns.add(toDomain(columnDO));
        }
        return columns;
    }

    @Override
    public long countPublished() {
        return columnMapper.countPublished();
    }

    @Override
    public List<Column> findRecommended(int limit) {
        List<ColumnDO> columnDOList = columnMapper.selectRecommended(limit);
        List<Column> columns = new ArrayList<Column>(columnDOList.size());
        for (ColumnDO columnDO : columnDOList) {
            columns.add(toDomain(columnDO));
        }
        return columns;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Column save(Column column) {
        ColumnDO columnDO = toData(column);
        if (columnMapper.countById(column.getId().getValue()) > 0) {
            columnMapper.update(columnDO);
        } else {
            columnMapper.insert(columnDO);
        }
        return column;
    }

    @Override
    public Long nextId() {
        Long nextId = columnMapper.selectNextId();
        return nextId == null ? 2000L : nextId;
    }

    @Override
    public List<Long> findArticleIds(ColumnId columnId) {
        return columnMapper.selectArticleIds(columnId.getValue());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindArticle(ColumnId columnId, Long articleId, int sortOrder) {
        if (columnMapper.countColumnArticle(columnId.getValue(), articleId) > 0) {
            columnMapper.restoreColumnArticle(columnId.getValue(), articleId, sortOrder);
        } else {
            columnMapper.insertColumnArticle(columnId.getValue(), articleId, sortOrder);
        }
    }

    @Override
    public List<Column> searchPublished(String keyword, String sort, int page, int pageSize) {
        int offset = (Math.max(page, 1) - 1) * Math.max(pageSize, 1);
        List<ColumnDO> columnDOList = columnMapper.searchPublished(keyword, sort, offset, pageSize);
        List<Column> columns = new ArrayList<>(columnDOList.size());
        for (ColumnDO columnDO : columnDOList) {
            columns.add(toDomain(columnDO));
        }
        return columns;
    }

    @Override
    public long countSearchPublished(String keyword) {
        return columnMapper.countSearchPublished(keyword);
    }

    private Column toDomain(ColumnDO columnDO) {
        return Column.restore(
            columnDO.getId(),
            new UserId(columnDO.getAuthorId()),
            columnDO.getTitle(),
            columnDO.getSummary(),
            columnDO.getCoverUrl(),
            columnDO.getStatus(),
            columnDO.getSortOrder(),
            columnDO.getSubscriberCount(),
            columnDO.getArticleCount(),
            columnDO.getCreatedAt(),
            columnDO.getUpdatedAt(),
            columnDO.getDeletedAt(),
            columnDO.getVersion()
        );
    }

    private ColumnDO toData(Column column) {
        ColumnDO columnDO = new ColumnDO();
        columnDO.setId(column.getId().getValue());
        columnDO.setAuthorId(column.getAuthorId().getValue());
        columnDO.setTitle(column.getTitle());
        columnDO.setSummary(column.getSummary());
        columnDO.setCoverUrl(column.getCoverUrl());
        columnDO.setStatus(column.getStatus());
        columnDO.setSortOrder(column.getSortOrder());
        columnDO.setSubscriberCount(column.getSubscriberCount());
        columnDO.setArticleCount(column.getArticleCount());
        columnDO.setCreatedAt(column.getCreatedAt());
        columnDO.setUpdatedAt(column.getUpdatedAt());
        columnDO.setDeletedAt(column.getDeletedAt());
        columnDO.setVersion(column.getVersion());
        return columnDO;
    }
}
