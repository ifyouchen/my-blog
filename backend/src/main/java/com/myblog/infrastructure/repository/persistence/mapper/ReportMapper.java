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

    ReportDO selectById(@Param("id") Long id);

    List<ReportDO> selectAdminPage(@Param("status") String status,
                                   @Param("targetType") String targetType,
                                   @Param("reasonType") String reasonType,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit);

    long countAdminPage(@Param("status") String status,
                        @Param("targetType") String targetType,
                        @Param("reasonType") String reasonType);

    int countPendingByReporterAndTarget(@Param("reporterUserId") Long reporterUserId,
                                        @Param("targetType") String targetType,
                                        @Param("targetId") Long targetId);

    Long selectNextId();

    int insertOrUpdate(ReportDO reportDO);
}
