package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.SensitiveWordDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 敏感词 MyBatis Mapper。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface SensitiveWordMapper {

    SensitiveWordDO selectById(@Param("id") Long id);

    List<SensitiveWordDO> selectPage(@Param("keyword") String keyword,
                                     @Param("category") String category,
                                     @Param("offset") int offset,
                                     @Param("limit") int limit);

    long countPage(@Param("keyword") String keyword,
                   @Param("category") String category);

    /** 查询所有敏感词（用于内容检测）。 */
    List<String> selectAllWords();

    /** 查询指定等级的敏感词列表。 */
    List<String> selectWordsByLevel(@Param("level") Integer level);

    int countByWord(@Param("word") String word, @Param("excludeId") Long excludeId);

    Long selectNextId();

    int insertOrUpdate(SensitiveWordDO sensitiveWordDO);

    int update(SensitiveWordDO sensitiveWordDO);

    int softDelete(@Param("id") Long id);
}

