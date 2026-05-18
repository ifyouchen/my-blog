package com.myblog.growth.infrastructure.repository.persistence.mapper;

import com.myblog.growth.infrastructure.repository.persistence.entity.UserPointJournalDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 积分流水 MyBatis Mapper.
 * <p>对应表 {@code user_point_journal}，XML 在 {@code mapper/growth/UserPointJournalMapper.xml}。</p>
 */
@Mapper
public interface UserPointJournalMapper {

    /**
     * INSERT IGNORE：biz_no 冲突时静默忽略，保证幂等.
     *
     * @param journal 积分流水 DO
     * @return 插入行数（1=成功，0=重复）
     */
    int insertIgnore(UserPointJournalDO journal);

    /**
     * 根据业务单号查询积分流水.
     *
     * @param bizNo 业务单号（幂等键）
     * @return 积分流水，不存在时返回 null
     */
    UserPointJournalDO selectByBizNo(@Param("bizNo") String bizNo);

    /**
     * 批量查询已存在的业务单号.
     *
     * @param bizNos 业务单号列表
     * @return 已存在的业务单号列表
     */
    List<String> selectExistingBizNos(@Param("bizNos") List<String> bizNos);

    /**
     * 查询用户近期积分流水（按创建时间倒序）.
     *
     * @param userId 用户 ID
     * @param limit  最多返回条数
     * @return 流水列表
     */
    List<UserPointJournalDO> selectRecentByUserId(@Param("userId") Long userId,
                                                   @Param("limit") int limit);

    /**
     * 按来源类型分页查询积分流水.
     *
     * @param userId     用户 ID
     * @param sourceType 来源类型（null 或空字符串表示不筛选）
     * @param limit      每页条数
     * @param offset     偏移量
     * @return 流水列表
     */
    List<UserPointJournalDO> selectPageByUserIdAndSourceType(
            @Param("userId") Long userId,
            @Param("sourceType") String sourceType,
            @Param("limit") int limit,
            @Param("offset") int offset);

    /**
     * 统计分页总数.
     *
     * @param userId     用户 ID
     * @param sourceType 来源类型（null 或空字符串表示不筛选）
     * @return 总数
     */
    long countByUserIdAndSourceType(
            @Param("userId") Long userId,
            @Param("sourceType") String sourceType);

    /**
     * 统计今日某来源类型的积分流水数（用于 dailyLimit 校验）.
     *
     * @param userId     用户 ID
     * @param sourceType 来源类型
     * @return 今日已发次数
     */
    int countTodayByUserIdAndSourceType(
            @Param("userId") Long userId,
            @Param("sourceType") String sourceType);
}

