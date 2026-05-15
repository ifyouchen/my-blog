package com.myblog.growth.domain.repository;

import com.myblog.growth.domain.model.aggregate.PointAccount;

import java.util.Optional;

/**
 * 积分账户 Repository 接口.
 * <p>
 * 由 Infrastructure 层实现，领域层通过此接口解耦持久化细节。
 * </p>
 */
public interface PointAccountRepository {

    /**
     * 根据用户 ID 查询积分账户.
     *
     * @param userId 用户 ID
     * @return 积分账户（Optional 包装，不存在时为 empty）
     */
    Optional<PointAccount> findByUserId(Long userId);

    /**
     * 保存积分账户（新增，回填主键）.
     *
     * @param account 积分账户聚合根
     */
    void save(PointAccount account);

    /**
     * 乐观锁 CAS 更新积分账户余额及统计字段.
     * <p>
     * UPDATE 条件：{@code version = account.version}，
     * 成功时 version 自增，失败（版本冲突）返回 0。
     * </p>
     *
     * @param account 包含最新余额和当前版本号的积分账户
     * @return 更新行数（1=成功，0=版本冲突）
     */
    int updateCAS(PointAccount account);
}

