package com.myblog.application.service;

import com.myblog.infrastructure.repository.persistence.entity.SensitiveWordDO;
import com.myblog.infrastructure.repository.persistence.mapper.SensitiveWordMapper;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 敏感词应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
@Profile("!memory")
public class SensitiveWordAppService {

    private final SensitiveWordMapper sensitiveWordMapper;

    public SensitiveWordAppService(SensitiveWordMapper sensitiveWordMapper) {
        this.sensitiveWordMapper = sensitiveWordMapper;
    }

    /**
     * 分页查询敏感词列表。
     */
    public PageResult<Map<String, Object>> pageList(String keyword, String category,
                                                     int page, int pageSize) {
        int safePageSize = Math.max(1, Math.min(pageSize, 100));
        int safePage = Math.max(1, page);
        int offset = (safePage - 1) * safePageSize;
        List<SensitiveWordDO> rows = sensitiveWordMapper.selectPage(keyword, category, offset, safePageSize);
        long total = sensitiveWordMapper.countPage(keyword, category);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>(rows.size());
        for (SensitiveWordDO row : rows) {
            items.add(toMap(row));
        }
        return new PageResult<Map<String, Object>>(items, safePage, safePageSize, total);
    }

    /**
     * 创建敏感词。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(String word, String category, String level) {
        validateWord(word);
        if (sensitiveWordMapper.countByWord(word.trim(), null) > 0) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "敏感词已存在：" + word);
        }
        SensitiveWordDO row = new SensitiveWordDO();
        row.setId(sensitiveWordMapper.selectNextId());
        row.setWord(word.trim());
        row.setCategory(category != null && !category.trim().isEmpty() ? category.trim() : "GENERAL");
        row.setLevel(normalizeLevel(level));
        sensitiveWordMapper.insertOrUpdate(row);
        return toMap(row);
    }

    /**
     * 更新敏感词。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> update(Long id, String word, String category, String level) {
        SensitiveWordDO row = sensitiveWordMapper.selectById(id);
        if (row == null) {
            throw new ApplicationException(ErrorCode.NOT_FOUND, "敏感词不存在");
        }
        if (word != null) {
            validateWord(word);
            if (sensitiveWordMapper.countByWord(word.trim(), id) > 0) {
                throw new ApplicationException(ErrorCode.PARAM_ERROR, "敏感词已存在：" + word);
            }
            row.setWord(word.trim());
        }
        if (category != null) {
            row.setCategory(category);
        }
        if (level != null) {
            row.setLevel(normalizeLevel(level));
        }
        sensitiveWordMapper.update(row);
        return toMap(row);
    }

    /**
     * 删除敏感词（软删除）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SensitiveWordDO row = sensitiveWordMapper.selectById(id);
        if (row == null) {
            throw new ApplicationException(ErrorCode.NOT_FOUND, "敏感词不存在");
        }
        sensitiveWordMapper.softDelete(id);
    }

    /**
     * 检测文本是否包含敏感词，返回命中的词列表。
     *
     * @param text 待检测文本
     * @return 命中的敏感词列表
     */
    public List<String> detect(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<String>();
        }
        List<String> words = sensitiveWordMapper.selectAllWords();
        List<String> hits = new ArrayList<String>();
        String lower = text.toLowerCase();
        for (String word : words) {
            if (lower.contains(word.toLowerCase())) {
                hits.add(word);
            }
        }
        return hits;
    }

    /**
     * 检测文本中 BLOCK 级别的敏感词。
     *
     * @param text 待检测文本
     * @return 命中的 BLOCK 级别敏感词列表
     */
    public List<String> detectBlockWords(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<String>();
        }
        List<String> words = sensitiveWordMapper.selectWordsByLevel(2);
        List<String> hits = new ArrayList<String>();
        String lower = text.toLowerCase();
        for (String word : words) {
            if (lower.contains(word.toLowerCase())) {
                hits.add(word);
            }
        }
        return hits;
    }

    /**
     * 检测文本中 WARN 级别的敏感词。
     *
     * @param text 待检测文本
     * @return 命中的 WARN 级别敏感词列表
     */
    public List<String> detectWarnWords(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<String>();
        }
        List<String> words = sensitiveWordMapper.selectWordsByLevel(1);
        List<String> hits = new ArrayList<String>();
        String lower = text.toLowerCase();
        for (String word : words) {
            if (lower.contains(word.toLowerCase())) {
                hits.add(word);
            }
        }
        return hits;
    }

    // ========== 私有方法 ==========

    private void validateWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "敏感词不能为空");
        }
        if (word.trim().length() > 100) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "敏感词不能超过 100 个字符");
        }
    }

    private Integer normalizeLevel(String level) {
        if ("BLOCK".equalsIgnoreCase(level)) {
            return 2;
        }
        return 1;
    }

    private Map<String, Object> toMap(SensitiveWordDO row) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("id", row.getId());
        map.put("word", row.getWord());
        map.put("category", row.getCategory());
        map.put("level", row.getLevel() == null ? "WARN" : (row.getLevel() == 2 ? "BLOCK" : "WARN"));
        map.put("createdAt", row.getCreatedAt());
        map.put("updatedAt", row.getUpdatedAt());
        return map;
    }
}

