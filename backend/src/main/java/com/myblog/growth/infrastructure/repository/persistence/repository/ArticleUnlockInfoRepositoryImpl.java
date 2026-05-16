package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.myblog.growth.domain.model.valueobject.ArticleUnlockInfo;
import com.myblog.growth.domain.repository.ArticleUnlockInfoRepository;
import com.myblog.growth.infrastructure.repository.persistence.mapper.ArticleUnlockInfoMapper;
import com.myblog.growth.infrastructure.repository.persistence.mapper.ArticleUnlockInfoMapper.ArticleUnlockInfoVO;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 文章解锁信息查询 Repository 实现.
 */
@Repository
public class ArticleUnlockInfoRepositoryImpl implements ArticleUnlockInfoRepository {

    private final ArticleUnlockInfoMapper mapper;

    public ArticleUnlockInfoRepositoryImpl(ArticleUnlockInfoMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<ArticleUnlockInfo> findByArticleId(Long articleId) {
        ArticleUnlockInfoVO vo = mapper.selectUnlockInfoById(articleId);
        if (vo == null) {
            return Optional.empty();
        }
        return Optional.of(ArticleUnlockInfo.of(
                vo.getId(),
                vo.isNeedUnlock(),
                vo.getUnlockPointPrice(),
                vo.getAuthorId(),
                vo.getStatus()
        ));
    }
}
