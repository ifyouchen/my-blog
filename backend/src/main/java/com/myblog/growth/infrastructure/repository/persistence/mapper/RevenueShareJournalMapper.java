package com.myblog.growth.infrastructure.repository.persistence.mapper;

import com.myblog.growth.infrastructure.repository.persistence.entity.RevenueShareJournalDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 分账流水 MyBatis Mapper.
 * <p>对应表 {@code revenue_share_journal}，XML 在 {@code mapper/growth/RevenueShareJournalMapper.xml}。</p>
 */
@Mapper
public interface RevenueShareJournalMapper {

    /**
     * INSERT IGNORE：uk_order_no 冲突时静默忽略.
     *
     * @param journal 分账流水 DO
     * @return 插入行数（1=成功，0=重复）
     */
    int insertIgnore(RevenueShareJournalDO journal);

    /**
     * 根据订单号查询并锁定分账流水.
     *
     * @param orderNo 解锁订单号
     * @return 分账流水，不存在时返回 null
     */
    RevenueShareJournalDO selectByOrderNoForUpdate(@Param("orderNo") String orderNo);

    /**
     * 标记分账已结算.
     *
     * @param orderNo           解锁订单号
     * @param pointJournalBizNo 作者积分流水业务单号
     * @return 更新行数
     */
    int markSettled(@Param("orderNo") String orderNo,
                    @Param("pointJournalBizNo") String pointJournalBizNo);

    /**
     * 标记分账结算失败.
     *
     * @param orderNo   解锁订单号
     * @param lastError 最近一次错误信息
     * @return 更新行数
     */
    int markFailed(@Param("orderNo") String orderNo,
                   @Param("lastError") String lastError);

    /**
     * 扫描需要补偿结算的分账流水.
     *
     * @param maxRetry 最大重试次数
     * @param limit    最大返回条数
     * @return 分账流水列表
     */
    List<RevenueShareJournalDO> selectRetryableForSettlement(@Param("maxRetry") int maxRetry,
                                                             @Param("limit") int limit);

    /**
     * 管理端分页查询分账流水.
     *
     * @param authorId         作者用户 ID（可选）
     * @param authorKeyword    作者用户名或邮箱（可选）
     * @param settlementStatus 结算状态（可选）
     * @param limit            每页条数
     * @param offset           偏移量
     * @return 分账流水列表
     */
    List<RevenueShareJournalDO> selectAdminPage(@Param("authorId") Long authorId,
                                                @Param("authorKeyword") String authorKeyword,
                                                @Param("settlementStatus") String settlementStatus,
                                                @Param("limit") int limit,
                                                @Param("offset") int offset);

    /**
     * 管理端统计分账流水总数.
     *
     * @param authorId         作者用户 ID（可选）
     * @param authorKeyword    作者用户名或邮箱（可选）
     * @param settlementStatus 结算状态（可选）
     * @return 总数
     */
    long countAdmin(@Param("authorId") Long authorId,
                    @Param("authorKeyword") String authorKeyword,
                    @Param("settlementStatus") String settlementStatus);

    /**
     * 查询作者的分账流水（按创建时间倒序）.
     *
     * @param authorId 作者用户 ID
     * @param limit    最多返回条数
     * @param offset   偏移量
     * @return 分账流水列表
     */
    List<RevenueShareJournalDO> selectPageByAuthorId(@Param("authorId") Long authorId,
                                                      @Param("limit") int limit,
                                                      @Param("offset") int offset);

    /**
     * 统计作者分账流水总数.
     *
     * @param authorId 作者用户 ID
     * @return 总数
     */
    long countByAuthorId(@Param("authorId") Long authorId);
}

