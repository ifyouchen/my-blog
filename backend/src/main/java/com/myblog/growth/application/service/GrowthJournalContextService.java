package com.myblog.growth.application.service;

import com.myblog.growth.domain.model.aggregate.UnlockOrder;
import com.myblog.growth.domain.model.valueobject.ArticleUnlockInfo;
import com.myblog.growth.domain.model.valueobject.PointJournal;
import com.myblog.growth.domain.repository.ArticleUnlockInfoRepository;
import com.myblog.growth.domain.repository.UnlockOrderRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 流水展示上下文查询服务.
 */
@Service
public class GrowthJournalContextService {

    private static final String SOURCE_UNLOCK = "UNLOCK";
    private static final String SOURCE_ARTICLE_UNLOCK = "ARTICLE_UNLOCK";
    private static final String SOURCE_REVENUE_SHARE = "REVENUE_SHARE";
    private static final String REVENUE_SHARE_BIZ_PREFIX = SOURCE_REVENUE_SHARE + ":";
    private static final String STATUS_PUBLISHED = "PUBLISHED";

    private final UnlockOrderRepository unlockOrderRepository;
    private final ArticleUnlockInfoRepository articleUnlockInfoRepository;

    public GrowthJournalContextService(UnlockOrderRepository unlockOrderRepository,
                                       ArticleUnlockInfoRepository articleUnlockInfoRepository) {
        this.unlockOrderRepository = unlockOrderRepository;
        this.articleUnlockInfoRepository = articleUnlockInfoRepository;
    }

    /**
     * 为积分流水批量解析文章上下文，返回值以积分流水 bizNo 为 key.
     */
    public Map<String, ArticleContext> resolvePointJournalContexts(List<PointJournal> journals) {
        if (journals == null || journals.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> journalBizNoToOrderNo = new HashMap<>();
        Set<String> orderNos = new LinkedHashSet<>();
        for (PointJournal journal : journals) {
            String orderNo = resolveOrderNo(journal.getSourceType(), journal.getBizNo());
            if (isBlank(orderNo) || isBlank(journal.getBizNo())) {
                continue;
            }
            journalBizNoToOrderNo.put(journal.getBizNo(), orderNo);
            orderNos.add(orderNo);
        }
        if (orderNos.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, UnlockOrder> ordersByNo = new HashMap<>();
        for (UnlockOrder order : unlockOrderRepository.findByOrderNos(orderNos)) {
            ordersByNo.put(order.getOrderNo(), order);
        }

        Set<Long> articleIds = new LinkedHashSet<>();
        for (UnlockOrder order : ordersByNo.values()) {
            if (order.getArticleId() != null) {
                articleIds.add(order.getArticleId());
            }
        }
        Map<Long, ArticleContext> articleContexts = resolveArticleContexts(articleIds);

        Map<String, ArticleContext> result = new HashMap<>();
        for (Map.Entry<String, String> entry : journalBizNoToOrderNo.entrySet()) {
            UnlockOrder order = ordersByNo.get(entry.getValue());
            if (order == null || order.getArticleId() == null) {
                continue;
            }
            ArticleContext context = articleContexts.get(order.getArticleId());
            result.put(entry.getKey(), context == null ? ArticleContext.missing(order.getArticleId()) : context);
        }
        return result;
    }

    /**
     * 为文章 ID 批量解析文章上下文.
     */
    public Map<Long, ArticleContext> resolveArticleContexts(Collection<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<Long> distinctIds = new LinkedHashSet<>();
        for (Long articleId : articleIds) {
            if (articleId != null) {
                distinctIds.add(articleId);
            }
        }
        if (distinctIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, ArticleUnlockInfo> articleInfos = articleUnlockInfoRepository.findByArticleIds(distinctIds);
        Map<Long, ArticleContext> result = new HashMap<>();
        for (Long articleId : distinctIds) {
            ArticleUnlockInfo info = articleInfos.get(articleId);
            result.put(articleId, info == null ? ArticleContext.missing(articleId) : ArticleContext.from(info));
        }
        return result;
    }

    private String resolveOrderNo(String sourceType, String bizNo) {
        if (isBlank(sourceType) || isBlank(bizNo)) {
            return null;
        }
        if (SOURCE_UNLOCK.equals(sourceType) || SOURCE_ARTICLE_UNLOCK.equals(sourceType)) {
            return bizNo;
        }
        if (SOURCE_REVENUE_SHARE.equals(sourceType) && bizNo.startsWith(REVENUE_SHARE_BIZ_PREFIX)) {
            return bizNo.substring(REVENUE_SHARE_BIZ_PREFIX.length());
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 文章展示上下文.
     */
    public static class ArticleContext {
        private final Long articleId;
        private final String articleTitle;
        private final String articleStatus;
        private final boolean articleAccessible;

        private ArticleContext(Long articleId, String articleTitle, String articleStatus,
                               boolean articleAccessible) {
            this.articleId = articleId;
            this.articleTitle = articleTitle;
            this.articleStatus = articleStatus;
            this.articleAccessible = articleAccessible;
        }

        public static ArticleContext from(ArticleUnlockInfo info) {
            return new ArticleContext(
                    info.getId(),
                    info.getTitle(),
                    info.getStatus(),
                    STATUS_PUBLISHED.equals(info.getStatus())
            );
        }

        public static ArticleContext missing(Long articleId) {
            return new ArticleContext(articleId, null, null, false);
        }

        public Long getArticleId() { return articleId; }
        public String getArticleTitle() { return articleTitle; }
        public String getArticleStatus() { return articleStatus; }
        public boolean isArticleAccessible() { return articleAccessible; }
    }
}
