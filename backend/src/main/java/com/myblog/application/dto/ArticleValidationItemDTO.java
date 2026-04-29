package com.myblog.application.dto;

/**
 * 文章发布校验项。
 *
 * @author Codex
 * @since 1.0.0
 */
public class ArticleValidationItemDTO {

    private String key;
    private String label;
    private boolean passed;
    private String level;
    private String message;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
