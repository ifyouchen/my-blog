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
     * 查询用户近期积分流水（按创建时间倒序）.
     *
     * @param userId 用户 ID
     * @param limit  最多返回条数
     * @return 流水列表
     */
    List<UserPointJournalDO> selectRecentByUserId(@Param("userId") Long userId,
                                                   @Param("limit") int limit);
}

