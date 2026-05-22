package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.infrastructure.repository.persistence.mapper.IdGeneratorMapper;
import org.springframework.stereotype.Component;

@Component
public class IdGenerator {
    private final IdGeneratorMapper idGeneratorMapper;

    public IdGenerator(IdGeneratorMapper idGeneratorMapper) {
        this.idGeneratorMapper = idGeneratorMapper;
    }

    public long nextId(String sequenceName) {
        idGeneratorMapper.updateNextId(sequenceName);
        return idGeneratorMapper.selectNextId();
    }
}
