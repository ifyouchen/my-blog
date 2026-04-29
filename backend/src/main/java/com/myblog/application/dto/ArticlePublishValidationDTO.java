package com.myblog.application.dto;

import java.util.ArrayList;
import java.util.List;

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
    private List<ArticleValidationItemDTO> checks = new ArrayList<ArticleValidationItemDTO>();

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

    public List<ArticleValidationItemDTO> getChecks() {
        return checks;
    }

    public void setChecks(List<ArticleValidationItemDTO> checks) {
        this.checks = checks;
    }
}
