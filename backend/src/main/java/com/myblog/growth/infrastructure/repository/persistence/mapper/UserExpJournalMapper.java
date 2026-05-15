package com.myblog.growth.infrastructure.repository.persistence.mapper;

import com.myblog.growth.infrastructure.repository.persistence.entity.UserExpJournalDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户经验流水 MyBatis Mapper.
 * <p>
 * 对应数据库表 {@code user_exp_journal}，只允许 INSERT，禁止 UPDATE / DELETE。
 * XML 定义于 {@code mapper/growth/UserExpJournalMapper.xml}。
 * </p>
 */
@Mapper
public interface UserExpJournalMapper {

    /**
     * 插入经验流水，遇到幂等键冲突时忽略（INSERT IGNORE）.
     *
     * @param journal 经验流水 DO
     * @return 插入行数（1=成功，0=幂等键已存在）
     */
    int insertIgnore(UserExpJournalDO journal);

    /**
     * 查询用户最近 N 条经验流水（按创建时间倒序）.
     *
     * @param userId 用户 ID
     * @param limit  最大返回条数
     * @return 经验流水列表
     */
    List<UserExpJournalDO> selectRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);
}

