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

    /**
     * 根据 ID 查询敏感词。
     *
     * @param id 敏感词 ID
     * @return 敏感词数据对象
     */
    SensitiveWordDO selectById(@Param("id") Long id);

    /**
     * 分页查询敏感词列表。
     *
     * @param keyword  关键字
     * @param category 分类
     * @param offset   偏移量
     * @param limit    限制数量
     * @return 敏感词列表
     */
    List<SensitiveWordDO> selectPage(@Param("keyword") String keyword,
                                     @Param("category") String category,
                                     @Param("offset") int offset,
                                     @Param("limit") int limit);

    /**
     * 统计敏感词数量。
     *
     * @param keyword  关键字
     * @param category 分类
     * @return 敏感词数量
     */
    long countPage(@Param("keyword") String keyword,
                   @Param("category") String category);

    /**
     * 查询所有敏感词（用于内容检测）。
     *
     * @return 敏感词列表
     */
    List<String> selectAllWords();

    /**
     * 查询指定等级的敏感词列表。
     *
     * @param level 等级
     * @return 敏感词列表
     */
    List<String> selectWordsByLevel(@Param("level") Integer level);

    /**
     * 根据词语统计敏感词数量（可排除指定 ID）。
     *
     * @param word      敏感词
     * @param excludeId 排除的敏感词 ID
     * @return 敏感词数量
     */
    int countByWord(@Param("word") String word, @Param("excludeId") Long excludeId);

    /**
     * 查询下一个敏感词 ID。
     *
     * @return 下一个敏感词 ID
     */
    Long selectNextId();

    /**
     * 插入或更新敏感词（INSERT ... ON DUPLICATE KEY UPDATE）。
     *
     * @param sensitiveWordDO 敏感词数据对象
     * @return 影响行数
     */
    int insertOrUpdate(SensitiveWordDO sensitiveWordDO);

    /**
     * 更新敏感词。
     *
     * @param sensitiveWordDO 敏感词数据对象
     * @return 影响行数
     */
    int update(SensitiveWordDO sensitiveWordDO);

    /**
     * 软删除敏感词。
     *
     * @param id 敏感词 ID
     * @return 影响行数
     */
    int softDelete(@Param("id") Long id);
}

