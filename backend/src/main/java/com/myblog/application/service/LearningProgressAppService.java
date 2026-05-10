package com.myblog.application.service;

import com.myblog.application.assembler.ArticleAssembler;
import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.LearningPathArticleDTO;
import com.myblog.application.dto.LearningProgressDTO;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.ColumnId;
import com.myblog.domain.model.valueobject.LearningPathArticle;
import com.myblog.domain.model.valueobject.TopicId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.ColumnRepository;
import com.myblog.domain.repository.LearningProgressRepository;
import com.myblog.domain.repository.TopicRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.infrastructure.repository.persistence.entity.LearningProgressDO;
import com.myblog.shared.enums.ArticleStatus;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 专题/专栏学习进度应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class LearningProgressAppService {

    public static final String ASSET_TYPE_TOPIC = "TOPIC";
    public static final String ASSET_TYPE_COLUMN = "COLUMN";

    private final LearningProgressRepository learningProgressRepository;
    private final TopicRepository topicRepository;
    private final ColumnRepository columnRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final ArticleAssembler articleAssembler;

    public LearningProgressAppService(LearningProgressRepository learningProgressRepository,
                                      TopicRepository topicRepository,
                                      ColumnRepository columnRepository,
                                      ArticleRepository articleRepository,
                                      UserRepository userRepository,
                                      ArticleAssembler articleAssembler) {
        this.learningProgressRepository = learningProgressRepository;
        this.topicRepository = topicRepository;
        this.columnRepository = columnRepository;
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
        this.articleAssembler = articleAssembler;
    }

    public LearningProgressDTO buildProgress(String assetType, Long assetId,
                                             List<LearningPathArticle> relations, Long userId) {
        Set<Long> completedIds = readCompletedIds(assetType, assetId, relations, userId);
        return buildProgressDTO(assetType, assetId, relations, completedIds);
    }

    public List<LearningPathArticleDTO> buildOutline(String assetType, Long assetId,
                                                     List<LearningPathArticle> relations, Long userId) {
        Set<Long> completedIds = readCompletedIds(assetType, assetId, relations, userId);
        List<LearningPathArticleDTO> outline = new ArrayList<LearningPathArticleDTO>();
        for (LearningPathArticle relation : relations) {
            ArticleDTO article = toArticleDTO(relation.getArticleId());
            if (article == null) {
                continue;
            }
            LearningPathArticleDTO item = new LearningPathArticleDTO();
            item.setArticle(article);
            item.setSectionTitle(defaultSection(relation.getSectionTitle()));
            item.setStepOrder(relation.getStepOrder());
            item.setRequired(relation.isRequired());
            item.setEditorNote(relation.getEditorNote());
            item.setCompleted(completedIds.contains(relation.getArticleId()));
            outline.add(item);
        }
        return outline;
    }

    @Transactional(rollbackFor = Exception.class)
    public LearningProgressDTO updateProgress(Long userId, String assetType, Long assetId,
                                              Long articleId, Boolean completed) {
        String normalizedAssetType = normalizeAssetType(assetType);
        if (assetId == null || articleId == null) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "学习资产和文章不能为空");
        }
        List<LearningPathArticle> relations = loadRelations(normalizedAssetType, assetId);
        ensureArticleInAsset(articleId, relations);

        LearningProgressDO progress = learningProgressRepository.findByUserAndAsset(
            userId,
            normalizedAssetType,
            assetId
        );
        if (progress == null) {
            progress = new LearningProgressDO();
            progress.setUserId(userId);
            progress.setAssetType(normalizedAssetType);
            progress.setAssetId(assetId);
            progress.setCompletedArticleIds("");
        }
        Set<Long> completedIds = parseCompletedIds(progress.getCompletedArticleIds(), relations);
        if (Boolean.FALSE.equals(completed)) {
            completedIds.remove(articleId);
        } else {
            completedIds.add(articleId);
        }

        applyProgress(progress, relations, completedIds, articleId);
        learningProgressRepository.save(progress);
        return buildProgressDTO(normalizedAssetType, assetId, relations, completedIds);
    }

    private List<LearningPathArticle> loadRelations(String assetType, Long assetId) {
        if (ASSET_TYPE_TOPIC.equals(assetType)) {
            return topicRepository.findArticleRelations(new TopicId(assetId));
        }
        return columnRepository.findArticleRelations(new ColumnId(assetId));
    }

    private void ensureArticleInAsset(Long articleId, List<LearningPathArticle> relations) {
        for (LearningPathArticle relation : relations) {
            if (articleId.equals(relation.getArticleId())) {
                return;
            }
        }
        throw new ApplicationException(ErrorCode.PARAM_ERROR, "文章不属于当前学习资产");
    }

    private Set<Long> readCompletedIds(String assetType, Long assetId,
                                       List<LearningPathArticle> relations, Long userId) {
        if (userId == null) {
            return new LinkedHashSet<Long>();
        }
        LearningProgressDO progress = learningProgressRepository.findByUserAndAsset(
            userId,
            normalizeAssetType(assetType),
            assetId
        );
        return parseCompletedIds(progress == null ? null : progress.getCompletedArticleIds(), relations);
    }

    private LearningProgressDTO buildProgressDTO(String assetType, Long assetId,
                                                 List<LearningPathArticle> relations, Set<Long> completedIds) {
        int totalCount = relations == null ? 0 : relations.size();
        int completedCount = Math.min(completedIds.size(), totalCount);
        int percent = totalCount == 0 ? 0 : (int) Math.floor(completedCount * 100.0d / totalCount);
        Long nextArticleId = resolveNextArticleId(relations, completedIds);

        LearningProgressDTO dto = new LearningProgressDTO();
        dto.setAssetType(normalizeAssetType(assetType));
        dto.setAssetId(assetId);
        dto.setCompletedCount(completedCount);
        dto.setTotalCount(totalCount);
        dto.setProgressPercent(percent);
        dto.setCompletedArticleIds(new ArrayList<Long>(completedIds));
        dto.setNextArticleId(nextArticleId);
        dto.setNextArticle(nextArticleId == null ? null : toArticleDTO(nextArticleId));
        return dto;
    }

    private void applyProgress(LearningProgressDO progress, List<LearningPathArticle> relations,
                               Set<Long> completedIds, Long lastArticleId) {
        int totalCount = relations == null ? 0 : relations.size();
        int completedCount = Math.min(completedIds.size(), totalCount);
        int percent = totalCount == 0 ? 0 : (int) Math.floor(completedCount * 100.0d / totalCount);
        progress.setCompletedArticleIds(joinIds(completedIds));
        progress.setCompletedCount(completedCount);
        progress.setProgressPercent(percent);
        progress.setLastArticleId(lastArticleId);
        progress.setLastReadAt(LocalDateTime.now());
    }

    private Set<Long> parseCompletedIds(String source, List<LearningPathArticle> relations) {
        Set<Long> allowedIds = new LinkedHashSet<Long>();
        if (relations != null) {
            for (LearningPathArticle relation : relations) {
                allowedIds.add(relation.getArticleId());
            }
        }
        Set<Long> ids = new LinkedHashSet<Long>();
        if (!StringUtils.hasText(source)) {
            return ids;
        }
        String[] parts = source.split(",");
        for (String part : parts) {
            try {
                Long id = Long.valueOf(part.trim());
                if (allowedIds.contains(id)) {
                    ids.add(id);
                }
            } catch (NumberFormatException ignored) {
                // 历史脏数据直接跳过
            }
        }
        return ids;
    }

    private String joinIds(Set<Long> ids) {
        StringBuilder builder = new StringBuilder();
        for (Long id : ids) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(id);
        }
        return builder.toString();
    }

    private Long resolveNextArticleId(List<LearningPathArticle> relations, Set<Long> completedIds) {
        if (relations == null || relations.isEmpty()) {
            return null;
        }
        for (LearningPathArticle relation : relations) {
            if (!completedIds.contains(relation.getArticleId())) {
                return relation.getArticleId();
            }
        }
        return relations.get(relations.size() - 1).getArticleId();
    }

    private ArticleDTO toArticleDTO(Long articleId) {
        Article article = articleRepository.findById(new ArticleId(articleId)).orElse(null);
        if (article == null || !ArticleStatus.PUBLISHED.equals(article.getStatus())) {
            return null;
        }
        User author = userRepository.findById(article.getAuthorId()).orElse(null);
        return author == null ? null : articleAssembler.toDTO(article, author);
    }

    private String defaultSection(String value) {
        return StringUtils.hasText(value) ? value : "推荐阅读";
    }

    private String normalizeAssetType(String assetType) {
        String normalized = assetType == null ? "" : assetType.trim().toUpperCase();
        if (ASSET_TYPE_TOPIC.equals(normalized) || ASSET_TYPE_COLUMN.equals(normalized)) {
            return normalized;
        }
        throw new ApplicationException(ErrorCode.PARAM_ERROR, "学习资产类型不正确");
    }
}
