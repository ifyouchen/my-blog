package com.myblog.domain.model.aggregate;

import com.myblog.domain.model.valueobject.UserId;
import com.myblog.shared.enums.ArticleStatus;
import com.myblog.shared.exception.DomainException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArticleUnlockRuleTest {

    @Test
    void paidUnlockRuleRequiresAtLeastTenPoints() {
        assertThatThrownBy(() -> createArticle(true, Article.MIN_UNLOCK_POINT_PRICE - 1))
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("解锁积分不能低于 " + Article.MIN_UNLOCK_POINT_PRICE);
    }

    @Test
    void paidUnlockRuleAcceptsMinimumPointsAndFreeArticleCanStayZero() {
        Article paidArticle = createArticle(true, Article.MIN_UNLOCK_POINT_PRICE);
        Article freeArticle = createArticle(false, 0);

        assertThat(paidArticle.isNeedUnlock()).isTrue();
        assertThat(paidArticle.getUnlockPointPrice()).isEqualTo(Article.MIN_UNLOCK_POINT_PRICE);
        assertThat(freeArticle.isNeedUnlock()).isFalse();
        assertThat(freeArticle.getUnlockPointPrice()).isZero();
    }

    private Article createArticle(boolean needUnlock, int unlockPointPrice) {
        return Article.create(
            950009L,
            new UserId(2L),
            "测试文章",
            "摘要",
            "这里是文章正文",
            null,
            "Java",
            Collections.singletonList("后端"),
            ArticleStatus.DRAFT,
            null,
            null,
            null,
            needUnlock,
            unlockPointPrice,
            (LocalDateTime) null
        );
    }
}
