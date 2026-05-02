package com.myblog.application.service;

import com.myblog.infrastructure.repository.persistence.entity.SensitiveWordDO;
import com.myblog.infrastructure.repository.persistence.mapper.SensitiveWordMapper;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.util.BizLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 敏感词应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
@Profile("!memory")
public class SensitiveWordAppService {

    private static final Logger log = LoggerFactory.getLogger(SensitiveWordAppService.class);

    private final SensitiveWordMapper sensitiveWordMapper;
    private final SensitiveWordCache sensitiveWordCache;

    public SensitiveWordAppService(SensitiveWordMapper sensitiveWordMapper,
                                    SensitiveWordCache sensitiveWordCache) {
        this.sensitiveWordMapper = sensitiveWordMapper;
        this.sensitiveWordCache = sensitiveWordCache;
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
        long _start = System.currentTimeMillis();
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
        sensitiveWordCache.invalidate();
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "创建敏感词",
            BizLogHelper.trace(),
            BizLogHelper.params("word", row.getWord(), "category", row.getCategory(), "level", row.getLevel() == null ? "WARN" : (row.getLevel() == 2 ? "BLOCK" : "WARN")),
            BizLogHelper.result("wordId=" + row.getId()),
            BizLogHelper.elapsed(_start));
        return toMap(row);
    }

    /**
     * 更新敏感词。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> update(Long id, String word, String category, String level) {
        long _start = System.currentTimeMillis();
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
        sensitiveWordCache.invalidate();
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "更新敏感词",
            BizLogHelper.trace(),
            BizLogHelper.params("sensitiveWordId", id, "word", word, "category", category, "level", level),
            BizLogHelper.result("wordId=" + id),
            BizLogHelper.elapsed(_start));
        return toMap(row);
    }

    /**
     * 删除敏感词（软删除）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        long _start = System.currentTimeMillis();
        SensitiveWordDO row = sensitiveWordMapper.selectById(id);
        if (row == null) {
            throw new ApplicationException(ErrorCode.NOT_FOUND, "敏感词不存在");
        }
        sensitiveWordMapper.softDelete(id);
        sensitiveWordCache.invalidate();
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "删除敏感词",
            BizLogHelper.trace(),
            BizLogHelper.params("sensitiveWordId", id),
            BizLogHelper.result("deleted=true"),
            BizLogHelper.elapsed(_start));
    }

    /**
     * 检测文本是否包含敏感词，返回命中的词列表。
     *
     * @param text 待检测文本
     * @return 命中的敏感词列表
     */
    public List<String> detect(String text) {
        return findHits(text, sensitiveWordCache.getAllWords());
    }

    /**
     * 检测文本中 BLOCK 级别的敏感词。
     *
     * @param text 待检测文本
     * @return 命中的 BLOCK 级别敏感词列表
     */
    public List<String> detectBlockWords(String text) {
        return findHits(text, sensitiveWordCache.getBlockWords());
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
        return findHits(text, sensitiveWordCache.getWarnWords());
    }

    /**
     * 获取 WARN 级别敏感词列表，供前端做提交前提示。
     *
     * @return WARN 级别敏感词列表
     */
    public List<String> listWarnWords() {
        return normalizeWords(sensitiveWordCache.getWarnWords());
    }

    /**
     * 将 WARN 级别敏感词替换为统一掩码。
     *
     * @param text 原始文本
     * @return 脱敏后的文本
     */
    public String maskWarnWords(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String result = text;
        for (String word : normalizeWords(sensitiveWordCache.getWarnWords())) {
            Pattern pattern = Pattern.compile(Pattern.quote(word), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            result = pattern.matcher(result).replaceAll(Matcher.quoteReplacement("***"));
        }
        return result;
    }

    private List<String> findHits(String text, List<String> words) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<String>();
        }
        String lower = text.toLowerCase(Locale.ROOT);
        List<String> hits = new ArrayList<String>();
        for (String word : normalizeWords(words)) {
            if (lower.contains(word.toLowerCase(Locale.ROOT))) {
                hits.add(word);
            }
        }
        return hits;
    }

    private List<String> normalizeWords(List<String> words) {
        Set<String> normalized = new LinkedHashSet<String>();
        if (words != null) {
            for (String word : words) {
                if (word != null && !word.trim().isEmpty()) {
                    normalized.add(word.trim());
                }
            }
        }
        List<String> result = new ArrayList<String>(normalized);
        result.sort((left, right) -> Integer.compare(right.length(), left.length()));
        return result;
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

