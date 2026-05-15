package com.myblog.growth.infrastructure.repository.persistence.mapper;

import com.myblog.growth.infrastructure.repository.persistence.entity.UserPointAccountDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 积分账户 MyBatis Mapper.
 * <p>对应表 {@code user_point_account}，XML 在 {@code mapper/growth/UserPointAccountMapper.xml}。</p>
 */
@Mapper
public interface UserPointAccountMapper {

    /**
     * 根据用户 ID 查询积分账户（排除软删除）.
     *
     * @param userId 用户 ID
     * @return 积分账户 DO，不存在时返回 null
     */
    UserPointAccountDO selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户 ID 查询并锁定积分账户（FOR UPDATE）.
     *
     * @param userId 用户 ID
     * @return 积分账户 DO，不存在时返回 null
     */
    UserPointAccountDO selectByUserIdForUpdate(@Param("userId") Long userId);

    /**
     * 插入新积分账户，回填自增主键.
     *
     * @param account 积分账户 DO
     */
    void insert(UserPointAccountDO account);

    /**
     * 乐观锁 CAS 更新余额及统计字段.
     *
     * @param account 包含最新字段和当前 version 的 DO
     * @return 更新行数（1=成功，0=版本冲突）
     */
    int updateBalanceCAS(UserPointAccountDO account);
}

