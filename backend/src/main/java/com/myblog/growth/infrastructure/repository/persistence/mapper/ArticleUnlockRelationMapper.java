package com.myblog.growth.infrastructure.repository.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 文章解锁关系 MyBatis Mapper.
 * <p>对应表 {@code article_unlock_relation}，XML 在 {@code mapper/growth/ArticleUnlockRelationMapper.xml}。</p>
 */
@Mapper
public interface ArticleUnlockRelationMapper {

    /**
     * 检查用户是否已解锁指定文章.
     *
     * @param userId    用户 ID
     * @param articleId 文章 ID
     * @return 记录数（0 或 1）
     */
    int countByUserIdAndArticleId(@Param("userId") Long userId,
                                  @Param("articleId") Long articleId);

    /**
     * INSERT IGNORE：uk_user_article 冲突时静默忽略.
     *
     * @param userId    用户 ID
     * @param articleId 文章 ID
     * @param orderNo   关联订单号
     * @return 插入行数（1=成功，0=重复）
     */
    int insertIgnore(@Param("userId") Long userId,
                     @Param("articleId") Long articleId,
                     @Param("orderNo") String orderNo);
}

