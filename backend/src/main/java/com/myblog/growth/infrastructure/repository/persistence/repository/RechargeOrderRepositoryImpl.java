package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.myblog.growth.domain.repository.RechargeOrderRepository;
import com.myblog.growth.infrastructure.repository.persistence.mapper.RechargeOrderMapper;
import org.springframework.stereotype.Repository;

/**
 * 充值订单 Repository 实现.
 */
@Repository
public class RechargeOrderRepositoryImpl implements RechargeOrderRepository {

    private final RechargeOrderMapper mapper;

    /**
     * 构造注入 Mapper.
     *
     * @param mapper 充值订单 Mapper
     */
    public RechargeOrderRepositoryImpl(RechargeOrderMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String findStatusByPayOrderNo(String payOrderNo) {
        return mapper.selectStatusByPayOrderNo(payOrderNo);
    }

    /**
     * {@inheritDoc}
     * <p>注意：channel 参数当前版本暂不存表，如需存储请在表中添加字段。</p>
     */
    @Override
    public int insertIgnore(Long userId, String payOrderNo, int amount, int pointAmount, String channel) {
        return mapper.insertIgnore(userId, payOrderNo, amount, pointAmount);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void markSuccess(String payOrderNo) {
        mapper.markSuccess(payOrderNo);
    }
}

