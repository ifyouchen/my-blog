package com.myblog.growth.domain.repository;

/**
 * 事件幂等消费记录 Repository 接口.
 * <p>
 * 通过在 {@code event_consume_record} 表中维护 (event_id, consumer_name) 唯一记录，
 * 保证同一事件在同一消费者下只被处理一次。
 * </p>
 */
public interface EventConsumeRepository {

    /**
     * 尝试插入 PROCESSING 状态记录.
     * <p>
     * 利用唯一索引 {@code uk_event_consumer(event_id, consumer_name)} 保证幂等：
     * 若记录已存在，INSERT IGNORE 不会报错，返回 0。
     * </p>
     *
     * @param eventId      事件唯一 ID（幂等键）
     * @param consumerName 消费者名称（通常为处理器类名）
     * @return 1 = 插入成功（首次处理），0 = 已存在（重复消费，直接跳过）
     */
    int tryInsertProcessing(String eventId, String consumerName);

    /**
     * 将指定记录标记为 SUCCESS.
     *
     * @param eventId      事件唯一 ID
     * @param consumerName 消费者名称
     */
    void markSuccess(String eventId, String consumerName);

    /**
     * 将指定记录标记为 FAILED，并记录错误信息.
     *
     * @param eventId      事件唯一 ID
     * @param consumerName 消费者名称
     * @param errorMsg     失败原因（截断至 1000 字符）
     */
    void markFailed(String eventId, String consumerName, String errorMsg);
}

