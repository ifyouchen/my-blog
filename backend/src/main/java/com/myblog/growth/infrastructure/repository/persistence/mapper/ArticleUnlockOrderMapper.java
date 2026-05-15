package com.myblog.growth.infrastructure.repository.persistence.mapper;

import com.myblog.growth.infrastructure.repository.persistence.entity.ArticleUnlockOrderDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 文章解锁订单 MyBatis Mapper.
 * <p>对应表 {@code article_unlock_order}，XML 在 {@code mapper/growth/ArticleUnlockOrderMapper.xml}。</p>
 */
@Mapper
public interface ArticleUnlockOrderMapper {

    /**
     * INSERT IGNORE：uk_order_no 或 uk_user_article 冲突时静默忽略.
     *
     * @param order 解锁订单 DO
     * @return 插入行数（1=成功，0=重复）
     */
    int insertIgnore(ArticleUnlockOrderDO order);

    /**
     * 根据订单号查询订单.
     *
     * @param orderNo 订单号
     * @return 解锁订单 DO，不存在时返回 null
     */
    ArticleUnlockOrderDO selectByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 根据用户 ID + 文章 ID 查询订单.
     *
     * @param userId    用户 ID
     * @param articleId 文章 ID
     * @return 解锁订单 DO，不存在时返回 null
     */
    ArticleUnlockOrderDO selectByUserIdAndArticleId(@Param("userId") Long userId,
                                                     @Param("articleId") Long articleId);

    /**
     * 将订单状态更新为 SUCCESS.
     *
     * @param orderNo 订单号
     */
    void markSuccess(@Param("orderNo") String orderNo);

    /**
     * 将订单状态更新为 FAILED.
     *
     * @param orderNo    订单号
     * @param failReason 失败原因
     */
    void markFailed(@Param("orderNo") String orderNo, @Param("failReason") String failReason);
}

