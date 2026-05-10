package com.myblog.application.dto;

/**
 * 专题/专栏学习路径文章。
 *
 * @author Codex
 * @since 1.0.0
 */
public class LearningPathArticleDTO {

    private ArticleDTO article;
    private String sectionTitle;
    private int stepOrder;
    private boolean required;
    private String editorNote;
    private boolean completed;

    public ArticleDTO getArticle() {
        return article;
    }

    public void setArticle(ArticleDTO article) {
        this.article = article;
    }

    public String getSectionTitle() {
        return sectionTitle;
    }

    public void setSectionTitle(String sectionTitle) {
        this.sectionTitle = sectionTitle;
    }

    public int getStepOrder() {
        return stepOrder;
    }

    public void setStepOrder(int stepOrder) {
        this.stepOrder = stepOrder;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getEditorNote() {
        return editorNote;
    }

    public void setEditorNote(String editorNote) {
        this.editorNote = editorNote;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
