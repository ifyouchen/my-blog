package com.myblog.growth.infrastructure.repository.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 文章解锁信息只读 Mapper.
 *
 * <p>
 * 成长模块通过此 Mapper 直接查询 {@code blog_article} 表中的解锁相关字段，
 * 不修改文章核心表，不调用博客应用服务。
 * </p>
 *
 * <p>对应字段：</p>
 * <ul>
 *   <li>{@code need_unlock} — 是否需要解锁（0=免费，1=收费）</li>
 *   <li>{@code unlock_point_price} — 解锁所需积分数</li>
 *   <li>{@code author_id} — 作者用户 ID（用于分账）</li>
 * </ul>
 */
@Mapper
public interface ArticleUnlockInfoMapper {

    /**
     * 查询文章的解锁信息.
     *
     * @param articleId 文章 ID
     * @return 解锁信息 VO，文章不存在时返回 null
     */
    ArticleUnlockInfoVO selectUnlockInfoById(@Param("articleId") Long articleId);

    /**
     * 文章解锁信息值对象（Mapper 内嵌使用）.
     */
    class ArticleUnlockInfoVO {

        /** 文章 ID. */
        private Long id;

        /** 是否需要解锁（1=需要，0=免费）. */
        private boolean needUnlock;

        /** 解锁所需积分数. */
        private int unlockPointPrice;

        /** 作者用户 ID. */
        private Long authorId;

        /** 文章状态（PUBLISHED 才能解锁）. */
        private String status;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public boolean isNeedUnlock() { return needUnlock; }
        public void setNeedUnlock(boolean needUnlock) { this.needUnlock = needUnlock; }

        public int getUnlockPointPrice() { return unlockPointPrice; }
        public void setUnlockPointPrice(int unlockPointPrice) { this.unlockPointPrice = unlockPointPrice; }

        public Long getAuthorId() { return authorId; }
        public void setAuthorId(Long authorId) { this.authorId = authorId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}

