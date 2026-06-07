package com.myblog.domain.model.valueobject;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 邮箱值对象。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class Email {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final String value;

    /**
     * 创建邮箱值对象。
     *
     * @param value 邮箱地址
     */
    public Email(String value) {
        if (value == null || value.length() > 254 || !EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
        this.value = value;
    }

    /**
     * 获取邮箱地址。
     *
     * @return 邮箱地址
     */
    public String getValue() {
        return value;
    }

    /**
     * 判断两个邮箱值对象是否相同。
     *
     * @param o 待比较对象
     * @return 是否相同
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Email)) {
            return false;
        }
        Email email = (Email) o;
        return Objects.equals(value, email.value);
    }

    /**
     * 计算邮箱哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
