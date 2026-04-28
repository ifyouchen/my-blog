package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.CategoryDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CategoryMapper {

    CategoryDO selectById(@Param("id") Long id);

    int countById(@Param("id") Long id);

    List<CategoryDO> selectAll(@Param("enabled") Boolean enabled);

    List<CategoryDO> selectPage(@Param("enabled") Boolean enabled,
                                @Param("offset") int offset,
                                @Param("pageSize") int pageSize);

    long countAll(@Param("enabled") Boolean enabled);

    int countByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    Long selectNextId();

    int insert(CategoryDO categoryDO);
    int insertOrUpdate(CategoryDO categoryDO);


    int update(CategoryDO categoryDO);
}
