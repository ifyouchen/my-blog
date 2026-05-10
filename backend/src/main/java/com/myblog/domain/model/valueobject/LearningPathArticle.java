package com.myblog.domain.model.valueobject;

/**
 * 专题/专栏中的路径文章编排信息。
 *
 * @author Codex
 * @since 1.0.0
 */
public class LearningPathArticle {

    private final Long articleId;
    private final String sectionTitle;
    private final int stepOrder;
    private final boolean required;
    private final String editorNote;

    public LearningPathArticle(Long articleId, String sectionTitle, int stepOrder,
                               boolean required, String editorNote) {
        this.articleId = articleId;
        this.sectionTitle = sectionTitle;
        this.stepOrder = stepOrder;
        this.required = required;
        this.editorNote = editorNote;
    }

    public Long getArticleId() {
        return articleId;
    }

    public String getSectionTitle() {
        return sectionTitle;
    }

    public int getStepOrder() {
        return stepOrder;
    }

    public boolean isRequired() {
        return required;
    }

    public String getEditorNote() {
        return editorNote;
    }
}
