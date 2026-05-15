package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.myblog.growth.domain.repository.EventConsumeRepository;
import com.myblog.growth.infrastructure.repository.persistence.mapper.EventConsumeRecordMapper;
import org.springframework.stereotype.Repository;

/**
 * 事件幂等消费记录 Repository 实现.
 */
@Repository
public class EventConsumeRepositoryImpl implements EventConsumeRepository {

    /** 事件幂等消费记录 MyBatis Mapper. */
    private final EventConsumeRecordMapper mapper;

    /**
     * 构造注入 Mapper.
     *
     * @param mapper 事件消费记录 Mapper
     */
    public EventConsumeRepositoryImpl(EventConsumeRecordMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int tryInsertProcessing(String eventId, String consumerName) {
        return mapper.insertIgnoreProcessing(eventId, consumerName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void markSuccess(String eventId, String consumerName) {
        mapper.updateToSuccess(eventId, consumerName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void markFailed(String eventId, String consumerName, String errorMsg) {
        // 截断 errorMsg 至 1000 字符，与数据库字段长度对齐
        String truncated = errorMsg != null && errorMsg.length() > 1000
                ? errorMsg.substring(0, 1000)
                : errorMsg;
        mapper.updateToFailed(eventId, consumerName, truncated);
    }
}

