package com.myblog.application.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 文章分页查询测试。
 *
 * @author Codex
 * @since 1.0.0
 */
class ArticlePageQueryTest {

    /**
     * 默认应使用最新排序。
     */
    @Test
    @DisplayName("非法排序值应回退到 latest")
    void shouldFallbackToLatestWhenSortIsInvalid() {
        ArticlePageQuery query = new ArticlePageQuery(1, 10, null, null, null, "unknown");

        assertThat(query.getSort()).isEqualTo(ArticlePageQuery.SORT_LATEST);
    }

    /**
     * 应保留合法排序值。
     */
    @Test
    @DisplayName("合法排序值应被保留")
    void shouldKeepValidSortValue() {
        ArticlePageQuery query = new ArticlePageQuery(1, 10, null, null, null, "featured");

        assertThat(query.getSort()).isEqualTo(ArticlePageQuery.SORT_FEATURED);
    }
}
