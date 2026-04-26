package com.myblog.domain.repository;

import com.myblog.infrastructure.repository.persistence.entity.UserSearchHistoryDO;

import java.util.List;

/**
 * 用户搜索历史仓储接口。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface UserSearchHistoryRepository {

    UserSearchHistoryDO findById(Long id);

    List<UserSearchHistoryDO> findByUserId(Long userId, int page, int pageSize);

    long countByUserId(Long userId);

    UserSearchHistoryDO findByUserIdAndKeyword(Long userId, String keyword);

    void save(UserSearchHistoryDO userSearchHistoryDO);

    void update(UserSearchHistoryDO userSearchHistoryDO);

    void deleteByUserId(Long userId);

    List<String> findHotKeywords(int limit);

    Long nextId();
}