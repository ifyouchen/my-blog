package com.myblog.growth.infrastructure.repository.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 事件幂等消费记录 MyBatis Mapper.
 * <p>
 * 对应数据库表 {@code event_consume_record}，
 * XML 定义于 {@code mapper/growth/EventConsumeRecordMapper.xml}。
 * </p>
 */
@Mapper
public interface EventConsumeRecordMapper {

    /**
     * 插入 PROCESSING 状态记录，遇唯一键冲突时忽略（INSERT IGNORE）.
     *
     * @param eventId      事件唯一 ID
     * @param consumerName 消费者名称
     * @return 插入行数（1=成功，0=已存在）
     */
    int insertIgnoreProcessing(@Param("eventId") String eventId,
                               @Param("consumerName") String consumerName);

    /**
     * 将指定记录状态更新为 SUCCESS.
     *
     * @param eventId      事件唯一 ID
     * @param consumerName 消费者名称
     */
    void updateToSuccess(@Param("eventId") String eventId,
                         @Param("consumerName") String consumerName);

    /**
     * 将指定记录状态更新为 FAILED，并记录错误信息.
     *
     * @param eventId      事件唯一 ID
     * @param consumerName 消费者名称
     * @param errorMsg     错误信息
     */
    void updateToFailed(@Param("eventId") String eventId,
                        @Param("consumerName") String consumerName,
                        @Param("errorMsg") String errorMsg);
}

