package com.myblog.growth.infrastructure.repository.persistence.mapper;

import com.myblog.growth.infrastructure.repository.persistence.entity.UserGrowthAccountDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户成长账户 MyBatis Mapper.
 * <p>
 * 对应数据库表 {@code user_growth_account}，XML 定义于
 * {@code mapper/growth/UserGrowthAccountMapper.xml}。
 * </p>
 */
@Mapper
public interface UserGrowthAccountMapper {

    /**
     * 根据用户 ID 查询成长账户（排除已软删除记录）.
     *
     * @param userId 用户 ID
     * @return 成长账户 DO，不存在时返回 null
     */
    UserGrowthAccountDO selectByUserId(@Param("userId") Long userId);

    /**
     * 插入新成长账户，回填自增主键.
     *
     * @param account 成长账户 DO
     */
    void insert(UserGrowthAccountDO account);

    /**
     * 乐观锁更新经验值和等级.
     * <p>
     * UPDATE 条件包含 {@code version = #{version}}，并将 version 自增。
     * </p>
     *
     * @param account 包含最新 currentExp、currentLevel、version 的 DO
     * @return 更新行数（1=成功，0=版本冲突）
     */
    int updateExpAndLevelCAS(UserGrowthAccountDO account);
}

