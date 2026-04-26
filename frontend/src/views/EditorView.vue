<script setup>
import { computed, inject, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router';
import { createArticleApi, getEditableArticleApi, updateArticleApi } from '@/api/articles';
import { getCategoriesApi, getTagsApi } from '@/api/admin';
import { uploadImageApi } from '@/api/uploads';
import SiteHeader from '@/components/SiteHeader.vue';
import ArticleToc from '@/components/ArticleToc.vue';
import MarkdownPreview from '@/components/MarkdownPreview.vue';
import RichMarkdownEditor from '@/components/RichMarkdownEditor.vue';
import { topics } from '@/data/home';
import { resolveMediaUrl } from '@/utils/media';

const DRAFT_STORAGE_PREFIX = 'my-blog-editor-draft';
const DEFAULT_ARTICLE_COVER_URL = '/api/uploads/files/default/article-cover.svg';

const defaultDraft = {
    title: '',
    summary: '',
    content: `# 开始写作

用一篇文章把你最近解决的问题写清楚。`,
    category: 'Spring Boot',
    tags: 'Spring Boot, JWT',
    coverUrl: ''
};

const route = useRoute();
const router = useRouter();
const loginModal = inject('loginModal', { requireLogin: () => false });

const isEditMode = computed(() => route.name === 'editorEdit');
const editorArticleId = computed(() => (isEditMode.value ? String(route.params.id || '') : 'new'));
const storageKey = computed(() => `${DRAFT_STORAGE_PREFIX}:${editorArticleId.value}`);

const draft = reactive({ ...defaultDraft });
const statusMessage = ref('');
const validationErrors = ref([]);
const publishLoading = ref(false);
const publishedArticle = ref(null);
const feedbackType = ref('info');
const categoryOptions = ref(topics.slice(1));
const tagOptions = ref([]);
const pageLoading = ref(false);
const lastSavedSnapshot = ref('');
const previewVisible = ref(false);
const recoveryInfo = ref(null);
const hydratingDraft = ref(false);
const allowRouteLeave = ref(false);
const coverUploading = ref(false);
const coverInputRef = ref(null);
const coverPreviewFailed = ref(false);

const wordCount = computed(() => draft.content.trim().length);
const hasUnsavedChanges = computed(() => createDraftSnapshot(draft) !== lastSavedSnapshot.value);
const previewTags = computed(() => parseTags(draft.tags));
const previewPublishedText = computed(() => (isEditMode.value ? '预览当前编辑内容' : '预览待发布内容'));
const displayCoverUrl = computed(() => {
    const source = draft.coverUrl || DEFAULT_ARTICLE_COVER_URL;
    return coverPreviewFailed.value ? resolveMediaUrl(DEFAULT_ARTICLE_COVER_URL, '') : resolveMediaUrl(source, '');
});
const isUsingDefaultCover = computed(() => !draft.coverUrl || draft.coverUrl === DEFAULT_ARTICLE_COVER_URL);

watch(() => draft.coverUrl, () => {
    coverPreviewFailed.value = false;
});

function createDefaultDraft() {
    return { ...defaultDraft };
}

function createDraftSnapshot(currentDraft) {
    return JSON.stringify({
        title: currentDraft.title || '',
        summary: currentDraft.summary || '',
        content: currentDraft.content || '',
        category: currentDraft.category || '',
        tags: currentDraft.tags || '',
        coverUrl: currentDraft.coverUrl || ''
    });
}

function parseTags(sourceTags) {
    const source = Array.isArray(sourceTags) ? sourceTags : String(sourceTags || '').split(',');
    return source.map((item) => String(item).trim()).filter(Boolean);
}

function applyDraft(source = {}) {
    draft.title = source.title || '';
    draft.summary = source.summary || '';
    draft.content = source.content || '';
    draft.category = source.category || categoryOptions.value[0] || '';
    draft.tags = Array.isArray(source.tags) ? source.tags.join(', ') : (source.tags || '');
    draft.coverUrl = source.coverUrl || '';
}

function syncSavedSnapshot() {
    lastSavedSnapshot.value = createDraftSnapshot(draft);
}

function readStoredDraft(targetKey) {
    try {
        const raw = localStorage.getItem(targetKey);
        return raw ? JSON.parse(raw) : null;
    } catch (error) {
        return null;
    }
}

function clearStoredDraft() {
    localStorage.removeItem(storageKey.value);
}

function persistLocalDraft() {
    if (hydratingDraft.value) {
        return;
    }
    localStorage.setItem(storageKey.value, JSON.stringify({
        ...draft,
        savedAt: new Date().toISOString()
    }));
}

function formatSavedAt(savedAt) {
    if (!savedAt) {
        return '刚刚';
    }
    const date = new Date(savedAt);
    if (Number.isNaN(date.getTime())) {
        return savedAt;
    }
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
}

function buildDraftSourceLabel() {
    return isEditMode.value ? '本地编辑稿' : '本地草稿';
}

function restoreRecoveredDraft() {
    if (!recoveryInfo.value?.draft) {
        return;
    }
    hydratingDraft.value = true;
    applyDraft(recoveryInfo.value.draft);
    hydratingDraft.value = false;
    statusMessage.value = `已恢复${buildDraftSourceLabel()}，保存时间 ${formatSavedAt(recoveryInfo.value.savedAt)}`;
    feedbackType.value = 'success';
    recoveryInfo.value = null;
}

function discardRecoveredDraft() {
    if (recoveryInfo.value?.storageKey) {
        localStorage.removeItem(recoveryInfo.value.storageKey);
    }
    recoveryInfo.value = null;
    statusMessage.value = isEditMode.value ? '已使用线上版本继续编辑' : '已清空上次未发布内容';
    feedbackType.value = 'info';
}

function validatePublishDraft() {
    const errors = [];
    if (!draft.title.trim()) {
        errors.push('标题不能为空');
    }
    if (!draft.summary.trim()) {
        errors.push('摘要不能为空');
    }
    if (!draft.content.trim()) {
        errors.push('正文不能为空');
    }
    if (!draft.category) {
        errors.push('请选择分类');
    }
    validationErrors.value = errors;
    return errors.length === 0;
}

async function fetchMetadata() {
    try {
        const [categories, tags] = await Promise.all([
            getCategoriesApi(true),
            getTagsApi(true)
        ]);
        if (categories?.length) {
            categoryOptions.value = categories.map((item) => item.name);
        }
        if (tags?.length) {
            tagOptions.value = tags.map((item) => item.name);
        }
    } catch (error) {
        categoryOptions.value = topics.slice(1);
        tagOptions.value = [];
    }
}

async function setupNewDraft() {
    hydratingDraft.value = true;
    const storedDraft = readStoredDraft(storageKey.value);
    if (storedDraft) {
        applyDraft(storedDraft);
        statusMessage.value = `已恢复本地草稿，保存时间 ${formatSavedAt(storedDraft.savedAt)}`;
        feedbackType.value = 'info';
    } else {
        applyDraft(createDefaultDraft());
    }
    hydratingDraft.value = false;
    syncSavedSnapshot();
    recoveryInfo.value = null;
}

async function fetchArticle() {
    if (!isEditMode.value) {
        await setupNewDraft();
        return;
    }
    pageLoading.value = true;
    try {
        const article = await getEditableArticleApi(route.params.id);
        const serverDraft = {
            title: article.title || '',
            summary: article.summary || '',
            content: article.rawContent || '',
            category: article.category || categoryOptions.value[0] || '',
            tags: Array.isArray(article.tags) ? article.tags.join(', ') : '',
            coverUrl: article.coverUrl || defaultDraft.coverUrl
        };
        const storedDraft = readStoredDraft(storageKey.value);

        hydratingDraft.value = true;
        applyDraft(serverDraft);
        hydratingDraft.value = false;
        syncSavedSnapshot();

        if (storedDraft && createDraftSnapshot(storedDraft) !== createDraftSnapshot(serverDraft)) {
            recoveryInfo.value = {
                draft: storedDraft,
                savedAt: storedDraft.savedAt,
                storageKey: storageKey.value
            };
            statusMessage.value = `检测到一份未同步的本地编辑稿，保存时间 ${formatSavedAt(storedDraft.savedAt)}`;
            feedbackType.value = 'warning';
        } else {
            recoveryInfo.value = null;
        }
    } catch (error) {
        statusMessage.value = error.message || '文章加载失败';
        feedbackType.value = 'error';
    } finally {
        pageLoading.value = false;
    }
}

async function persistArticle(status) {
    if (publishLoading.value) {
        return null;
    }
    const actionText = status === 'PUBLISHED' ? '发布' : '保存草稿';
    const canContinue = loginModal.requireLogin(() => persistArticle(status), {
        title: `登录后${actionText}`,
        message: `登录后可以${actionText}文章，并在个人中心管理内容。`,
        actionText: `登录并${actionText}`
    });
    if (!canContinue) {
        statusMessage.value = `请先登录后再${actionText}`;
        feedbackType.value = 'warning';
        return null;
    }
    if (status === 'PUBLISHED' && !validatePublishDraft()) {
        statusMessage.value = `${actionText}前请补齐必要信息`;
        feedbackType.value = 'warning';
        return null;
    }

    publishLoading.value = true;
    statusMessage.value = status === 'PUBLISHED' ? '正在发布文章，请稍候...' : '正在保存草稿，请稍候...';
    feedbackType.value = 'info';
    publishedArticle.value = null;

    try {
        const article = isEditMode.value
            ? await updateArticleApi(route.params.id, draft, status)
            : await createArticleApi(draft, status);
        hydratingDraft.value = true;
        applyDraft({
            title: article.title,
            summary: article.summary,
            content: article.rawContent,
            category: article.category,
            tags: article.tags,
            coverUrl: article.coverUrl
        });
        hydratingDraft.value = false;
        if (!isEditMode.value) {
            allowRouteLeave.value = true;
            await router.replace(`/editor/${article.id}`);
            allowRouteLeave.value = false;
        }
        clearStoredDraft();
        recoveryInfo.value = null;
        if (status === 'PUBLISHED') {
            publishedArticle.value = article;
            statusMessage.value = `发布成功，文章 ID：${article.id}`;
            feedbackType.value = 'success';
        } else {
            statusMessage.value = `草稿已保存，文章 ID：${article.id}`;
            feedbackType.value = 'success';
        }
        syncSavedSnapshot();
        validationErrors.value = [];
        return article;
    } catch (error) {
        persistLocalDraft();
        statusMessage.value = error.message || `${actionText}失败`;
        feedbackType.value = 'error';
        return null;
    } finally {
        publishLoading.value = false;
    }
}

async function saveDraft() {
    await persistArticle('DRAFT');
}

async function publishArticle() {
    await persistArticle('PUBLISHED');
}

function triggerCoverPicker() {
    if (coverUploading.value || publishLoading.value || pageLoading.value) {
        return;
    }
    coverInputRef.value?.click();
}

async function handleCoverSelected(event) {
    const [file] = event.target?.files || [];
    event.target.value = '';
    if (!file) {
        return;
    }
    coverUploading.value = true;
    try {
        const result = await uploadImageApi(file, 'cover');
        draft.coverUrl = result.url || defaultDraft.coverUrl;
        coverPreviewFailed.value = false;
        statusMessage.value = '封面已上传，发布或保存草稿后会写入文章';
        feedbackType.value = 'success';
    } catch (error) {
        statusMessage.value = error.message || '封面上传失败';
        feedbackType.value = 'error';
    } finally {
        coverUploading.value = false;
    }
}

function handleCoverPreviewLoad() {
    coverPreviewFailed.value = false;
}

function handleCoverPreviewError() {
    if (!displayCoverUrl.value) {
        return;
    }
    coverPreviewFailed.value = true;
    statusMessage.value = '封面图片暂时无法访问，请检查链接或稍后重试';
    feedbackType.value = 'error';
}

function viewPublishedArticle() {
    if (publishedArticle.value?.id) {
        allowRouteLeave.value = true;
        router.push(`/articles/${publishedArticle.value.id}`);
    }
}

function continueWriting() {
    publishedArticle.value = null;
    statusMessage.value = '可以继续编辑当前内容';
    feedbackType.value = 'info';
}

function togglePreview() {
    previewVisible.value = !previewVisible.value;
}

function confirmDiscardChanges() {
    return window.confirm('当前还有未保存改动，确定要离开当前编辑页吗？');
}

function handleBeforeUnload(event) {
    if (!hasUnsavedChanges.value || allowRouteLeave.value) {
        return;
    }
    event.preventDefault();
    event.returnValue = '';
}

watch(
    () => route.params.id,
    async () => {
        publishedArticle.value = null;
        previewVisible.value = false;
        await fetchArticle();
    }
);

watch(draft, () => {
    if (!pageLoading.value) {
        persistLocalDraft();
    }
}, { deep: true });

onBeforeRouteLeave(() => {
    if (allowRouteLeave.value || !hasUnsavedChanges.value) {
        return true;
    }
    return confirmDiscardChanges();
});

onMounted(async () => {
    window.addEventListener('beforeunload', handleBeforeUnload);
    await fetchMetadata();
    await fetchArticle();
});

onUnmounted(() => {
    window.removeEventListener('beforeunload', handleBeforeUnload);
});
</script>

<template>
    <SiteHeader />
    <main class="page-shell editor-layout" data-testid="editor-page">
        <section class="editor-main" data-testid="editor-main">
            <div class="section-heading editor-heading">
                <div>
                    <p class="eyebrow">创作中心</p>
                    <h1>{{ isEditMode ? '编辑文章' : '发布文章' }}</h1>
                </div>
                <div class="editor-actions">
                    <button type="button" :disabled="publishLoading || pageLoading" data-testid="editor-preview-button" @click="togglePreview">
                        {{ previewVisible ? '收起预览' : '发布前预览' }}
                    </button>
                    <button type="button" :disabled="publishLoading || pageLoading" data-testid="editor-save-draft" @click="saveDraft">保存草稿</button>
                    <button
                        class="primary-action"
                        type="button"
                        :disabled="publishLoading || pageLoading"
                        data-testid="editor-publish-button"
                        @click="publishArticle"
                    >
                        {{ publishLoading ? '提交中...' : '发布文章' }}
                    </button>
                </div>
            </div>

            <p v-if="statusMessage" :class="['editor-action-status', feedbackType]">
                {{ statusMessage }}
            </p>

            <section v-if="recoveryInfo" class="editor-recovery-banner">
                <div>
                    <p class="eyebrow">检测到本地恢复内容</p>
                    <strong>{{ isEditMode ? '这篇文章有一份未同步的本地编辑稿。' : '你上次有一份未发布草稿。' }}</strong>
                    <span>保存时间 {{ formatSavedAt(recoveryInfo.savedAt) }}，你可以选择恢复继续写，或丢弃它继续使用当前内容。</span>
                </div>
                <div class="editor-recovery-actions">
                    <button class="primary-action" type="button" @click="restoreRecoveredDraft">恢复本地草稿</button>
                    <button type="button" @click="discardRecoveredDraft">使用当前版本</button>
                </div>
            </section>

            <p v-if="pageLoading" class="loading-text">正在加载文章内容...</p>

            <section
                v-if="publishedArticle"
                class="publish-result"
                aria-live="polite"
            >
                <div>
                    <p class="eyebrow">发布成功</p>
                    <h2>{{ publishedArticle.title }}</h2>
                    <p>文章已经发布到站点，读者现在可以在文章详情页查看。</p>
                    <div class="publish-meta">
                        <span>ID {{ publishedArticle.id }}</span>
                        <span>{{ publishedArticle.category }}</span>
                        <span>{{ publishedArticle.publishedText }}</span>
                    </div>
                </div>
                <div class="publish-result-actions">
                    <button class="primary-action" type="button" @click="viewPublishedArticle">查看文章</button>
                    <button type="button" @click="continueWriting">继续编辑</button>
                </div>
            </section>

            <section v-if="previewVisible" class="editor-preview-panel">
                <div class="editor-preview-head">
                    <div>
                        <p class="eyebrow">发布前预览</p>
                        <h2>{{ draft.title || '未填写标题' }}</h2>
                    </div>
                    <div class="editor-preview-meta">
                        <span>{{ draft.category || '未选择分类' }}</span>
                        <span>{{ previewPublishedText }}</span>
                        <span>{{ wordCount }} 字</span>
                    </div>
                </div>
                <p v-if="draft.summary" class="editor-preview-summary">{{ draft.summary }}</p>
                <div v-if="previewTags.length" class="editor-preview-tags">
                    <span v-for="tag in previewTags" :key="tag">{{ tag }}</span>
                </div>
                <img
                    class="editor-preview-cover"
                    :src="displayCoverUrl"
                    alt="预览封面"
                    @load="handleCoverPreviewLoad"
                    @error="handleCoverPreviewError"
                >
                <MarkdownPreview v-if="draft.content.trim()" :content="draft.content" />
                <div v-else class="editor-preview-empty">正文还没有内容，继续往下写就好。</div>
            </section>

            <section v-if="!previewVisible">
            <label class="editor-title">
                <span class="sr-only">文章标题</span>
                <input v-model="draft.title" type="text" placeholder="请输入文章标题" data-testid="editor-title-input">
            </label>

            <label class="editor-summary">
                <span>文章摘要</span>
                <textarea v-model="draft.summary" placeholder="用一两句话介绍这篇文章" data-testid="editor-summary-input"></textarea>
            </label>

            <RichMarkdownEditor v-model="draft.content" />
            <div class="editor-feedback">
                <span>正文 {{ wordCount }} 字</span>
                <span :class="['editor-dirty', hasUnsavedChanges ? 'warning' : 'saved']">
                    {{ hasUnsavedChanges ? '未保存改动，离开页面会提醒你' : '内容已同步到当前编辑态' }}
                </span>
            </div>
            <ul v-if="validationErrors.length" class="error-list">
                <li v-for="error in validationErrors" :key="error">{{ error }}</li>
            </ul>
            </section>
        </section>

        <aside class="editor-side">
            <ArticleToc
                :content="draft.content"
                target-selector=".rich-markdown-editor .ProseMirror"
            />
            <section class="side-section editor-category-section">
                <div class="editor-category-head">
                    <p class="eyebrow">分类</p>
                    <span class="editor-category-value">{{ draft.category || '请选择分类' }}</span>
                </div>
                <div class="editor-category-select-wrap">
                    <select v-model="draft.category" class="editor-category-select">
                        <option v-for="topic in categoryOptions" :key="topic" :value="topic">{{ topic }}</option>
                    </select>
                </div>
            </section>
            <section class="side-section">
                <p class="eyebrow">标签</p>
                <input v-model="draft.tags" type="text" :placeholder="tagOptions.length ? `例如：${tagOptions.slice(0, 3).join(', ')}` : '多个标签用英文逗号分隔'">
            </section>
            <section class="side-section upload-box">
                <div class="cover-card-head">
                    <div>
                        <p class="eyebrow">文章封面</p>
                        <h3>{{ isUsingDefaultCover ? '给文章加一张封面图' : '当前封面' }}</h3>
                    </div>
                    <span :class="['cover-status-pill', isUsingDefaultCover ? 'default' : 'custom']">
                        {{ isUsingDefaultCover ? '系统默认封面' : '已上传自定义封面' }}
                    </span>
                </div>
                <input
                    ref="coverInputRef"
                    type="file"
                    accept="image/*"
                    class="sr-only"
                    @change="handleCoverSelected"
                >
                <div class="cover-card-preview">
                    <img
                        :src="displayCoverUrl"
                        alt="文章封面预览"
                        @load="handleCoverPreviewLoad"
                        @error="handleCoverPreviewError"
                    >
                    <div class="cover-card-overlay">
                        <span>{{ isUsingDefaultCover ? '未上传时将使用系统默认封面' : '这张图会作为文章封面展示' }}</span>
                    </div>
                </div>
                <p v-if="isUsingDefaultCover" class="cover-help-text">
                    当前会使用系统默认封面，你可以随时上传一张更贴合主题的封面。
                </p>
                <button
                    type="button"
                    class="cover-upload-button"
                    :disabled="coverUploading || publishLoading || pageLoading"
                    @click="triggerCoverPicker"
                >
                    {{ coverUploading ? '上传中...' : (isUsingDefaultCover ? '上传封面' : '更换封面') }}
                </button>
            </section>
        </aside>
    </main>
</template>

<style scoped>
.editor-recovery-banner,
.editor-preview-panel {
    display: grid;
    gap: 14px;
    padding: 18px 20px;
    background: var(--surface);
    border: 1px solid rgba(31, 122, 224, 0.08);
    border-radius: 18px;
    box-shadow: var(--shadow);
}

.editor-recovery-banner {
    grid-template-columns: minmax(0, 1fr) auto;
    align-items: center;
}

.editor-recovery-banner strong,
.editor-preview-head h2 {
    color: var(--text);
}

.editor-recovery-banner strong,
.editor-preview-head h2,
.editor-preview-summary {
    margin: 0;
}

.editor-recovery-banner span,
.editor-preview-meta span,
.editor-preview-empty {
    color: var(--muted);
    line-height: 1.7;
}

.editor-recovery-actions,
.editor-preview-meta,
.editor-preview-tags {
    display: flex;
    gap: 10px;
    align-items: center;
    flex-wrap: wrap;
}

.editor-preview-head {
    display: flex;
    gap: 14px;
    align-items: end;
    justify-content: space-between;
}

.editor-preview-meta span,
.editor-preview-tags span {
    display: inline-flex;
    align-items: center;
    min-height: 30px;
    padding: 0 10px;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: 999px;
    font-size: 13px;
}

.editor-preview-summary {
    color: var(--muted);
    line-height: 1.8;
}

.editor-preview-cover {
    width: 100%;
    max-height: 280px;
    object-fit: cover;
    border-radius: 18px;
}

.editor-preview-empty {
    padding: 22px 18px;
    background: var(--surface-soft);
    border: 1px dashed var(--line);
    border-radius: 16px;
}

.upload-box {
    display: grid;
    gap: 14px;
    padding: 18px;
    border: 1px solid color-mix(in srgb, var(--brand, #1f7ae0) 18%, var(--line));
    border-radius: 18px;
    background:
        radial-gradient(circle at top right, rgba(31, 122, 224, 0.14), transparent 42%),
        linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(244, 248, 255, 0.96));
    box-shadow: 0 20px 40px rgba(15, 23, 42, 0.08);
}

.cover-card-head {
    display: flex;
    gap: 12px;
    align-items: flex-start;
    justify-content: space-between;
}

.cover-card-head h3 {
    margin: 4px 0 0;
    color: var(--text);
    font-size: 18px;
    line-height: 1.35;
}

.cover-status-pill {
    display: inline-flex;
    align-items: center;
    min-height: 30px;
    padding: 0 12px;
    border-radius: 999px;
    font-size: 12px;
    font-weight: 700;
    white-space: nowrap;
}

.cover-status-pill.default {
    color: #8a5b00;
    background: rgba(255, 214, 102, 0.22);
    border: 1px solid rgba(201, 141, 0, 0.22);
}

.cover-status-pill.custom {
    color: var(--brand-strong);
    background: rgba(31, 122, 224, 0.12);
    border: 1px solid rgba(31, 122, 224, 0.16);
}

.cover-card-preview {
    position: relative;
    overflow: hidden;
    border-radius: 18px;
    border: 1px solid rgba(15, 23, 42, 0.08);
    background: var(--surface-soft);
    aspect-ratio: 16 / 10;
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.75);
}

.cover-card-preview img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.cover-card-overlay {
    position: absolute;
    inset: auto 0 0;
    padding: 14px 16px;
    background: linear-gradient(180deg, rgba(16, 23, 31, 0), rgba(16, 23, 31, 0.78));
}

.cover-card-overlay span {
    color: rgba(255, 255, 255, 0.92);
    font-size: 13px;
    line-height: 1.6;
}

.cover-help-text {
    margin: 0;
    padding: 12px 14px;
    color: var(--muted);
    font-size: 13px;
    line-height: 1.7;
    background: rgba(31, 122, 224, 0.05);
    border: 1px solid rgba(31, 122, 224, 0.08);
    border-radius: 12px;
}

.cover-upload-button {
    display: inline-flex;
    justify-content: center;
    align-items: center;
    min-height: 44px;
    padding: 0 16px;
    border: 0;
    border-radius: 999px;
    background: linear-gradient(135deg, var(--brand, #1f7ae0), var(--brand-strong, #1664c4));
    color: #ffffff;
    font-size: 14px;
    font-weight: 700;
    cursor: pointer;
    transition: transform 0.16s ease, box-shadow 0.16s ease, filter 0.16s ease;
    box-shadow: 0 14px 28px rgba(31, 122, 224, 0.2);
}

.cover-upload-button:hover {
    transform: translateY(-1px);
    filter: brightness(1.03);
}

.cover-upload-button:disabled {
    opacity: 0.58;
    cursor: not-allowed;
    transform: none;
    filter: none;
    box-shadow: none;
}

@media (max-width: 760px) {
    .editor-recovery-banner {
        grid-template-columns: 1fr;
    }

    .editor-preview-head {
        flex-direction: column;
        align-items: stretch;
    }

    .cover-card-head {
        flex-direction: column;
    }
}
</style>
