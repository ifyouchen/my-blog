package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.TagDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 标签 MyBatis Mapper。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface TagMapper {

    /**
     * 根据 ID 查询标签。
     *
     * @param id 标签 ID
     * @return 标签数据对象
     */
    TagDO selectById(@Param("id") Long id);

    /**
     * 根据 ID 统计标签数量。
     *
     * @param id 标签 ID
     * @return 标签数量
     */
    int countById(@Param("id") Long id);

    /**
     * 查询所有标签。
     *
     * @param enabled 是否启用，null 表示不过滤
     * @return 标签列表
     */
    List<TagDO> selectAll(@Param("enabled") Boolean enabled);

    /**
     * 分页查询标签列表。
     *
     * @param enabled  是否启用，null 表示不过滤
     * @param keyword  关键字
     * @param offset   偏移量
     * @param pageSize 每页大小
     * @return 标签列表
     */
    List<TagDO> selectPage(@Param("enabled") Boolean enabled,
                           @Param("keyword") String keyword,
                           @Param("offset") int offset,
                           @Param("pageSize") int pageSize);

    /**
     * 统计标签总数。
     *
     * @param enabled 是否启用，null 表示不过滤
     * @param keyword 关键字
     * @return 标签总数
     */
    long countAll(@Param("enabled") Boolean enabled,
                  @Param("keyword") String keyword);

    /**
     * 根据名称统计标签数量（可排除指定 ID）。
     *
     * @param name      标签名称
     * @param excludeId 排除的标签 ID
     * @return 标签数量
     */
    int countByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    /**
     * 查询下一个标签 ID。
     *
     * @return 下一个标签 ID
     */
    Long selectNextId();

    /**
     * 插入标签。
     *
     * @param tagDO 标签数据对象
     * @return 影响行数
     */
    int insert(TagDO tagDO);

    /**
     * 插入或更新标签（INSERT ... ON DUPLICATE KEY UPDATE）。
     *
     * @param tagDO 标签数据对象
     * @return 影响行数
     */
    int insertOrUpdate(TagDO tagDO);

    /**
     * 更新标签。
     *
     * @param tagDO 标签数据对象
     * @return 影响行数
     */
    int update(TagDO tagDO);

    /**
     * 查询热门标签。
     *
     * @param limit 返回数量限制
     * @return 热门标签列表
     */
    List<TagDO> selectHot(@Param("limit") int limit);

    /**
     * 查询所有不重复的标签大类。
     *
     * @return 大类列表，包含 name 和 tagCount
     */
    List<Map<String, Object>> selectDistinctGroups();

    /**
     * 重命名标签大类。
     *
     * @param oldName 原大类名称
     * @param newName 新大类名称
     * @return 影响行数
     */
    int renameGroup(@Param("oldName") String oldName, @Param("newName") String newName);

    /**
     * 清空指定大类的标签（设为 NULL）。
     *
     * @param groupName 大类名称
     * @return 影响行数
     */
    int clearGroup(@Param("groupName") String groupName);
}
