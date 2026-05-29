package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.ColumnDO;
import com.myblog.infrastructure.repository.persistence.entity.LearningPathArticleDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 专栏 Mapper。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface ColumnMapper {

    /**
     * 根据 ID 查询专栏。
     *
     * @param id 专栏 ID
     * @return 专栏数据对象
     */
    ColumnDO selectById(@Param("id") Long id);

    /**
     * 分页查询已发布专栏。
     *
     * @param offset   偏移量
     * @param pageSize 每页大小
     * @return 专栏列表
     */
    List<ColumnDO> selectPublished(@Param("offset") int offset, @Param("pageSize") int pageSize);

    /**
     * 统计已发布专栏数量。
     *
     * @return 已发布专栏数量
     */
    long countPublished();

    /**
     * 根据作者 ID 分页查询已发布专栏。
     *
     * @param authorId 作者 ID
     * @param offset   偏移量
     * @param pageSize 每页大小
     * @return 专栏列表
     */
    List<ColumnDO> selectPublishedByAuthorId(@Param("authorId") Long authorId,
                                             @Param("offset") int offset,
                                             @Param("pageSize") int pageSize);

    /**
     * 统计作者已发布专栏数量。
     *
     * @param authorId 作者 ID
     * @return 专栏数量
     */
    long countPublishedByAuthorId(@Param("authorId") Long authorId);

    /**
     * 查询推荐专栏。
     *
     * @param limit 返回数量限制
     * @return 推荐专栏列表
     */
    List<ColumnDO> selectRecommended(@Param("limit") int limit);

    /**
     * 查询下一个专栏 ID。
     *
     * @return 下一个专栏 ID
     */
    Long selectNextId();

    /**
     * 根据 ID 统计专栏数量。
     *
     * @param id 专栏 ID
     * @return 专栏数量
     */
    int countById(@Param("id") Long id);

    /**
     * 插入专栏。
     *
     * @param columnDO 专栏数据对象
     * @return 影响行数
     */
    int insert(ColumnDO columnDO);

    /**
     * 插入或更新专栏（INSERT ... ON DUPLICATE KEY UPDATE）。
     *
     * @param columnDO 专栏数据对象
     * @return 影响行数
     */
    int insertOrUpdate(ColumnDO columnDO);

    /**
     * 更新专栏。
     *
     * @param columnDO 专栏数据对象
     * @return 影响行数
     */
    int update(ColumnDO columnDO);

    /**
     * 查询专栏下的文章 ID 列表。
     *
     * @param columnId 专栏 ID
     * @return 文章 ID 列表
     */
    List<Long> selectArticleIds(@Param("columnId") Long columnId);

    /**
     * 查询专栏下文章关联关系（含学习路径信息）。
     *
     * @param columnId 专栏 ID
     * @return 学习路径文章列表
     */
    List<LearningPathArticleDO> selectArticleRelations(@Param("columnId") Long columnId);

    /**
     * 统计专栏文章关联数量。
     *
     * @param columnId  专栏 ID
     * @param articleId 文章 ID
     * @return 关联数量
     */
    int countColumnArticle(@Param("columnId") Long columnId, @Param("articleId") Long articleId);

    /**
     * 恢复专栏文章关联（重新绑定已被软删除的关联）。
     *
     * @param columnId  专栏 ID
     * @param articleId 文章 ID
     * @param sortOrder 排序
     * @return 影响行数
     */
    int restoreColumnArticle(@Param("columnId") Long columnId, @Param("articleId") Long articleId,
                             @Param("sortOrder") int sortOrder);

    /**
     * 新增专栏文章关联。
     *
     * @param columnId  专栏 ID
     * @param articleId 文章 ID
     * @param sortOrder 排序
     * @return 影响行数
     */
    int insertColumnArticle(@Param("columnId") Long columnId, @Param("articleId") Long articleId,
                            @Param("sortOrder") int sortOrder);

    /**
     * 将专栏的 article_count +1（绑定文章时维护冗余字段）。
     *
     * @param columnId 专栏 ID
     * @return 影响行数
     */
    int incrementArticleCount(@Param("columnId") Long columnId);

    /**
     * 将专栏的 article_count -1（移除文章时维护冗余字段）。
     *
     * @param columnId 专栏 ID
     * @return 影响行数
     */
    int decrementArticleCount(@Param("columnId") Long columnId);

    /**
     * 删除专栏文章关联。
     *
     * @param columnId  专栏 ID
     * @param articleId 文章 ID
     * @return 影响行数
     */
    int deleteColumnArticle(@Param("columnId") Long columnId, @Param("articleId") Long articleId);

    /**
     * 搜索已发布专栏。
     *
     * @param keyword 关键字
     * @param sort    排序方式
     * @param offset  偏移量
     * @param limit   限制数量
     * @return 专栏列表
     */
    List<ColumnDO> searchPublished(@Param("keyword") String keyword, @Param("sort") String sort,
                                   @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 统计已发布专栏搜索数量。
     *
     * @param keyword 关键字
     * @return 专栏数量
     */
    long countSearchPublished(@Param("keyword") String keyword);

    /**
     * 后台管理：分页查询所有专栏（不含软删除）。
     *
     * @param keyword 关键字
     * @param status 状态筛选
     * @param offset  偏移量
     * @param limit   限制数量
     * @return 专栏列表
     */
    List<ColumnDO> selectAll(@Param("keyword") String keyword,
                             @Param("status") String status,
                             @Param("offset") int offset,
                             @Param("limit") int limit);

    /**
     * 统计所有专栏数量。
     *
     * @param keyword 关键字
     * @param status 状态筛选
     * @return 专栏数量
     */
    long countAll(@Param("keyword") String keyword, @Param("status") String status);

    /**
     * 后台管理：软删除专栏。
     *
     * @param id 专栏 ID
     * @return 影响行数
     */
    int softDelete(@Param("id") Long id);

    /**
     * 创作者：查询自己的专栏列表（不含已删除）。
     *
     * @param authorId 作者 ID
     * @return 专栏列表
     */
    List<ColumnDO> selectByAuthorId(@Param("authorId") Long authorId);

    /**
     * 创作者：统计自己的专栏数量（不含已删除）。
     *
     * @param authorId 作者 ID
     * @return 专栏数量
     */
    int countByAuthorId(@Param("authorId") Long authorId);
}
