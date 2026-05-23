package com.myblog.application.service;

import com.myblog.infrastructure.repository.persistence.mapper.SensitiveWordMapper;
import org.redisson.api.RSet;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * 敏感词 Redis 缓存，启动时从 DB 加载，定时 15 分钟刷新。
 * 使用 Redis Set 存储，支持多实例共享。
 *
 * @author Codex
 * @since 1.0.0
 */
@Component
public class SensitiveWordCache {

    private static final Logger log = LoggerFactory.getLogger(SensitiveWordCache.class);
    private static final String ALL_WORDS_KEY = "sensitive:all";
    private static final String BLOCK_WORDS_KEY = "sensitive:block";
    private static final String WARN_WORDS_KEY = "sensitive:warn";
    private static final String INVALIDATE_TOPIC = "sensitive:invalidate";

    private final SensitiveWordMapper sensitiveWordMapper;
    private final RSet<String> allWordsSet;
    private final RSet<String> blockWordsSet;
    private final RSet<String> warnWordsSet;
    private final RTopic invalidateTopic;

    public SensitiveWordCache(SensitiveWordMapper sensitiveWordMapper,
                               RedissonClient redissonClient) {
        this.sensitiveWordMapper = sensitiveWordMapper;
        this.allWordsSet = redissonClient.getSet(ALL_WORDS_KEY, StringCodec.INSTANCE);
        this.blockWordsSet = redissonClient.getSet(BLOCK_WORDS_KEY, StringCodec.INSTANCE);
        this.warnWordsSet = redissonClient.getSet(WARN_WORDS_KEY, StringCodec.INSTANCE);
        this.invalidateTopic = redissonClient.getTopic(INVALIDATE_TOPIC, StringCodec.INSTANCE);
    }

    @PostConstruct
    public void init() {
        log.info("Initializing sensitive word cache from database");
        refresh();
    }

    @Scheduled(fixedDelay = 15 * 60 * 1000, initialDelay = 15 * 60 * 1000)
    public void scheduledRefresh() {
        refresh();
    }

    public void refresh() {
        try {
            allWordsSet.clear();
            List<String> all = sensitiveWordMapper.selectAllWords();
            if (!all.isEmpty()) {
                allWordsSet.addAll(all);
            }

            blockWordsSet.clear();
            List<String> block = sensitiveWordMapper.selectWordsByLevel(2);
            if (!block.isEmpty()) {
                blockWordsSet.addAll(block);
            }

            warnWordsSet.clear();
            List<String> warn = sensitiveWordMapper.selectWordsByLevel(1);
            if (!warn.isEmpty()) {
                warnWordsSet.addAll(warn);
            }

            log.debug("Sensitive word cache refreshed: all={}, block={}, warn={}",
                all.size(), block.size(), warn.size());
        } catch (Exception e) {
            log.error("Failed to refresh sensitive word cache", e);
        }
    }

    public void invalidate() {
        allWordsSet.clear();
        blockWordsSet.clear();
        warnWordsSet.clear();
        try {
            invalidateTopic.publish("refresh");
        } catch (Exception e) {
            log.warn("Failed to publish invalidate message", e);
        }
        refresh();
    }

    public List<String> getAllWords() {
        Set<String> words = allWordsSet.readAll();
        return new ArrayList<>(words);
    }

    public List<String> getBlockWords() {
        Set<String> words = blockWordsSet.readAll();
        return new ArrayList<>(words);
    }

    public List<String> getWarnWords() {
        Set<String> words = warnWordsSet.readAll();
        return new ArrayList<>(words);
    }
}
