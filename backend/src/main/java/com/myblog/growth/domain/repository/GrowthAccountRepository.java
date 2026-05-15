package com.myblog.growth.domain.repository;

import com.myblog.growth.domain.model.aggregate.GrowthAccount;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 用户成长账户 Repository 接口.
 */
public interface GrowthAccountRepository {

    /**
     * 根据用户 ID 查询成长账户.
     *
     * @param userId 用户 ID
     * @return 成长账户（Optional）
     */
    Optional<GrowthAccount> findByUserId(Long userId);

    /**
     * 批量查询用户成长账户.
     *
     * @param userIds 用户 ID 列表
     * @return 用户 ID 到成长账户的映射
     */
    Map<Long, GrowthAccount> findByUserIds(List<Long> userIds);

    /**
     * 保存成长账户（新增）.
     *
     * @param account 成长账户
     */
    void save(GrowthAccount account);

    /**
     * 使用乐观锁更新经验值和等级.
     * <p>
     * 更新条件：version = account.version，若版本不匹配则返回 false。
     * </p>
     *
     * @param account 带最新经验和版本号的成长账户
     * @return 更新行数（1=成功，0=版本冲突）
     */
    int updateExpAndLevel(GrowthAccount account);
}

