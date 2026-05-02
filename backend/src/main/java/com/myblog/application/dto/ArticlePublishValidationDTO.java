package com.myblog.application.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 文章发布校验结果。
 *
 * @author Codex
 * @since 1.0.0
 */
public class ArticlePublishValidationDTO {

    private boolean publishable;
    private List<String> errors = new ArrayList<String>();
    private List<String> warnings = new ArrayList<String>();
    private List<Map<String, Object>> checks = new ArrayList<Map<String, Object>>();

    public boolean isPublishable() {
        return publishable;
    }

    public void setPublishable(boolean publishable) {
        this.publishable = publishable;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public List<Map<String, Object>> getChecks() {
        return checks;
    }

    public void setChecks(List<Map<String, Object>> checks) {
        this.checks = checks;
    }
}
