package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.myblog.growth.domain.repository.RevenueShareRepository;
import com.myblog.growth.infrastructure.repository.persistence.entity.RevenueShareJournalDO;
import com.myblog.growth.infrastructure.repository.persistence.mapper.RevenueShareJournalMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 分账流水 Repository 实现.
 */
@Repository
public class RevenueShareRepositoryImpl implements RevenueShareRepository {

    private final RevenueShareJournalMapper mapper;

    /**
     * 构造注入 Mapper.
     *
     * @param mapper 分账流水 Mapper
     */
    public RevenueShareRepositoryImpl(RevenueShareJournalMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int insertIgnore(String orderNo, Long articleId, Long authorId,
                            int totalPoints, int platformPoints, int authorPoints, String shareRatio) {
        RevenueShareJournalDO do_ = new RevenueShareJournalDO();
        do_.setOrderNo(orderNo);
        do_.setArticleId(articleId);
        do_.setAuthorId(authorId);
        do_.setTotalPoints(totalPoints);
        do_.setPlatformPoints(platformPoints);
        do_.setAuthorPoints(authorPoints);
        do_.setShareRatio(shareRatio);
        return mapper.insertIgnore(do_);
    }

    /**
     * 分页查询作者分账流水.
     *
     * @param authorId 作者用户 ID
     * @param page     页码（从 1 开始）
     * @param size     每页条数
     * @return 分账流水 DO 列表
     */
    public List<RevenueShareJournalDO> findPageByAuthorId(Long authorId, int page, int size) {
        int offset = (page - 1) * size;
        return mapper.selectPageByAuthorId(authorId, size, offset);
    }

    /**
     * 统计作者分账流水总数.
     *
     * @param authorId 作者用户 ID
     * @return 总数
     */
    public long countByAuthorId(Long authorId) {
        return mapper.countByAuthorId(authorId);
    }
}

