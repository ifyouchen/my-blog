package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.TagDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TagMapper {

    TagDO selectById(@Param("id") Long id);

    int countById(@Param("id") Long id);

    List<TagDO> selectAll(@Param("enabled") Boolean enabled);

    List<TagDO> selectPage(@Param("enabled") Boolean enabled,
                           @Param("offset") int offset,
                           @Param("pageSize") int pageSize);

    long countAll(@Param("enabled") Boolean enabled);

    int countByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    Long selectNextId();

    int insert(TagDO tagDO);

    int update(TagDO tagDO);
}
