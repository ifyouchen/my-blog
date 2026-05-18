package com.myblog.growth.infrastructure.repository.persistence.mapper;

import com.myblog.growth.domain.model.valueobject.BadgeDefinition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 徽章定义 MyBatis Mapper.
 *
 * @author Codex
 * @since 1.0.0
 */
@Mapper
public interface BadgeDefinitionMapper {

    /**
     * 查询所有启用定义.
     *
     * @return 徽章定义列表
     */
    List<BadgeDefinition> selectAllEnabled();

    /**
     * 根据编码查询启用定义.
     *
     * @param code 徽章编码
     * @return 徽章定义
     */
    Optional<BadgeDefinition> selectEnabledByCode(@Param("code") String code);

    /**
     * 根据编码批量查询启用定义.
     *
     * @param codes 徽章编码列表
     * @return 徽章定义列表
     */
    List<BadgeDefinition> selectEnabledByCodes(@Param("codes") List<String> codes);
}
