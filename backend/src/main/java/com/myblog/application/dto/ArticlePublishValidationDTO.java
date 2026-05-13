package com.myblog.application.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 文章发布校验结果。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class ArticlePublishValidationDTO {

    /** 是否可发布 */
    private boolean publishable;
    /** 校验错误列表 */
    private List<String> errors = new ArrayList<String>();
    /** 校验警告列表 */
    private List<String> warnings = new ArrayList<String>();
    /** 校验项详情列表 */
    private List<Map<String, Object>> checks = new ArrayList<Map<String, Object>>();

    /**
     * 获取是否可发布。
     *
     * @return 是否可发布
     */
    public boolean isPublishable() {
        return publishable;
    }

    /**
     * 设置是否可发布。
     *
     * @param publishable 是否可发布
     */
    public void setPublishable(boolean publishable) {
        this.publishable = publishable;
    }

    /**
     * 获取校验错误列表。
     *
     * @return 校验错误列表
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * 设置校验错误列表。
     *
     * @param errors 校验错误列表
     */
    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    /**
     * 获取校验警告列表。
     *
     * @return 校验警告列表
     */
    public List<String> getWarnings() {
        return warnings;
    }

    /**
     * 设置校验警告列表。
     *
     * @param warnings 校验警告列表
     */
    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    /**
     * 获取校验项详情列表。
     *
     * @return 校验项详情列表
     */
    public List<Map<String, Object>> getChecks() {
        return checks;
    }

    /**
     * 设置校验项详情列表。
     *
     * @param checks 校验项详情列表
     */
    public void setChecks(List<Map<String, Object>> checks) {
        this.checks = checks;
    }
}
