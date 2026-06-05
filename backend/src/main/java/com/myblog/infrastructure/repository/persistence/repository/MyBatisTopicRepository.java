package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.model.aggregate.Topic;
import com.myblog.domain.model.valueobject.LearningPathArticle;
import com.myblog.domain.model.valueobject.TopicId;
import com.myblog.domain.repository.TopicRepository;
import com.myblog.infrastructure.repository.persistence.converter.TopicPersistenceConverter;
import com.myblog.infrastructure.repository.persistence.entity.LearningPathArticleDO;
import com.myblog.infrastructure.repository.persistence.entity.TopicDO;
import com.myblog.infrastructure.repository.persistence.mapper.TopicMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 专题 MyBatis 仓储实现。
 *
 * @author Codex
 * @since 1.0.0
 */
@Repository
public class MyBatisTopicRepository implements TopicRepository {

    private final TopicMapper topicMapper;
    private final IdGenerator idGenerator;

    /**
     * 创建专题 MyBatis 仓储。
     *
     * @param topicMapper 专题 Mapper
     * @param idGenerator 全局 ID 生成器
     */
    public MyBatisTopicRepository(TopicMapper topicMapper, IdGenerator idGenerator) {
        this.topicMapper = topicMapper;
        this.idGenerator = idGenerator;
    }

    /**
     * 根据专题 ID 查询专题。
     *
     * @param topicId 专题 ID
     * @return 专题 Optional
     */
    @Override
    public Optional<Topic> findById(TopicId topicId) {
        TopicDO topicDO = topicMapper.selectById(topicId.getValue());
        return topicDO == null ? Optional.empty() : Optional.of(toDomain(topicDO));
    }

