package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.ArticleVersionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 文章版本历史 MyBatis Mapper 接口。
 *
 * @author Codex
 * @since 1.0.0
 */
@Mapper
public interface ArticleVersionMapper {

    /**
     * 查询文章版本列表（按版本号降序）。
     *
     * @param articleId 文章 ID
     * @return 版本列表
     */
    List<ArticleVersionDO> selectByArticleId(@Param("articleId") Long articleId);

    /**
     * 查询指定版本（含 content）。
     *
     * @param articleId 文章 ID
     * @param versionNo 版本号
     * @return 版本详情
     */
    ArticleVersionDO selectByArticleIdAndVersionNo(@Param("articleId") Long articleId,
                                                    @Param("versionNo") Integer versionNo);

    /**
     * 查询文章最新版本号。
     *
     * @param articleId 文章 ID
     * @return 最新版本号，不存在时返回 null
     */
    Integer selectMaxVersionNo(@Param("articleId") Long articleId);

    /**
     * 插入版本快照。
     *
     * @param version 版本数据对象
     */
    void insert(ArticleVersionDO version);

    /**
     * 删除最旧版本（保留最近 keepCount 条）。
     *
     * @param articleId 文章 ID
     * @param keepCount 保留数量
     */
    void deleteOldest(@Param("articleId") Long articleId, @Param("keepCount") int keepCount);

    /**
     * 统计文章版本数量。
     *
     * @param articleId 文章 ID
     * @return 版本数量
     */
    int countByArticleId(@Param("articleId") Long articleId);
}

