package com.myblog.domain.model.valueobject;

/**
 * 专题/专栏中的路径文章编排信息。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class LearningPathArticle {

    /** 文章 ID。 */
    private final Long articleId;
    /** 所属章节标题。 */
    private final String sectionTitle;
    /** 在路径中的排列顺序。 */
    private final int stepOrder;
    /** 是否为必读文章。 */
    private final boolean required;
    /** 编辑备注。 */
    private final String editorNote;

    /**
     * 创建学习路径文章。
     *
     * @param articleId    文章 ID
     * @param sectionTitle 章节标题
     * @param stepOrder    排列顺序
     * @param required     是否必读
     * @param editorNote   编辑备注
     */
    public LearningPathArticle(Long articleId, String sectionTitle, int stepOrder,
                               boolean required, String editorNote) {
        this.articleId = articleId;
        this.sectionTitle = sectionTitle;
        this.stepOrder = stepOrder;
        this.required = required;
        this.editorNote = editorNote;
    }

    /**
     * 获取文章 ID。
     *
     * @return 文章 ID
     */
    public Long getArticleId() {
        return articleId;
    }

    /**
     * 获取章节标题。
     *
     * @return 章节标题
     */
    public String getSectionTitle() {
        return sectionTitle;
    }

    /**
     * 获取文章在路径中的排列顺序。
     *
     * @return 排列顺序
     */
    public int getStepOrder() {
        return stepOrder;
    }

    /**
     * 判断文章是否为路径中的必读项。
     *
     * @return 是否必读
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * 获取编辑备注。
     *
     * @return 编辑备注
     */
    public String getEditorNote() {
        return editorNote;
    }
}
