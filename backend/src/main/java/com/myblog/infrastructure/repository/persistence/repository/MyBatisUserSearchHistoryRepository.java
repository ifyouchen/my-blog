package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.model.readmodel.UserSearchHistory;
import com.myblog.domain.repository.UserSearchHistoryRepository;
import com.myblog.infrastructure.repository.persistence.entity.UserSearchHistoryDO;
import com.myblog.infrastructure.repository.persistence.mapper.UserSearchHistoryMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
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
    private final IdGenerator idGenerator;

    /**
     * 创建用户搜索历史 MyBatis 仓储。
     *
     * @param userSearchHistoryMapper 用户搜索历史 Mapper
     * @param idGenerator 全局 ID 生成器
     */
    public MyBatisUserSearchHistoryRepository(UserSearchHistoryMapper userSearchHistoryMapper, IdGenerator idGenerator) {
        this.userSearchHistoryMapper = userSearchHistoryMapper;
        this.idGenerator = idGenerator;
    }

    /**
     * 根据 ID 查询搜索历史记录。
     *
     * @param id 搜索历史记录 ID
     * @return 搜索历史数据对象
     */
    @Override
    public UserSearchHistory findById(Long id) {
        return toReadModel(userSearchHistoryMapper.selectById(id));
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
    public List<UserSearchHistory> findByUserId(Long userId, int page, int pageSize) {
        int offset = (Math.max(page, 1) - 1) * Math.max(pageSize, 1);
        return toReadModels(userSearchHistoryMapper.selectByUserId(userId, offset, pageSize));
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
    public UserSearchHistory findByUserIdAndKeyword(Long userId, String keyword) {
        return toReadModel(userSearchHistoryMapper.selectByUserIdAndKeyword(userId, keyword));
    }

    /**
     * 保存搜索历史记录。
     *
     * @param userSearchHistoryDO 搜索历史数据对象
     */
    @Override
    public void save(UserSearchHistory userSearchHistory) {
        userSearchHistoryMapper.insert(toDO(userSearchHistory));
    }

    /**
     * 更新搜索历史记录。
     *
     * @param userSearchHistoryDO 搜索历史数据对象
     */
    @Override
    public void update(UserSearchHistory userSearchHistory) {
        userSearchHistoryMapper.update(toDO(userSearchHistory));
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
        return idGenerator.nextId("blog_user_search_history");
    }

    /**
     * 转换搜索历史列表。
     *
     * @param rows 持久化行列表
     * @return 搜索历史列表
     */
    private List<UserSearchHistory> toReadModels(List<UserSearchHistoryDO> rows) {
        List<UserSearchHistory> result = new ArrayList<UserSearchHistory>(rows.size());
        for (UserSearchHistoryDO row : rows) {
            result.add(toReadModel(row));
        }
        return result;
    }

    /**
     * 转换搜索历史。
     *
     * @param row 持久化行
     * @return 搜索历史
     */
    private UserSearchHistory toReadModel(UserSearchHistoryDO row) {
        if (row == null) {
            return null;
        }
        UserSearchHistory history = new UserSearchHistory();
        history.setId(row.getId());
        history.setUserId(row.getUserId());
        history.setKeyword(row.getKeyword());
        history.setSearchCount(row.getSearchCount());
        history.setLastSearchedAt(row.getLastSearchedAt());
        history.setCreatedAt(row.getCreatedAt());
        history.setUpdatedAt(row.getUpdatedAt());
        history.setDeletedAt(row.getDeletedAt());
        history.setVersion(row.getVersion());
        return history;
    }

    /**
     * 转换为持久化行。
     *
     * @param history 搜索历史
     * @return 持久化行
     */
    private UserSearchHistoryDO toDO(UserSearchHistory history) {
        UserSearchHistoryDO row = new UserSearchHistoryDO();
        row.setId(history.getId());
        row.setUserId(history.getUserId());
        row.setKeyword(history.getKeyword());
        row.setSearchCount(history.getSearchCount());
        row.setLastSearchedAt(history.getLastSearchedAt());
        row.setCreatedAt(history.getCreatedAt());
        row.setUpdatedAt(history.getUpdatedAt());
        row.setDeletedAt(history.getDeletedAt());
        row.setVersion(history.getVersion());
        return row;
    }
}
