package com.myblog.domain.model.valueobject;

import com.myblog.shared.exception.DomainException;
import com.myblog.shared.exception.ErrorCode;
import java.io.Serializable;
import java.util.Objects;

/**
 * 文章 ID 值对象。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class ArticleId implements Serializable {

    private final Long value;

    /**
     * 创建文章 ID。
     *
     * @param value 文章 ID 原始值
     */
    public ArticleId(Long value) {
        if (value == null || value <= 0L) {
            throw new DomainException(ErrorCode.PARAM_ERROR, "文章ID不能为空");
        }
        this.value = value;
    }

    /**
     * 获取文章 ID 原始值。
     *
     * @return 文章 ID 原始值
     */
    public Long getValue() {
        return value;
    }

    /**
     * 判断两个文章 ID 是否相同。
     *
     * @param o 待比较对象
     * @return 是否相同
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ArticleId)) {
            return false;
        }
        ArticleId articleId = (ArticleId) o;
        return Objects.equals(value, articleId.value);
    }

    /**
     * 计算文章 ID 哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
