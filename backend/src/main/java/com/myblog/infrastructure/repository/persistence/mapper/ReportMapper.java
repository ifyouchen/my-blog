package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.ReportDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 举报 MyBatis Mapper。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface ReportMapper {

    /**
     * 根据 ID 查询举报记录。
     *
     * @param id 举报 ID
     * @return 举报数据对象
     */
    ReportDO selectById(@Param("id") Long id);

    /**
     * 后台管理分页查询举报列表。
     *
     * @param status     举报状态
     * @param targetType 举报目标类型
     * @param reasonType 举报原因类型
     * @param offset     偏移量
     * @param limit      限制数量
     * @return 举报列表
     */
    List<ReportDO> selectAdminPage(@Param("status") String status,
                                   @Param("targetType") String targetType,
                                   @Param("reasonType") String reasonType,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit);

    /**
     * 统计后台管理举报数量。
     *
     * @param status     举报状态
     * @param targetType 举报目标类型
     * @param reasonType 举报原因类型
     * @return 举报数量
     */
    long countAdminPage(@Param("status") String status,
                        @Param("targetType") String targetType,
                        @Param("reasonType") String reasonType);

    /**
     * 统计同一举报人对同一目标的待处理举报数量。
     *
     * @param reporterUserId 举报人用户 ID
     * @param targetType     举报目标类型
     * @param targetId       举报目标 ID
     * @return 待处理举报数量
     */
    int countPendingByReporterAndTarget(@Param("reporterUserId") Long reporterUserId,
                                        @Param("targetType") String targetType,
                                        @Param("targetId") Long targetId);

    /**
     * 查询下一个举报 ID。
     *
     * @return 下一个举报 ID
     */
    Long selectNextId();

    /**
     * 插入或更新举报（INSERT ... ON DUPLICATE KEY UPDATE）。
     *
     * @param reportDO 举报数据对象
     * @return 影响行数
     */
    int insertOrUpdate(ReportDO reportDO);
}
