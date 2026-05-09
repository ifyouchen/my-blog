package com.myblog.application.service;

import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.TopicDTO;
import com.myblog.application.assembler.ArticleAssembler;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.Topic;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.TopicId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.TopicRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.shared.enums.ArticleStatus;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.util.BizLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 专题应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class TopicAppService {

    private static final Logger log = LoggerFactory.getLogger(TopicAppService.class);

    private final TopicRepository topicRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final ArticleAssembler articleAssembler;

    public TopicAppService(TopicRepository topicRepository,
                           ArticleRepository articleRepository,
                           UserRepository userRepository,
                           ArticleAssembler articleAssembler) {
        this.topicRepository = topicRepository;
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
        this.articleAssembler = articleAssembler;
    }

    // ==================== 前台接口 ====================

    /**
     * 查询热门专题（按排序权重取前 N 条已发布专题）。
     *
     * @param limit 返回数量
     * @return 热门专题列表
     */
    public List<TopicDTO> listHotTopics(int limit) {
        List<Topic> topics = topicRepository.findPublished(1, limit);
        List<TopicDTO> items = new ArrayList<>(topics.size());
        for (Topic topic : topics) {
            items.add(toDTO(topic));
        }
        return items;
    }


    /**
     * 分页查询专题。
     */
    public PageResult<TopicDTO> pageTopics(int page, int pageSize) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        List<Topic> topics = topicRepository.findPublished(currentPage, currentPageSize);
        List<TopicDTO> items = new ArrayList<>(topics.size());
        for (Topic topic : topics) {
            items.add(toDTO(topic));
        }
        return new PageResult<>(items, currentPage, currentPageSize, topicRepository.countPublished());
    }

    /**
     * 获取专题详情。
     */
    public TopicDTO getTopicDetail(Long topicId) {
        Topic topic = loadPublishedTopic(topicId);
        return toDTO(topic);
    }

    /**
     * 分页查询专题文章。
     */
    public PageResult<ArticleDTO> pageTopicArticles(Long topicId, int page, int pageSize) {
        loadPublishedTopic(topicId);
        List<Long> articleIds = topicRepository.findArticleIds(new TopicId(topicId));
        List<ArticleDTO> items = new ArrayList<>();
        List<Article> visibleArticles = new ArrayList<>();
        for (Long articleId : articleIds) {
            Article article = articleRepository.findById(new ArticleId(articleId)).orElse(null);
            if (article != null && ArticleStatus.PUBLISHED.equals(article.getStatus())) {
                visibleArticles.add(article);
            }
        }
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        int fromIndex = Math.min((currentPage - 1) * currentPageSize, visibleArticles.size());
        int toIndex = Math.min(fromIndex + currentPageSize, visibleArticles.size());
        for (Article article : visibleArticles.subList(fromIndex, toIndex)) {
            User author = userRepository.findById(article.getAuthorId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章作者不存在"));
            items.add(articleAssembler.toDTO(article, author));
        }
        return new PageResult<>(items, currentPage, currentPageSize, visibleArticles.size());
    }

    /**
     * 获取专题内某篇文章的上下篇。
     *
     * @param topicId   专题 ID
     * @param articleId 当前文章 ID
     * @return Map with keys "prev" and "next", values may be null
     */
    public java.util.Map<String, ArticleDTO> getTopicArticleNeighbors(Long topicId, Long articleId) {
        loadPublishedTopic(topicId);
        List<Long> articleIds = topicRepository.findArticleIds(new TopicId(topicId));
        List<Long> visibleIds = new ArrayList<>();
        for (Long id : articleIds) {
            Article article = articleRepository.findById(new ArticleId(id)).orElse(null);
            if (article != null && ArticleStatus.PUBLISHED.equals(article.getStatus())) {
                visibleIds.add(id);
            }
        }
        int idx = visibleIds.indexOf(articleId);
        java.util.Map<String, ArticleDTO> result = new java.util.LinkedHashMap<>();
        result.put("prev", idx > 0 ? toNeighborDTO(visibleIds.get(idx - 1)) : null);
        result.put("next", idx >= 0 && idx < visibleIds.size() - 1 ? toNeighborDTO(visibleIds.get(idx + 1)) : null);
        return result;
    }

    private ArticleDTO toNeighborDTO(Long articleId) {
        Article article = articleRepository.findById(new ArticleId(articleId)).orElse(null);
        if (article == null) {
            return null;
        }
        return userRepository.findById(article.getAuthorId())
            .map(author -> articleAssembler.toDTO(article, author))
            .orElse(null);
    }

    // ==================== 管理后台接口 ====================

    /**
     * 后台：分页查询所有专题（含下架）。
     */
    public PageResult<TopicDTO> adminPageTopics(String keyword, int page, int pageSize) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        List<Topic> topics = topicRepository.findAll(keyword, currentPage, currentPageSize);
        List<TopicDTO> items = new ArrayList<>(topics.size());
        for (Topic topic : topics) {
            items.add(toAdminDTO(topic));
        }
        return new PageResult<>(items, currentPage, currentPageSize, topicRepository.countAll(keyword));
    }

    /**
     * 后台：创建专题。
     */
    @Transactional(rollbackFor = Exception.class)
    public TopicDTO adminCreateTopic(String title, String summary, String coverUrl, Integer sortOrder) {
        long _start = System.currentTimeMillis();
        Topic topic = Topic.create(
            topicRepository.nextId(),
            title, summary, coverUrl, sortOrder
        );
        topicRepository.save(topic);
        TopicDTO result = toAdminDTO(topic);
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            "后台创建专题",
            BizLogHelper.params("title", title),
            BizLogHelper.result("topicId=" + result.getId()),
            BizLogHelper.elapsed(_start));
        return result;
    }

    /**
     * 后台：更新专题。
     */
    @Transactional(rollbackFor = Exception.class)
    public TopicDTO adminUpdateTopic(Long topicId, String title, String summary,
                                     String coverUrl, Integer sortOrder, String status) {
        long _start = System.currentTimeMillis();
        Topic topic = topicRepository.findById(new TopicId(topicId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "专题不存在"));
        topic.update(title, summary, coverUrl, sortOrder, status);
        topicRepository.save(topic);
        TopicDTO result = toAdminDTO(topic);
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            "后台更新专题",
            BizLogHelper.params("topicId", topicId, "title", title),
            BizLogHelper.result("topicId=" + result.getId()),
            BizLogHelper.elapsed(_start));
        return result;
    }

    /**
     * 后台：删除专题（软删除）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void adminDeleteTopic(Long topicId) {
        long _start = System.currentTimeMillis();
        Topic topic = topicRepository.findById(new TopicId(topicId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "专题不存在"));
        topic.delete();
        topicRepository.save(topic);
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            "后台删除专题",
            BizLogHelper.params("topicId", topicId),
            BizLogHelper.result("deleted=true"),
            BizLogHelper.elapsed(_start));
    }

    /**
     * 后台：专题绑定文章。
     */
    @Transactional(rollbackFor = Exception.class)
    public void adminBindArticle(Long topicId, Long articleId, Integer sortOrder) {
        long _start = System.currentTimeMillis();
        topicRepository.findById(new TopicId(topicId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "专题不存在"));
        articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
        topicRepository.bindArticle(new TopicId(topicId), articleId, sortOrder == null ? 0 : sortOrder);
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            "专题绑定文章",
            BizLogHelper.params("topicId", topicId, "articleId", articleId, "sortOrder", sortOrder),
            BizLogHelper.result("bound=true"),
            BizLogHelper.elapsed(_start));
    }

    /**
     * 后台：专题解绑文章。
     */
    @Transactional(rollbackFor = Exception.class)
    public void adminUnbindArticle(Long topicId, Long articleId) {
        long _start = System.currentTimeMillis();
        topicRepository.unbindArticle(new TopicId(topicId), articleId);
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            "专题解绑文章",
            BizLogHelper.params("topicId", topicId, "articleId", articleId),
            BizLogHelper.result("unbound=true"),
            BizLogHelper.elapsed(_start));
    }

    private Topic loadPublishedTopic(Long topicId) {
        Topic topic = topicRepository.findById(new TopicId(topicId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "专题不存在"));
        if (!topic.isPublished()) {
            throw new ApplicationException(ErrorCode.NOT_FOUND, "专题不存在");
        }
        return topic;
    }

    private TopicDTO toDTO(Topic topic) {
        TopicDTO dto = new TopicDTO();
        dto.setId(topic.getId().getValue());
        dto.setTitle(topic.getTitle());
        dto.setSummary(topic.getSummary());
        dto.setCoverUrl(topic.getCoverUrl());
        dto.setArticleCount(topic.getArticleCount());
        return dto;
    }

    private TopicDTO toAdminDTO(Topic topic) {
        TopicDTO dto = new TopicDTO();
        dto.setId(topic.getId().getValue());
        dto.setTitle(topic.getTitle());
        dto.setSummary(topic.getSummary());
        dto.setCoverUrl(topic.getCoverUrl());
        dto.setArticleCount(topic.getArticleCount());
        dto.setStatus(topic.getStatus());
        dto.setSortOrder(topic.getSortOrder());
        dto.setCreatedAt(topic.getCreatedAt());
        return dto;
    }
}
