package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.AnnouncementDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 公告 Mapper 接口。
 *
 * @author Codex
 * @since 1.0.0
 */
@Mapper
public interface AnnouncementMapper {

    /**
     * 查询有效公告列表（published=1，未过期，未删除）。
     *
     * @return 有效公告列表
     */
    List<AnnouncementDO> selectActive();

    /**
     * 分页查询全部公告（管理用）。
     *
     * @param offset 偏移量
     * @param size   每页数量
     * @return 公告列表
     */
    List<AnnouncementDO> selectPage(@Param("offset") int offset, @Param("size") int size);

    /**
     * 统计全部公告数量。
     *
     * @return 数量
     */
    long countAll();

    /**
     * 按 ID 查询。
     *
     * @param id 公告 ID
     * @return 公告 DO
     */
    AnnouncementDO selectById(@Param("id") Long id);

    /**
     * 插入公告。
     *
     * @param do_ 公告 DO
     */
    void insert(AnnouncementDO do_);

    /**
     * 更新公告。
     *
     * @param do_ 公告 DO
     */
    void updateByPrimaryKeySelective(AnnouncementDO do_);

    /**
     * 软删除公告。
     *
     * @param id 公告 ID
     */
    void softDelete(@Param("id") Long id);
}

