package com.myblog.growth.domain.repository;

/**
 * 文章解锁关系 Repository 接口.
 * <p>
 * 解锁关系是解锁成功后的凭证，对应 {@code article_unlock_relation} 表。
 * </p>
 */
public interface UnlockRelationRepository {

    /**
     * 检查用户是否已解锁指定文章.
     *
     * @param userId    用户 ID
     * @param articleId 文章 ID
     * @return {@code true} 表示已解锁
     */
    boolean existsByUserIdAndArticleId(Long userId, Long articleId);

    /**
     * 保存解锁凭证（INSERT IGNORE，uk_user_article 冲突时忽略）.
     *
     * @param userId    用户 ID
     * @param articleId 文章 ID
     * @param orderNo   关联订单号
     * @return 插入行数（1=成功，0=已存在）
     */
    int insertIgnore(Long userId, Long articleId, String orderNo);
}

