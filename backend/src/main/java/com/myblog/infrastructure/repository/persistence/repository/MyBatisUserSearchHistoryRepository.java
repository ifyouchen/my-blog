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

    public MyBatisUserSearchHistoryRepository(UserSearchHistoryMapper userSearchHistoryMapper) {
        this.userSearchHistoryMapper = userSearchHistoryMapper;
    }

    @Override
    public UserSearchHistoryDO findById(Long id) {
        return userSearchHistoryMapper.selectById(id);
    }

    @Override
    public List<UserSearchHistoryDO> findByUserId(Long userId, int page, int pageSize) {
        int offset = (Math.max(page, 1) - 1) * Math.max(pageSize, 1);
        return userSearchHistoryMapper.selectByUserId(userId, offset, pageSize);
    }

    @Override
    public long countByUserId(Long userId) {
        return userSearchHistoryMapper.countByUserId(userId);
    }

    @Override
    public UserSearchHistoryDO findByUserIdAndKeyword(Long userId, String keyword) {
        return userSearchHistoryMapper.selectByUserIdAndKeyword(userId, keyword);
    }

    @Override
    public void save(UserSearchHistoryDO userSearchHistoryDO) {
        userSearchHistoryMapper.insert(userSearchHistoryDO);
    }

    @Override
    public void update(UserSearchHistoryDO userSearchHistoryDO) {
        userSearchHistoryMapper.update(userSearchHistoryDO);
    }

    @Override
    public void deleteByUserId(Long userId) {
        userSearchHistoryMapper.deleteByUserId(userId);
    }

    @Override
    public List<String> findHotKeywords(int limit) {
        return userSearchHistoryMapper.selectHotKeywords(limit);
    }

    @Override
    public Long nextId() {
        Long nextId = userSearchHistoryMapper.selectNextId();
        return nextId == null ? 1L : nextId;
    }
}