    /**
     * 分页查询已发布专题。
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @return 专题列表
     */
    @Override
    public List<Topic> findPublished(int page, int pageSize) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        List<TopicDO> topicDOList = topicMapper.selectPublished((currentPage - 1) * currentPageSize, currentPageSize);
        List<Topic> topics = new ArrayList<>(topicDOList.size());
        for (TopicDO topicDO : topicDOList) {
            topics.add(toDomain(topicDO));
        }
        return topics;
    }

    /**
     * 统计已发布专题数量。
     *
     * @return 已发布专题数量
     */
    @Override
    public long countPublished() {
        return topicMapper.countPublished();
    }

    /**
     * 保存专题。
     *
     * @param topic 专题聚合根
     * @return 保存后的专题
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Topic save(Topic topic) {
        TopicDO topicDO = toData(topic);
        topicMapper.insertOrUpdate(topicDO);
        return topic;
    }

    /**
     * 生成下一个专题 ID。
     *
     * @return 专题 ID
     */
    @Override
    public Long nextId() {
        return idGenerator.nextId("blog_topic");
    }

    /**
     * 查询专题下的文章 ID 列表。
     *
     * @param topicId 专题 ID
     * @return 文章 ID 列表
     */
    @Override
    public List<Long> findArticleIds(TopicId topicId) {
        return topicMapper.selectArticleIds(topicId.getValue());
    }

    /**
     * 查询专题下文章关联关系（含学习路径信息）。
     *
     * @param topicId 专题 ID
     * @return 学习路径文章列表
     */
    @Override
    public List<LearningPathArticle> findArticleRelations(TopicId topicId) {
        List<LearningPathArticleDO> list = topicMapper.selectArticleRelations(topicId.getValue());
        List<LearningPathArticle> result = new ArrayList<>(list.size());
        for (LearningPathArticleDO item : list) {
            result.add(toPathArticle(item));
        }
        return result;
    }

    /**
     * 搜索已发布专题。
     *
     * @param keyword    关键字
     * @param difficulty 难度
     * @param page       页码
     * @param pageSize   每页大小
     * @return 专题列表
     */
    @Override
    public List<Topic> searchPublished(String keyword, String difficulty, int page, int pageSize) {
        int p = Math.max(page, 1);
        int ps = Math.max(pageSize, 1);
        List<TopicDO> list = topicMapper.searchPublished(keyword, difficulty, (p - 1) * ps, ps);
        List<Topic> topics = new ArrayList<>(list.size());
        for (TopicDO topicDO : list) {
            topics.add(toDomain(topicDO));
        }
        return topics;
    }

    /**
     * 统计已发布专题搜索数量。
     *
     * @param keyword    关键字
     * @param difficulty 难度
     * @return 专题数量
     */
    @Override
    public long countSearchPublished(String keyword, String difficulty) {
        return topicMapper.countSearchPublished(keyword, difficulty);
    }

    /**
     * 绑定文章到专题（已存在时跳过）。
     *
     * @param topicId   专题 ID
     * @param articleId 文章 ID
     * @param sortOrder 排序
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindArticle(TopicId topicId, Long articleId, int sortOrder) {
        if (topicMapper.countTopicArticle(topicId.getValue(), articleId) > 0) {
            if (topicMapper.countActiveTopicArticle(topicId.getValue(), articleId) > 0) {
                topicMapper.updateTopicArticleSort(topicId.getValue(), articleId, sortOrder);
            } else if (topicMapper.restoreTopicArticle(topicId.getValue(), articleId, sortOrder) > 0) {
                topicMapper.incrementArticleCount(topicId.getValue());
            }
            return;
        }
        topicMapper.insertTopicArticle(topicId.getValue(), articleId, sortOrder);
        topicMapper.incrementArticleCount(topicId.getValue());
    }

    /**
     * 从专题解绑文章。
     *
     * @param topicId   专题 ID
     * @param articleId 文章 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindArticle(TopicId topicId, Long articleId) {
        topicMapper.deleteTopicArticle(topicId.getValue(), articleId);
        topicMapper.decrementArticleCount(topicId.getValue());
    }

    /**
     * 原子递增专题文章数。
     *
     * @param topicId 专题 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementArticleCount(TopicId topicId) {
        topicMapper.incrementArticleCount(topicId.getValue());
    }

    /**
     * 后台管理分页查询所有专题。
     *
     * @param keyword  关键字
     * @param page     页码
     * @param pageSize 每页大小
     * @return 专题列表
     */
    @Override
    public List<Topic> findAll(String keyword, int page, int pageSize) {
        int p = Math.max(page, 1);
        int ps = Math.max(pageSize, 1);
        List<TopicDO> list = topicMapper.selectAll(keyword, (p - 1) * ps, ps);
        List<Topic> topics = new ArrayList<>(list.size());
        for (TopicDO d : list) {
            topics.add(toDomain(d));
        }
        return topics;
    }

    /**
     * 统计所有专题数量。
     *
     * @param keyword 关键字
     * @return 专题数量
     */
    @Override
    public long countAll(String keyword) {
        return topicMapper.countAll(keyword);
    }

    /**
     * 软删除专题。
     *
     * @param topicId 专题 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void softDelete(TopicId topicId) {
        topicMapper.softDelete(topicId.getValue());
    }

    /**
     * 按近期活跃度查询热门专题。
     *
     * @param withinDays 统计天数
     * @param limit      返回数量
     * @return 热门专题列表
     */
    @Override
    public List<Topic> findHotByActivity(int withinDays, int limit) {
        int days = Math.max(withinDays, 1);
        int lim = Math.max(limit, 1);
        List<TopicDO> list = topicMapper.selectHotByActivity(days, lim);
        List<Topic> topics = new ArrayList<>(list.size());
        for (TopicDO d : list) {
            topics.add(toDomain(d));
        }
        return topics;
    }

    /**
     * 将 DO 转换为领域对象。
     *
     * @param topicDO 专题数据对象
     * @return 专题聚合根
     */
    private Topic toDomain(TopicDO topicDO) {
        return TopicPersistenceConverter.toDomain(topicDO);
    }

    /**
     * 将领域对象转换为 DO，并补充时间戳与版本号。
     *
     * @param topic 专题聚合根
     * @return 专题数据对象
     */
    private TopicDO toData(Topic topic) {
        TopicDO topicDO = TopicPersistenceConverter.toData(topic);
        if (topicDO.getCreatedAt() == null) {
            topicDO.setCreatedAt(LocalDateTime.now());
        }
        topicDO.setUpdatedAt(LocalDateTime.now());
        if (topicDO.getVersion() == null) {
            topicDO.setVersion(0);
        }
        return topicDO;
    }

    /**
     * 将学习路径文章 DO 转换为领域值对象。
     *
     * @param item 学习路径文章数据对象
     * @return 学习路径文章值对象
     */
    private LearningPathArticle toPathArticle(LearningPathArticleDO item) {
        return new LearningPathArticle(
            item.getArticleId(),
            item.getSectionTitle(),
            item.getStepOrder() == null ? 0 : item.getStepOrder(),
            item.getRequired() == null || item.getRequired(),
            item.getEditorNote()
        );
    }
}
