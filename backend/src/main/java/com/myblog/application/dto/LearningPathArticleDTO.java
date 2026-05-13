package com.myblog.application.dto;

/**
 * 专题/专栏学习路径文章。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class LearningPathArticleDTO {

    /** 关联文章 */
    private ArticleDTO article;
    /** 所属章节标题 */
    private String sectionTitle;
    /** 步骤顺序 */
    private int stepOrder;
    /** 是否必读 */
    private boolean required;
    /** 编辑备注 */
    private String editorNote;
    /** 是否已完成 */
    private boolean completed;

    /**
     * 获取关联文章。
     *
     * @return 关联文章
     */
    public ArticleDTO getArticle() {
        return article;
    }

    /**
     * 设置关联文章。
     *
     * @param article 关联文章
     */
    public void setArticle(ArticleDTO article) {
        this.article = article;
    }

    /**
     * 获取所属章节标题。
     *
     * @return 所属章节标题
     */
    public String getSectionTitle() {
        return sectionTitle;
    }

    /**
     * 设置所属章节标题。
     *
     * @param sectionTitle 所属章节标题
     */
    public void setSectionTitle(String sectionTitle) {
        this.sectionTitle = sectionTitle;
    }

    /**
     * 获取步骤顺序。
     *
     * @return 步骤顺序
     */
    public int getStepOrder() {
        return stepOrder;
    }

    /**
     * 设置步骤顺序。
     *
     * @param stepOrder 步骤顺序
     */
    public void setStepOrder(int stepOrder) {
        this.stepOrder = stepOrder;
    }

    /**
     * 获取是否必读。
     *
     * @return 是否必读
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * 设置是否必读。
     *
     * @param required 是否必读
     */
    public void setRequired(boolean required) {
        this.required = required;
    }

    /**
     * 获取编辑备注。
     *
     * @return 编辑备注
     */
    public String getEditorNote() {
        return editorNote;
    }

    /**
     * 设置编辑备注。
     *
     * @param editorNote 编辑备注
     */
    public void setEditorNote(String editorNote) {
        this.editorNote = editorNote;
    }

    /**
     * 获取是否已完成。
     *
     * @return 是否已完成
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * 设置是否已完成。
     *
     * @param completed 是否已完成
     */
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
