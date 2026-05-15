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

