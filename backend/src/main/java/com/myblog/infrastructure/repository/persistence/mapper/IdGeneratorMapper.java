package com.myblog.infrastructure.repository.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IdGeneratorMapper {
    void updateNextId(@Param("name") String name);
    long selectNextId();
}
