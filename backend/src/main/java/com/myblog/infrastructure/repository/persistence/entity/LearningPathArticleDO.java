package com.myblog.infrastructure.repository.persistence.entity;

/**
 * 专题/专栏文章编排数据对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class LearningPathArticleDO {

    private Long articleId;
    private String sectionTitle;
    private Integer stepOrder;
    private Boolean required;
    private String editorNote;

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public String getSectionTitle() {
        return sectionTitle;
    }

    public void setSectionTitle(String sectionTitle) {
        this.sectionTitle = sectionTitle;
    }

    public Integer getStepOrder() {
        return stepOrder;
    }

    public void setStepOrder(Integer stepOrder) {
        this.stepOrder = stepOrder;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public String getEditorNote() {
        return editorNote;
    }

    public void setEditorNote(String editorNote) {
        this.editorNote = editorNote;
    }
}
