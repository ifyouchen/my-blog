package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.model.aggregate.Column;
import com.myblog.domain.model.valueobject.ColumnId;
import com.myblog.domain.model.valueobject.LearningPathArticle;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ColumnRepository;
import com.myblog.infrastructure.repository.persistence.entity.ColumnDO;
import com.myblog.infrastructure.repository.persistence.entity.LearningPathArticleDO;
import com.myblog.infrastructure.repository.persistence.mapper.ColumnMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final IdGenerator idGenerator;

    /**
     * 创建专栏 MyBatis 仓储。
     *
     * @param columnMapper 专栏 Mapper
     * @param idGenerator 全局 ID 生成器
     */
    public MyBatisColumnRepository(ColumnMapper columnMapper, IdGenerator idGenerator) {
        this.columnMapper = columnMapper;
        this.idGenerator = idGenerator;
    }

    /**
     * 根据专栏 ID 查询专栏。
     *
     * @param columnId 专栏 ID
     * @return 专栏 Optional
     */
    @Override
    public Optional<Column> findById(ColumnId columnId) {
        ColumnDO columnDO = columnMapper.selectById(columnId.getValue());
        return columnDO == null ? Optional.<Column>empty() : Optional.of(toDomain(columnDO));
    }

    /**
     * 分页查询已发布专栏。
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @return 专栏列表
     */
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

    /**
     * 统计已发布专栏数量。
     *
     * @return 已发布专栏数量
     */
    @Override
    public long countPublished() {
        return columnMapper.countPublished();
    }

    /**
     * 查询推荐专栏列表。
     *
     * @param limit 返回数量限制
     * @return 推荐专栏列表
     */
    @Override
    public List<Column> findPublishedByAuthorId(Long authorId, int page, int pageSize) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        List<ColumnDO> list = columnMapper.selectPublishedByAuthorId(
            authorId,
            (currentPage - 1) * currentPageSize,
            currentPageSize
        );
        List<Column> columns = new ArrayList<Column>(list.size());
        for (ColumnDO columnDO : list) {
            columns.add(toDomain(columnDO));
        }
        return columns;
    }

    @Override
    public long countPublishedByAuthorId(Long authorId) {
        return columnMapper.countPublishedByAuthorId(authorId);
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

    /**
     * 保存专栏。
     *
     * @param column 专栏聚合根
     * @return 保存后的专栏
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Column save(Column column) {
        ColumnDO columnDO = toData(column);
        columnMapper.insertOrUpdate(columnDO);
        return column;
    }

    /**
     * 生成下一个专栏 ID。
     *
     * @return 专栏 ID
     */
    @Override
    public Long nextId() {
        return idGenerator.nextId("blog_column");
    }

    /**
     * 查询专栏下的文章 ID 列表。
     *
     * @param columnId 专栏 ID
     * @return 文章 ID 列表
     */
    @Override
    public List<Long> findArticleIds(ColumnId columnId) {
        return columnMapper.selectArticleIds(columnId.getValue());
    }

    /**
     * 查询专栏下文章关联关系（含学习路径信息）。
     *
     * @param columnId 专栏 ID
     * @return 学习路径文章列表
     */
    @Override
    public List<LearningPathArticle> findArticleRelations(ColumnId columnId) {
        List<LearningPathArticleDO> list = columnMapper.selectArticleRelations(columnId.getValue());
        List<LearningPathArticle> result = new ArrayList<LearningPathArticle>(list.size());
        for (LearningPathArticleDO item : list) {
            result.add(toPathArticle(item));
        }
        return result;
    }

    /**
     * 绑定文章到专栏（已存在时恢复）。
     *
     * @param columnId  专栏 ID
     * @param articleId 文章 ID
     * @param sortOrder 排序
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindArticle(ColumnId columnId, Long articleId, int sortOrder) {
        if (columnMapper.countColumnArticle(columnId.getValue(), articleId) > 0) {
            if (columnMapper.countActiveColumnArticle(columnId.getValue(), articleId) > 0) {
                columnMapper.updateColumnArticleSort(columnId.getValue(), articleId, sortOrder);
            } else if (columnMapper.restoreColumnArticle(columnId.getValue(), articleId, sortOrder) > 0) {
                columnMapper.incrementArticleCount(columnId.getValue());
            }
        } else {
            columnMapper.insertColumnArticle(columnId.getValue(), articleId, sortOrder);
            // 维护冗余的 article_count 字段
            columnMapper.incrementArticleCount(columnId.getValue());
        }
    }

    /**
     * 从专栏解绑文章。
     *
     * @param columnId  专栏 ID
     * @param articleId 文章 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindArticle(ColumnId columnId, Long articleId) {
        columnMapper.deleteColumnArticle(columnId.getValue(), articleId);
        columnMapper.decrementArticleCount(columnId.getValue());
    }

    /**
     * 搜索已发布专栏。
     *
     * @param keyword  关键字
     * @param sort     排序方式
     * @param page     页码
     * @param pageSize 每页大小
     * @return 专栏列表
     */
    @Override
    public List<Column> searchPublished(String keyword, String sort, int page, int pageSize) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        int offset = (currentPage - 1) * currentPageSize;
        List<ColumnDO> columnDOList = columnMapper.searchPublished(keyword, sort, offset, currentPageSize);
        List<Column> columns = new ArrayList<Column>(columnDOList.size());
        for (ColumnDO columnDO : columnDOList) {
            columns.add(toDomain(columnDO));
        }
        return columns;
    }

    /**
     * 统计已发布专栏搜索数量。
     *
     * @param keyword 关键字
     * @return 专栏数量
     */
    @Override
    public long countSearchPublished(String keyword) {
        return columnMapper.countSearchPublished(keyword);
    }

    /**
     * 后台管理分页查询所有专栏。
     *
     * @param keyword  关键字
     * @param status   状态筛选
     * @param page     页码
     * @param pageSize 每页大小
     * @return 专栏列表
     */
    @Override
    public List<Column> findAll(String keyword, String status, int page, int pageSize) {
        int p = Math.max(page, 1);
        int ps = Math.max(pageSize, 1);
        List<ColumnDO> list = columnMapper.selectAll(keyword, status, (p - 1) * ps, ps);
        List<Column> cols = new ArrayList<Column>(list.size());
        for (ColumnDO d : list) { cols.add(toDomain(d)); }
        return cols;
    }

    /**
     * 统计所有专栏数量。
     *
     * @param keyword 关键字
     * @param status 状态筛选
     * @return 专栏数量
     */
    @Override
    public long countAll(String keyword, String status) {
        return columnMapper.countAll(keyword, status);
    }

    /**
     * 软删除专栏。
     *
     * @param columnId 专栏 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void softDelete(ColumnId columnId) {
        columnMapper.softDelete(columnId.getValue());
    }

    /**
     * 查询指定作者的专栏列表。
     *
     * @param authorId 作者 ID
     * @return 专栏列表
     */
    @Override
    public List<Column> findByAuthorId(Long authorId) {
        List<ColumnDO> list = columnMapper.selectByAuthorId(authorId);
        List<Column> result = new ArrayList<>(list.size());
        for (ColumnDO d : list) {
            result.add(toDomain(d));
        }
        return result;
    }

    /**
     * 统计指定作者的专栏数量。
     *
     * @param authorId 作者 ID
     * @return 专栏数量
     */
    @Override
    public int countByAuthorId(Long authorId) {
        return columnMapper.countByAuthorId(authorId);
    }

    /**
     * 将 DO 转换为领域对象。
     *
     * @param columnDO 专栏数据对象
     * @return 专栏聚合根
     */
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
            columnDO.getIntro(),
            columnDO.getDifficulty(),
            columnDO.getEstimatedMinutes(),
            columnDO.getSourceType(),
            columnDO.getSourceNote(),
            columnDO.getRecommended(),
            columnDO.getRecommendWeight(),
            columnDO.getCreatedAt(),
            columnDO.getUpdatedAt(),
            columnDO.getDeletedAt(),
            columnDO.getVersion()
        );
    }

    /**
     * 将领域对象转换为 DO。
     *
     * @param column 专栏聚合根
     * @return 专栏数据对象
     */
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
        columnDO.setIntro(column.getIntro());
        columnDO.setDifficulty(column.getDifficulty());
        columnDO.setEstimatedMinutes(column.getEstimatedMinutes());
        columnDO.setSourceType(column.getSourceType());
        columnDO.setSourceNote(column.getSourceNote());
        columnDO.setRecommended(column.isRecommended());
        columnDO.setRecommendWeight(column.getRecommendWeight());
        columnDO.setCreatedAt(column.getCreatedAt() != null ? column.getCreatedAt() : LocalDateTime.now());
        columnDO.setUpdatedAt(LocalDateTime.now());
        columnDO.setDeletedAt(column.getDeletedAt());
        columnDO.setVersion(column.getVersion() != null ? column.getVersion() : 0);
        return columnDO;
    }

    /**
     * 将学习路径文章 DO 转换为领域值对象。
     *
     * @param item 学习路径文章数据对象
     * @return 学习路径文章值对象
     */
    private LearningPathArticle toPathArticle(LearningPathArticleDO item) {
        return new LearningPathArticle(
            item.getArticleId(),
            item.getSectionTitle(),
            item.getStepOrder() == null ? 0 : item.getStepOrder(),
            item.getRequired() == null || item.getRequired(),
            item.getEditorNote()
        );
    }
}
