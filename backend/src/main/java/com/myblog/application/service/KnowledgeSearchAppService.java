package com.myblog.application.service;

import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.ColumnDTO;
import com.myblog.application.dto.TagDTO;
import com.myblog.application.dto.TopicDTO;
import com.myblog.application.dto.UnifiedSearchResultDTO;
import com.myblog.application.query.ArticlePageQuery;
import com.myblog.shared.result.PageResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * 知识发现统一搜索应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class KnowledgeSearchAppService {

    private final ArticleAppService articleAppService;
    private final TopicAppService topicAppService;
    private final ColumnAppService columnAppService;
    private final TagAppService tagAppService;

    public KnowledgeSearchAppService(ArticleAppService articleAppService,
                                     TopicAppService topicAppService,
                                     ColumnAppService columnAppService,
                                     TagAppService tagAppService) {
        this.articleAppService = articleAppService;
        this.topicAppService = topicAppService;
        this.columnAppService = columnAppService;
        this.tagAppService = tagAppService;
    }

    public UnifiedSearchResultDTO unifiedSearch(String keyword, String type, String category, String tag,
                                                String difficulty, String sort, int page, int pageSize,
                                                Long currentUserId) {
        String normalizedType = normalizeType(type);
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.min(Math.max(pageSize, 1), 20);

        UnifiedSearchResultDTO result = new UnifiedSearchResultDTO();
        result.setKeyword(keyword == null ? "" : keyword.trim());
        result.setArticles(shouldSearch(normalizedType, "articles")
            ? searchArticles(keyword, category, tag, sort, currentPage, currentPageSize, currentUserId)
            : emptyPage(currentPage, currentPageSize));
        result.setTopics(shouldSearch(normalizedType, "topics")
            ? topicAppService.searchTopics(keyword, difficulty, currentPage, currentPageSize, currentUserId)
            : emptyPage(currentPage, currentPageSize));
        result.setColumns(shouldSearch(normalizedType, "columns")
            ? columnAppService.searchColumns(keyword, currentPage, currentPageSize, currentUserId)
            : emptyPage(currentPage, currentPageSize));
        result.setTags(shouldSearch(normalizedType, "tags")
            ? tagAppService.getTagPage(currentPage, currentPageSize, true, keyword).getItems()
            : new ArrayList<TagDTO>());
        return result;
    }

    private PageResult<ArticleDTO> searchArticles(String keyword, String category, String tag, String sort,
                                                  int page, int pageSize, Long currentUserId) {
        ArticlePageQuery query = new ArticlePageQuery(page, pageSize, keyword, category, tag, sort);
        query.setCurrentUserId(currentUserId);
        return articleAppService.pagePublishedArticles(query);
    }

    private boolean shouldSearch(String type, String target) {
        return type == null || target.equals(type);
    }

    private String normalizeType(String type) {
        if (type == null || type.trim().isEmpty() || "all".equalsIgnoreCase(type)) {
            return null;
        }
        String value = type.trim().toLowerCase();
        if ("article".equals(value)) {
            return "articles";
        }
        if ("topic".equals(value)) {
            return "topics";
        }
        if ("column".equals(value)) {
            return "columns";
        }
        if ("tag".equals(value)) {
            return "tags";
        }
        if ("articles".equals(value) || "topics".equals(value) || "columns".equals(value) || "tags".equals(value)) {
            return value;
        }
        return null;
    }

    private <T> PageResult<T> emptyPage(int page, int pageSize) {
        return new PageResult<T>(new ArrayList<T>(), page, pageSize, 0);
    }
}
