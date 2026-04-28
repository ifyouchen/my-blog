package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.ColumnDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 专栏 Mapper。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface ColumnMapper {

    ColumnDO selectById(@Param("id") Long id);

    List<ColumnDO> selectPublished(@Param("offset") int offset, @Param("pageSize") int pageSize);

    long countPublished();

    List<ColumnDO> selectRecommended(@Param("limit") int limit);

    Long selectNextId();

    int countById(@Param("id") Long id);

    int insert(ColumnDO columnDO);
    int insertOrUpdate(ColumnDO columnDO);


    int update(ColumnDO columnDO);

    List<Long> selectArticleIds(@Param("columnId") Long columnId);

    int countColumnArticle(@Param("columnId") Long columnId, @Param("articleId") Long articleId);

    int restoreColumnArticle(@Param("columnId") Long columnId, @Param("articleId") Long articleId,
                             @Param("sortOrder") int sortOrder);

    int insertColumnArticle(@Param("columnId") Long columnId, @Param("articleId") Long articleId,
                            @Param("sortOrder") int sortOrder);

    /**
     * 将专栏的 article_count +1（绑定文章时维护冗余字段）。
     */
    int incrementArticleCount(@Param("columnId") Long columnId);

    List<ColumnDO> searchPublished(@Param("keyword") String keyword, @Param("sort") String sort,
                                   @Param("offset") int offset, @Param("limit") int limit);

    long countSearchPublished(@Param("keyword") String keyword);
}
