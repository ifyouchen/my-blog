package com.myblog.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Snowflake ID 生成器。
 * <p>
 * 64 位结构：1 bit 符号位 | 41 bit 时间戳（毫秒） | 10 bit 机器 ID | 12 bit 序列号
 * 每秒最大可生成 4096000 个 ID，支持 69 年不重复。
 * </p>
 *
 * @author Codex
 * @since 1.0.0
 */
public class SnowflakeIdGenerator {

    private static final Logger log = LoggerFactory.getLogger(SnowflakeIdGenerator.class);

    /** 起始时间戳（2024-01-01 00:00:00 UTC） */
    private static final long EPOCH = 1704067200000L;

    /** 机器 ID 位数 */
    private static final long MACHINE_ID_BITS = 10L;

    /** 序列号位数 */
    private static final long SEQUENCE_BITS = 12L;

    /** 机器 ID 最大值 */
    private static final long MAX_MACHINE_ID = ~(-1L << MACHINE_ID_BITS);

    /** 序列号最大值 */
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    /** 机器 ID 左移位数 */
    private static final long MACHINE_ID_SHIFT = SEQUENCE_BITS;

    /** 时间戳左移位数 */
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;

    private final long machineId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    /**
     * 创建 Snowflake ID 生成器。
     *
     * @param machineId 机器 ID，范围 [0, 1023]
     */
    public SnowflakeIdGenerator(long machineId) {
        if (machineId < 0 || machineId > MAX_MACHINE_ID) {
            throw new IllegalArgumentException(
                String.format("machineId must be between 0 and %d, got %d", MAX_MACHINE_ID, machineId)
            );
        }
        this.machineId = machineId;
        log.info("SnowflakeIdGenerator initialized with machineId={}", machineId);
    }

    /**
     * 生成下一个全局唯一 ID。
     *
     * @return 全局唯一 ID
     */
    public synchronized long nextId() {
        long currentTimestamp = currentTimeMillis();

        if (currentTimestamp < lastTimestamp) {
            long diff = lastTimestamp - currentTimestamp;
            log.warn("Clock moved backwards by {}ms, waiting to recover", diff);
            // 等待时钟追上
            while (currentTimestamp < lastTimestamp) {
                currentTimestamp = currentTimeMillis();
            }
        }

        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                // 序列号溢出，等待下一毫秒
                currentTimestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = currentTimestamp;

        return ((currentTimestamp - EPOCH) << TIMESTAMP_SHIFT)
            | (machineId << MACHINE_ID_SHIFT)
            | sequence;
    }

    private long waitNextMillis(long lastTimestamp) {
        long timestamp = currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimeMillis();
        }
        return timestamp;
    }

    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}

