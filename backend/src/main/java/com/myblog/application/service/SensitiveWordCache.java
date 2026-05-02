package com.myblog.application.service;

import com.myblog.infrastructure.repository.persistence.mapper.SensitiveWordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 敏感词本地缓存，启动时从 DB 加载，定时 15 分钟刷新。
 * 手动增删改操作由调用方在更新 DB 后触发 invalidate()，下次读取时自动回填。
 *
 * @author Codex
 * @since 1.0.0
 */
@Component
@Profile("!memory")
public class SensitiveWordCache {

    private static final Logger log = LoggerFactory.getLogger(SensitiveWordCache.class);

    private final SensitiveWordMapper sensitiveWordMapper;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private volatile List<String> allWords;
    private volatile List<String> blockWords;
    private volatile List<String> warnWords;

    public SensitiveWordCache(SensitiveWordMapper sensitiveWordMapper) {
        this.sensitiveWordMapper = sensitiveWordMapper;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing sensitive word cache from database");
        refresh();
    }

    /**
     * 定时刷新：每 15 分钟全量加载。
     */
    @Scheduled(fixedDelay = 15 * 60 * 1000, initialDelay = 15 * 60 * 1000)
    public void scheduledRefresh() {
        refresh();
    }

    /**
     * 全量刷新缓存。
     */
    public void refresh() {
        lock.writeLock().lock();
        try {
            allWords = sensitiveWordMapper.selectAllWords();
            blockWords = sensitiveWordMapper.selectWordsByLevel(2);
            warnWords = sensitiveWordMapper.selectWordsByLevel(1);
            log.debug("Sensitive word cache refreshed: all={}, block={}, warn={}",
                allWords.size(), blockWords.size(), warnWords.size());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 清空缓存，下一次读取时自动从 DB 加载。
     */
    public void invalidate() {
        lock.writeLock().lock();
        try {
            allWords = null;
            blockWords = null;
            warnWords = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<String> getAllWords() {
        List<String> result = allWords;
        if (result == null) {
            lock.writeLock().lock();
            try {
                result = allWords;
                if (result == null) {
                    result = sensitiveWordMapper.selectAllWords();
                    allWords = result;
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
        return result;
    }

    public List<String> getBlockWords() {
        List<String> result = blockWords;
        if (result == null) {
            lock.writeLock().lock();
            try {
                result = blockWords;
                if (result == null) {
                    result = sensitiveWordMapper.selectWordsByLevel(2);
                    blockWords = result;
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
        return result;
    }

    public List<String> getWarnWords() {
        List<String> result = warnWords;
        if (result == null) {
            lock.writeLock().lock();
            try {
                result = warnWords;
                if (result == null) {
                    result = sensitiveWordMapper.selectWordsByLevel(1);
                    warnWords = result;
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
        return result;
    }
}
