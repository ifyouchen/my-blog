package com.myblog.infrastructure.repository.persistence.entity;

/**
 * 专题/专栏文章编排数据对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class LearningPathArticleDO {

    /**
     * 文章 ID
     */
    private Long articleId;

    /**
     * 所属章节标题
     */
    private String sectionTitle;

    /**
     * 步骤排序序号
     */
    private Integer stepOrder;

    /**
     * 是否为必读文章
     */
    private Boolean required;

    /**
     * 编辑备注
     */
    private String editorNote;

    /**
     * 获取文章 ID。
     *
     * @return 文章 ID
     */
    public Long getArticleId() {
        return articleId;
    }

    /**
     * 设置文章 ID。
     *
     * @param articleId 文章 ID
     */
    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    /**
     * 获取所属章节标题。
     *
     * @return 章节标题
     */
    public String getSectionTitle() {
        return sectionTitle;
    }

    /**
     * 设置所属章节标题。
     *
     * @param sectionTitle 章节标题
     */
    public void setSectionTitle(String sectionTitle) {
        this.sectionTitle = sectionTitle;
    }

    /**
     * 获取步骤排序序号。
     *
     * @return 步骤序号
     */
    public Integer getStepOrder() {
        return stepOrder;
    }

    /**
     * 设置步骤排序序号。
     *
     * @param stepOrder 步骤序号
     */
    public void setStepOrder(Integer stepOrder) {
        this.stepOrder = stepOrder;
    }

    /**
     * 获取是否为必读文章。
     *
     * @return true 表示必读，false 表示可选
     */
    public Boolean getRequired() {
        return required;
    }

    /**
     * 设置是否为必读文章。
     *
     * @param required 是否必读
     */
    public void setRequired(Boolean required) {
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
}
