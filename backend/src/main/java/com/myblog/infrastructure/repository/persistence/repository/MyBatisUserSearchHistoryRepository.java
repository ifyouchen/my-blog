package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.repository.UserSearchHistoryRepository;
import com.myblog.infrastructure.repository.persistence.entity.UserSearchHistoryDO;
import com.myblog.infrastructure.repository.persistence.mapper.UserSearchHistoryMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户搜索历史 MyBatis 仓储实现。
 *
 * @author Codex
 * @since 1.0.0
 */
@Repository
public class MyBatisUserSearchHistoryRepository implements UserSearchHistoryRepository {

    private final UserSearchHistoryMapper userSearchHistoryMapper;

    /**
     * 创建用户搜索历史 MyBatis 仓储。
     *
     * @param userSearchHistoryMapper 用户搜索历史 Mapper
     */
    public MyBatisUserSearchHistoryRepository(UserSearchHistoryMapper userSearchHistoryMapper) {
        this.userSearchHistoryMapper = userSearchHistoryMapper;
    }

    /**
     * 根据 ID 查询搜索历史记录。
     *
     * @param id 搜索历史记录 ID
     * @return 搜索历史数据对象
     */
    @Override
    public UserSearchHistoryDO findById(Long id) {
        return userSearchHistoryMapper.selectById(id);
    }

    /**
     * 分页查询用户的搜索历史列表。
     *
     * @param userId   用户 ID
     * @param page     页码
     * @param pageSize 每页大小
     * @return 搜索历史列表
     */
    @Override
    public List<UserSearchHistoryDO> findByUserId(Long userId, int page, int pageSize) {
        int offset = (Math.max(page, 1) - 1) * Math.max(pageSize, 1);
        return userSearchHistoryMapper.selectByUserId(userId, offset, pageSize);
    }

    /**
     * 统计用户的搜索历史数量。
     *
     * @param userId 用户 ID
     * @return 搜索历史数量
     */
    @Override
    public long countByUserId(Long userId) {
        return userSearchHistoryMapper.countByUserId(userId);
    }

    /**
     * 根据用户 ID 和关键字查询搜索历史记录。
     *
     * @param userId  用户 ID
     * @param keyword 搜索关键字
     * @return 搜索历史数据对象
     */
    @Override
    public UserSearchHistoryDO findByUserIdAndKeyword(Long userId, String keyword) {
        return userSearchHistoryMapper.selectByUserIdAndKeyword(userId, keyword);
    }

    /**
     * 保存搜索历史记录。
     *
     * @param userSearchHistoryDO 搜索历史数据对象
     */
    @Override
    public void save(UserSearchHistoryDO userSearchHistoryDO) {
        userSearchHistoryMapper.insert(userSearchHistoryDO);
    }

    /**
     * 更新搜索历史记录。
     *
     * @param userSearchHistoryDO 搜索历史数据对象
     */
    @Override
    public void update(UserSearchHistoryDO userSearchHistoryDO) {
        userSearchHistoryMapper.update(userSearchHistoryDO);
    }

    /**
     * 删除用户的所有搜索历史记录。
     *
     * @param userId 用户 ID
     */
    @Override
    public void deleteByUserId(Long userId) {
        userSearchHistoryMapper.deleteByUserId(userId);
    }

    /**
     * 查询热门搜索关键字列表。
     *
     * @param limit 返回数量限制
     * @return 热门关键字列表
     */
    @Override
    public List<String> findHotKeywords(int limit) {
        return userSearchHistoryMapper.selectHotKeywords(limit);
    }

    /**
     * 生成下一个搜索历史记录 ID。
     *
     * @return 搜索历史记录 ID
     */
    @Override
    public Long nextId() {
        return userSearchHistoryMapper.selectNextId();
    }
}
