package com.myblog.growth.application.service;

import com.myblog.growth.domain.repository.UnlockRelationRepository;
import com.myblog.shared.enums.UserRole;
import org.springframework.stereotype.Service;

/**
 * 文章付费内容访问控制服务.
 *
 * <p>集中管理付费文章的全文访问权限判断逻辑，统一以下场景：
 * <ul>
 *   <li>免费文章 → 允许访问</li>
 *   <li>未登录游客 → 拒绝（返回 {@code CONTENT_LOCKED}）</li>
 *   <li>作者本人 → 允许（{@code AUTHOR_SELF}）</li>
 *   <li>管理员 → 允许（{@code ADMIN_BYPASS}）</li>
 *   <li>已解锁用户 → 允许（{@code UNLOCKED}）</li>
 *   <li>其他 → 拒绝（{@code CONTENT_LOCKED}）</li>
 * </ul>
 * </p>
 */
@Service
public class ArticleContentAccessService {

    /** 作者本人可直接阅读. */
    public static final String REASON_AUTHOR_SELF = "AUTHOR_SELF";

    /** 管理员可免积分查看. */
    public static final String REASON_ADMIN_BYPASS = "ADMIN_BYPASS";

    /** 已通过积分解锁. */
    public static final String REASON_UNLOCKED = "UNLOCKED";

    /** 内容被锁定，需解锁. */
    public static final String REASON_CONTENT_LOCKED = "CONTENT_LOCKED";

    private final UnlockRelationRepository unlockRelationRepository;

    public ArticleContentAccessService(UnlockRelationRepository unlockRelationRepository) {
        this.unlockRelationRepository = unlockRelationRepository;
    }

    /**
     * 检查当前用户是否有权查看付费文章的全文内容.
     *
     * @param articleId      文章 ID
     * @param authorId       作者 ID
     * @param needUnlock     文章是否需要解锁
     * @param currentUserId  当前用户 ID（未登录为 null）
     * @param currentUserRole 当前用户角色（未登录为 null）
     * @return 访问结果
     */
    public AccessResult checkAccess(Long articleId, Long authorId, boolean needUnlock,
                                    Long currentUserId, String currentUserRole) {
        // ① 免费文章 → 直接允许
        if (!needUnlock) {
            return AccessResult.allowed();
        }

        // ② 未登录 → 拒绝
        if (currentUserId == null) {
            return AccessResult.denied(REASON_CONTENT_LOCKED);
        }

        // ③ 作者本人 → 允许
        if (currentUserId.equals(authorId)) {
            return AccessResult.allowed(REASON_AUTHOR_SELF);
        }

        // ④ 管理员 → 允许
        if (UserRole.ADMIN.name().equals(currentUserRole)) {
            return AccessResult.allowed(REASON_ADMIN_BYPASS);
        }

        // ⑤ 已解锁 → 允许
        if (unlockRelationRepository.existsByUserIdAndArticleId(currentUserId, articleId)) {
            return AccessResult.allowed(REASON_UNLOCKED);
        }

        // ⑥ 其他 → 拒绝
        return AccessResult.denied(REASON_CONTENT_LOCKED);
    }

    /**
     * 访问控制结果.
     */
    public static class AccessResult {
        private final boolean canReadFullContent;
        private final String reason;

        private AccessResult(boolean canReadFullContent, String reason) {
            this.canReadFullContent = canReadFullContent;
            this.reason = reason;
        }

        /** 允许访问（免费文章）. */
        public static AccessResult allowed() {
            return new AccessResult(true, null);
        }

        /** 允许访问（带原因）. */
        public static AccessResult allowed(String reason) {
            return new AccessResult(true, reason);
        }

        /** 拒绝访问. */
        public static AccessResult denied(String reason) {
            return new AccessResult(false, reason);
        }

        public boolean isCanReadFullContent() { return canReadFullContent; }
        public String getReason() { return reason; }
    }
}
