package com.myblog.application.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.myblog.application.dto.CategoryDTO;
import com.myblog.application.dto.SearchBootstrapDTO;
import com.myblog.application.dto.SearchHistoryDTO;
import com.myblog.application.dto.TagDTO;
import com.myblog.domain.repository.UserSearchHistoryRepository;
import com.myblog.infrastructure.repository.persistence.entity.UserSearchHistoryDO;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.util.BizLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 搜索历史应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class SearchHistoryAppService {

    private static final Logger log = LoggerFactory.getLogger(SearchHistoryAppService.class);
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_RECENT_KEYWORDS = 10;
    private static final int HOT_KEYWORDS_LIMIT = 20;

    private final UserSearchHistoryRepository userSearchHistoryRepository;
    private final Cache<String, SearchBootstrapDTO> searchBootstrapCache;
    private final Cache<Integer, List<String>> hotKeywordsCache;
    private final CategoryAppService categoryAppService;
    private final TagAppService tagAppService;
    private final TopicAppService topicAppService;
    private final ColumnAppService columnAppService;

    public SearchHistoryAppService(UserSearchHistoryRepository userSearchHistoryRepository,
                                   Cache<String, SearchBootstrapDTO> searchBootstrapCache,
                                   Cache<Integer, List<String>> hotKeywordsCache,
                                   CategoryAppService categoryAppService,
                                   TagAppService tagAppService,
                                   TopicAppService topicAppService,
                                   ColumnAppService columnAppService) {
        this.userSearchHistoryRepository = userSearchHistoryRepository;
        this.searchBootstrapCache = searchBootstrapCache;
        this.hotKeywordsCache = hotKeywordsCache;
        this.categoryAppService = categoryAppService;
        this.tagAppService = tagAppService;
        this.topicAppService = topicAppService;
        this.columnAppService = columnAppService;
    }

    /**
     * 获取搜索引导数据（热门关键字 + 用户最近搜索）。
     *
     * @param userId 用户 ID（可为 null）
     * @return 搜索引导数据
     */
    public SearchBootstrapDTO getSearchBootstrap(Long userId) {
        String cacheKey = userId != null ? "user:" + userId : "global";
        SearchBootstrapDTO cached = searchBootstrapCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }

        SearchBootstrapDTO bootstrap = new SearchBootstrapDTO();

        // Load categories and tags
        List<CategoryDTO> categories = categoryAppService.getCategories(true);
        List<TagDTO> tags = tagAppService.getTags(true);
        bootstrap.setCategories(categories);
        bootstrap.setTags(tags);
        bootstrap.setRecommendedTopics(topicAppService.listHotTopics(4));
        bootstrap.setRecommendedColumns(columnAppService.listRecommendedColumns(4, userId));

        // Load hot keywords
        List<String> hotKeywords = hotKeywordsCache.getIfPresent(HOT_KEYWORDS_LIMIT);
        if (hotKeywords == null) {
            hotKeywords = userSearchHistoryRepository.findHotKeywords(HOT_KEYWORDS_LIMIT);
            hotKeywordsCache.put(HOT_KEYWORDS_LIMIT, hotKeywords);
        }
        bootstrap.setHotKeywords(hotKeywords);

        // Load recent keywords for logged-in user
        if (userId != null) {
            List<UserSearchHistoryDO> recentHistory = userSearchHistoryRepository.findByUserId(userId, 1, MAX_RECENT_KEYWORDS);
            List<String> recentKeywords = new ArrayList<>(recentHistory.size());
            for (UserSearchHistoryDO history : recentHistory) {
                recentKeywords.add(history.getKeyword());
            }
            bootstrap.setRecentKeywords(recentKeywords);
        } else {
            bootstrap.setRecentKeywords(new ArrayList<>());
        }

        searchBootstrapCache.put(cacheKey, bootstrap);
        return bootstrap;
    }

    /**
     * 记录搜索关键字。
     *
     * @param userId 用户 ID
     * @param keyword 关键字
     */
    public void recordSearch(Long userId, String keyword) {
        long _start = System.currentTimeMillis();
        if (userId == null || keyword == null || keyword.trim().isEmpty()) {
            return;
        }
        String trimmedKeyword = keyword.trim();

        UserSearchHistoryDO existing = userSearchHistoryRepository.findByUserIdAndKeyword(userId, trimmedKeyword);
        LocalDateTime now = LocalDateTime.now();

        if (existing != null) {
            existing.setSearchCount(existing.getSearchCount() + 1);
            existing.setLastSearchedAt(now);
            userSearchHistoryRepository.update(existing);
        } else {
            UserSearchHistoryDO newHistory = new UserSearchHistoryDO();
            newHistory.setId(userSearchHistoryRepository.nextId());
            newHistory.setUserId(userId);
            newHistory.setKeyword(trimmedKeyword);
            newHistory.setSearchCount(1);
            newHistory.setLastSearchedAt(now);
            newHistory.setCreatedAt(now);
            newHistory.setUpdatedAt(now);
            newHistory.setDeletedAt(null);
            newHistory.setVersion(0L);
            userSearchHistoryRepository.save(newHistory);
        }

        invalidateUserCache(userId);
        log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(userId),
            "记录搜索",
            BizLogHelper.params("keyword", trimmedKeyword),
            BizLogHelper.result("recorded=true"),
            BizLogHelper.elapsed(_start));
    }

    /**
     * 获取用户搜索历史。
     *
     * @param userId 用户 ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 搜索历史分页
     */
    public PageResult<SearchHistoryDTO> getUserSearchHistory(Long userId, int page, int pageSize) {
        List<UserSearchHistoryDO> historyList = userSearchHistoryRepository.findByUserId(userId, page, pageSize);
        long total = userSearchHistoryRepository.countByUserId(userId);

        List<SearchHistoryDTO> dtos = new ArrayList<>(historyList.size());
        for (UserSearchHistoryDO history : historyList) {
            SearchHistoryDTO dto = new SearchHistoryDTO();
            dto.setId(history.getId());
            dto.setKeyword(history.getKeyword());
            dto.setSearchCount(history.getSearchCount());
            dto.setLastSearchedAt(history.getLastSearchedAt() != null
                    ? DATETIME_FORMATTER.format(history.getLastSearchedAt()) : null);
            dtos.add(dto);
        }

        return new PageResult<>(dtos, page, pageSize, total);
    }

    /**
     * 清空用户搜索历史。
     *
     * @param userId 用户 ID
     */
    public void clearUserSearchHistory(Long userId) {
        long _start = System.currentTimeMillis();
        userSearchHistoryRepository.deleteByUserId(userId);
        invalidateUserCache(userId);
        log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(userId),
            "清空搜索历史",
            BizLogHelper.params(),
            BizLogHelper.result("cleared=true"),
            BizLogHelper.elapsed(_start));
    }

    /**
     * 获取热门搜索关键字。
     *
     * @param limit 限制数量
     * @return 热门关键字列表
     */
    public List<String> getHotKeywords(int limit) {
        List<String> cached = hotKeywordsCache.getIfPresent(limit);
        if (cached != null) {
            return cached;
        }

        List<String> hotKeywords = userSearchHistoryRepository.findHotKeywords(limit);
        hotKeywordsCache.put(limit, hotKeywords);
        return hotKeywords;
    }

    /**
     * 获取用户最近搜索关键字。
     *
     * @param userId 用户 ID
     * @param limit 限制数量
     * @return 最近关键字列表
     */
    public List<String> getRecentKeywords(Long userId, int limit) {
        if (userId == null) {
            return new ArrayList<>();
        }
        List<UserSearchHistoryDO> recentHistory = userSearchHistoryRepository.findByUserId(userId, 1, limit);
        List<String> recentKeywords = new ArrayList<>(recentHistory.size());
        for (UserSearchHistoryDO history : recentHistory) {
            recentKeywords.add(history.getKeyword());
        }
        return recentKeywords;
    }

    private void invalidateUserCache(Long userId) {
        searchBootstrapCache.asMap().remove("user:" + userId);
        hotKeywordsCache.asMap().remove(HOT_KEYWORDS_LIMIT);
    }
}
