package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.Column;
import com.myblog.domain.model.valueobject.ColumnId;
import com.myblog.domain.model.valueobject.LearningPathArticle;

import java.util.List;
import java.util.Optional;

/**
 * 专栏仓储接口。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface ColumnRepository {

    /**
     * 根据专栏 ID 查询专栏。
     *
     * @param columnId 专栏 ID
     * @return 专栏 Optional
     */
    Optional<Column> findById(ColumnId columnId);

    /**
     * 分页查询已发布专栏。
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @return 专栏列表
     */
    List<Column> findPublished(int page, int pageSize);

    /**
     * 统计已发布专栏数量。
     *
     * @return 已发布专栏数量
     */
    long countPublished();

    /**
     * 分页查询指定作者的已发布专栏。
     *
     * @param authorId 作者 ID
     * @param page     页码
     * @param pageSize 每页大小
     * @return 专栏列表
     */
    List<Column> findPublishedByAuthorId(Long authorId, int page, int pageSize);

    /**
     * 统计指定作者的已发布专栏数量。
     *
     * @param authorId 作者 ID
     * @return 已发布专栏数量
     */
    long countPublishedByAuthorId(Long authorId);

    /**
     * 查询推荐专栏。
     *
     * @param limit 返回数量限制
     * @return 推荐专栏列表
     */
    List<Column> findRecommended(int limit);

    /**
     * 保存专栏（新增或更新）。
     *
     * @param column 专栏聚合根
     * @return 保存后的专栏
     */
    Column save(Column column);

    /**
     * 生成下一个专栏 ID。
     *
     * @return 专栏 ID
     */
    Long nextId();

    /**
     * 查询专栏关联的文章 ID 列表。
     *
     * @param columnId 专栏 ID
     * @return 文章 ID 列表
     */
    List<Long> findArticleIds(ColumnId columnId);

    /**
     * 查询专栏的文章编排关系列表。
     *
     * @param columnId 专栏 ID
     * @return 学习路径文章列表
     */
    List<LearningPathArticle> findArticleRelations(ColumnId columnId);

    /**
     * 绑定文章到专栏。
     *
     * @param columnId  专栏 ID
     * @param articleId 文章 ID
     * @param sortOrder 排序权重
     */
    void bindArticle(ColumnId columnId, Long articleId, int sortOrder);

    /**
     * 从专栏解绑文章。
     *
     * @param columnId  专栏 ID
     * @param articleId 文章 ID
     */
    void unbindArticle(ColumnId columnId, Long articleId);

    /**
     * 搜索已发布专栏。
     *
     * @param keyword  关键字
     * @param sort     排序方式
     * @param page     页码
     * @param pageSize 每页大小
     * @return 专栏列表
     */
    List<Column> searchPublished(String keyword, String sort, int page, int pageSize);

    /**
     * 统计搜索已发布专栏数量。
     *
     * @param keyword 关键字
     * @return 专栏数量
     */
    long countSearchPublished(String keyword);

    /**
     * 后台管理：分页查询所有专栏（含草稿、已删除除外）。
     *
     * @param keyword  关键字
     * @param page     页码
     * @param pageSize 每页大小
     * @return 专栏列表
     */
    List<Column> findAll(String keyword, int page, int pageSize);

    /**
     * 统计后台专栏数量。
     *
     * @param keyword 关键字
     * @return 专栏数量
     */
    long countAll(String keyword);

    /**
     * 后台管理：软删除专栏。
     *
     * @param columnId 专栏 ID
     */
    void softDelete(ColumnId columnId);

    /**
     * 创作者：查询自己的专栏列表（不含已删除）。
     *
     * @param authorId 作者 ID
     * @return 专栏列表
     */
    List<Column> findByAuthorId(Long authorId);

    /**
     * 创作者：统计自己的专栏数量（不含已删除）。
     *
     * @param authorId 作者 ID
     * @return 专栏数量
     */
    int countByAuthorId(Long authorId);
}
