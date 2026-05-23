package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.CategoryGroupDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 分类组 MyBatis Mapper。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface CategoryGroupMapper {

    /**
     * 根据 ID 查询分类组。
     *
     * @param id 分类组 ID
     * @return 分类组数据对象
     */
    CategoryGroupDO selectById(@Param("id") Long id);

    /**
     * 根据名称查询分类组。
     *
     * @param name 分类组名称
     * @return 分类组数据对象
     */
    CategoryGroupDO selectByName(@Param("name") String name);

    /**
     * 查询分类组列表。
     *
     * @param enabled 是否启用
     * @return 分类组列表
     */
    List<CategoryGroupDO> selectAll(@Param("enabled") Boolean enabled);

    /**
     * 根据名称统计分类组数量。
     *
     * @param name 分类组名称
     * @param excludeId 排除 ID
     * @return 数量
     */
    int countByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    /**
     * 插入或更新分类组。
     *
     * @param groupDO 分类组数据对象
     * @return 影响行数
     */
    int insertOrUpdate(CategoryGroupDO groupDO);
}
