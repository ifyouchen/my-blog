package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.Tag;
import com.myblog.domain.model.valueobject.TagId;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 标签仓储接口。
 *
 * @author my-blog
 * @since 1.0.0
 */
public interface TagRepository {

    /**
     * 根据标签 ID 查询标签。
     *
     * @param tagId 标签 ID
     * @return 标签 Optional
     */
    Optional<Tag> findById(TagId tagId);

    /**
     * 查询所有标签，可按启用状态筛选。
     *
     * @param enabled 是否启用，为 null 时不筛选
     * @return 标签列表
     */
    List<Tag> findAll(Boolean enabled);

    /**
     * 分页查询标签。
     *
     * @param enabled  是否启用，为 null 时不筛选
     * @param keyword  关键字，为 null 时不筛选
     * @param page     页码（从 1 开始）
     * @param pageSize 每页大小
     * @return 标签列表
     */
    List<Tag> findPage(Boolean enabled, String keyword, int page, int pageSize);

    /**
     * 统计标签数量。
     *
     * @param enabled 是否启用，为 null 时不筛选
     * @param keyword 关键字，为 null 时不筛选
     * @return 标签数量
     */
    long count(Boolean enabled, String keyword);

    /**
     * 判断标签名称是否已存在（排除指定 ID）。
     *
     * @param name      标签名称
     * @param excludeId 需要排除的标签 ID，为 null 时不排除
     * @return 是否存在同名标签
     */
    boolean existsByName(String name, TagId excludeId);

    /**
     * 保存标签（新增或更新）。
     *
     * @param tag 标签聚合根
     * @return 保存后的标签
     */
    Tag save(Tag tag);

    /**
     * 生成下一个标签 ID。
     *
     * @return 标签 ID
     */
    Long nextId();

    /**
     * 查询热门标签（按使用次数降序）。
     *
     * @param limit 返回数量
     * @return 热门标签列表
     */
    List<Tag> findHot(int limit);

    /**
     * 查询所有不重复的标签大类。
     *
     * @return 大类列表
     */
    List<Map<String, Object>> findDistinctGroups();

    /**
     * 重命名标签大类。
     *
     * @param oldName 原大类名称
     * @param newName 新大类名称
     */
    void renameGroup(String oldName, String newName);

    /**
     * 清空指定大类的标签（设为 NULL）。
     *
     * @param groupName 大类名称
     */
    void clearGroup(String groupName);
}
