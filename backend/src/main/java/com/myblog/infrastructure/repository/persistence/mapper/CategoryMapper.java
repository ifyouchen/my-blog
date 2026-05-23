package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.CategoryDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 分类 MyBatis Mapper。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface CategoryMapper {

    /**
     * 根据 ID 查询分类。
     *
     * @param id 分类 ID
     * @return 分类数据对象
     */
    CategoryDO selectById(@Param("id") Long id);

    /**
     * 根据 ID 统计分类数量。
     *
     * @param id 分类 ID
     * @return 分类数量
     */
    int countById(@Param("id") Long id);

    /**
     * 查询所有分类。
     *
     * @param enabled 是否启用，null 表示不过滤
     * @return 分类列表
     */
    List<CategoryDO> selectAll(@Param("enabled") Boolean enabled);

    /**
     * 分页查询分类列表。
     *
     * @param enabled  是否启用，null 表示不过滤
     * @param offset   偏移量
     * @param pageSize 每页大小
     * @return 分类列表
     */
    List<CategoryDO> selectPage(@Param("enabled") Boolean enabled,
                                @Param("groupId") Long groupId,
                                @Param("keyword") String keyword,
                                @Param("offset") int offset,
                                @Param("pageSize") int pageSize);

    /**
     * 统计分类总数。
     *
     * @param enabled 是否启用，null 表示不过滤
     * @return 分类总数
     */
    long countAll(@Param("enabled") Boolean enabled,
                  @Param("groupId") Long groupId,
                  @Param("keyword") String keyword);

    /**
     * 根据名称统计分类数量（可排除指定 ID）。
     *
     * @param name      分类名称
     * @param excludeId 排除的分类 ID
     * @return 分类数量
     */
    int countByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    /**
     * 根据启用分类名称统计数量，分类组也必须启用。
     *
     * @param name 分类名称
     * @return 分类数量
     */
    int countEnabledByName(@Param("name") String name);

    /**
     * 统计指定分类组下的小分类数量。
     *
     * @param groupId 分类组 ID
     * @return 小分类数量
     */
    long countByGroupId(@Param("groupId") Long groupId);

    /**
     * 查询下一个分类 ID。
     *
     * @return 下一个分类 ID
     */
    Long selectNextId();

    /**
     * 插入分类。
     *
     * @param categoryDO 分类数据对象
     * @return 影响行数
     */
    int insert(CategoryDO categoryDO);

    /**
     * 插入或更新分类（INSERT ... ON DUPLICATE KEY UPDATE）。
     *
     * @param categoryDO 分类数据对象
     * @return 影响行数
     */
    int insertOrUpdate(CategoryDO categoryDO);

    /**
     * 更新分类。
     *
     * @param categoryDO 分类数据对象
     * @return 影响行数
     */
    int update(CategoryDO categoryDO);

    List<Map<String, Object>> selectDistinctGroups();

    int renameGroup(@Param("oldName") String oldName, @Param("newName") String newName);

    int clearGroup(@Param("groupName") String groupName);
}
