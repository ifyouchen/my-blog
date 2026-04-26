package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.UserSearchHistoryDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户搜索历史 MyBatis Mapper。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface UserSearchHistoryMapper {

    UserSearchHistoryDO selectById(@Param("id") Long id);

    List<UserSearchHistoryDO> selectByUserId(@Param("userId") Long userId,
                                             @Param("offset") int offset,
                                             @Param("limit") int limit);

    long countByUserId(@Param("userId") Long userId);

    UserSearchHistoryDO selectByUserIdAndKeyword(@Param("userId") Long userId,
                                                 @Param("keyword") String keyword);

    int insert(UserSearchHistoryDO userSearchHistoryDO);

    int update(UserSearchHistoryDO userSearchHistoryDO);

    int deleteByUserId(@Param("userId") Long userId);

    List<String> selectHotKeywords(@Param("limit") int limit);

    int countById(@Param("id") Long id);

    Long selectNextId();
}