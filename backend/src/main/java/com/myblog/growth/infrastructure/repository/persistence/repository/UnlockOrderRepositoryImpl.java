package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.myblog.growth.domain.model.aggregate.UnlockOrder;
import com.myblog.growth.domain.repository.UnlockOrderRepository;
import com.myblog.growth.infrastructure.repository.persistence.entity.ArticleUnlockOrderDO;
import com.myblog.growth.infrastructure.repository.persistence.mapper.ArticleUnlockOrderMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 解锁订单 Repository 实现.
 */
@Repository
public class UnlockOrderRepositoryImpl implements UnlockOrderRepository {

    private final ArticleUnlockOrderMapper mapper;

    /**
     * 构造注入 Mapper.
     *
     * @param mapper 文章解锁订单 Mapper
     */
    public UnlockOrderRepositoryImpl(ArticleUnlockOrderMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int insertIgnore(UnlockOrder order) {
        ArticleUnlockOrderDO do_ = toDO(order);
        int rows = mapper.insertIgnore(do_);
        if (rows > 0 && do_.getId() != null) {
            order.setId(do_.getId());
        }
        return rows;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<UnlockOrder> findByOrderNo(String orderNo) {
        ArticleUnlockOrderDO do_ = mapper.selectByOrderNo(orderNo);
        return Optional.ofNullable(do_).map(this::toDomain);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<UnlockOrder> findByUserIdAndArticleId(Long userId, Long articleId) {
        ArticleUnlockOrderDO do_ = mapper.selectByUserIdAndArticleId(userId, articleId);
        return Optional.ofNullable(do_).map(this::toDomain);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void markSuccess(String orderNo) {
        mapper.markSuccess(orderNo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void markFailed(String orderNo, String failReason) {
        mapper.markFailed(orderNo, failReason);
    }

    // ────────────────────────── 私有转换方法 ──────────────────────────────

    private ArticleUnlockOrderDO toDO(UnlockOrder order) {
        ArticleUnlockOrderDO do_ = new ArticleUnlockOrderDO();
        do_.setId(order.getId());
        do_.setOrderNo(order.getOrderNo());
        do_.setUserId(order.getUserId());
        do_.setArticleId(order.getArticleId());
        do_.setPointsCost(order.getPointsCost());
        do_.setStatus(order.getStatus());
        do_.setFailReason(order.getFailReason());
        do_.setCreatedAt(order.getCreatedAt());
        do_.setUpdatedAt(order.getUpdatedAt());
        do_.setVersion(order.getVersion());
        return do_;
    }

    private UnlockOrder toDomain(ArticleUnlockOrderDO do_) {
        return UnlockOrder.restore(
                do_.getId(),
                do_.getOrderNo(),
                do_.getUserId(),
                do_.getArticleId(),
                do_.getPointsCost(),
                do_.getStatus(),
                do_.getFailReason(),
                do_.getCreatedAt(),
                do_.getUpdatedAt(),
                do_.getVersion()
        );
    }
}

