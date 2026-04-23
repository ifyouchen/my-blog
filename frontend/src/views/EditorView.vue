<script setup>
import { computed, inject, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router';
import { createArticleApi, getEditableArticleApi, updateArticleApi } from '@/api/articles';
import { getCategoriesApi, getTagsApi } from '@/api/admin';
import SiteHeader from '@/components/SiteHeader.vue';
import ArticleToc from '@/components/ArticleToc.vue';
import MarkdownPreview from '@/components/MarkdownPreview.vue';
import RichMarkdownEditor from '@/components/RichMarkdownEditor.vue';
import { topics } from '@/data/home';

const DRAFT_STORAGE_PREFIX = 'my-blog-editor-draft';

const defaultDraft = {
    title: '',
    summary: '',
    content: `# 开始写作

用一篇文章把你最近解决的问题写清楚。`,
    category: 'Spring Boot',
    tags: 'Spring Boot, JWT',
    coverUrl: 'https://images.unsplash.com/photo-1515879218367-8466d910aaa4?auto=format&fit=crop&w=700&q=80'
};

const coverOptions = [
    defaultDraft.coverUrl,
    'https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=700&q=80',
    'https://images.unsplash.com/photo-1558494949-ef010cbdcc31?auto=format&fit=crop&w=700&q=80'
];

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

const wordCount = computed(() => draft.content.trim().length);
const hasUnsavedChanges = computed(() => createDraftSnapshot(draft) !== lastSavedSnapshot.value);
const previewTags = computed(() => parseTags(draft.tags));
const previewPublishedText = computed(() => (isEditMode.value ? '预览当前编辑内容' : '预览待发布内容'));

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
    draft.coverUrl = source.coverUrl || defaultDraft.coverUrl;
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

function changeCover() {
    const currentIndex = coverOptions.indexOf(draft.coverUrl);
    const nextIndex = currentIndex >= 0 ? (currentIndex + 1) % coverOptions.length : 0;
    draft.coverUrl = coverOptions[nextIndex];
    statusMessage.value = '已更换封面预览';
    feedbackType.value = 'info';
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
    <main class="page-shell editor-layout">
        <section class="editor-main">
            <div class="section-heading editor-heading">
                <div>
                    <p class="eyebrow">创作中心</p>
                    <h1>{{ isEditMode ? '编辑文章' : '发布文章' }}</h1>
                </div>
                <div class="editor-actions">
                    <button type="button" :disabled="publishLoading || pageLoading" @click="togglePreview">
                        {{ previewVisible ? '收起预览' : '发布前预览' }}
                    </button>
                    <button type="button" :disabled="publishLoading || pageLoading" @click="saveDraft">保存草稿</button>
                    <button
                        class="primary-action"
                        type="button"
                        :disabled="publishLoading || pageLoading"
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
                <img class="editor-preview-cover" :src="draft.coverUrl" alt="预览封面">
                <MarkdownPreview v-if="draft.content.trim()" :content="draft.content" />
                <div v-else class="editor-preview-empty">正文还没有内容，继续往下写就好。</div>
            </section>

            <label class="editor-title">
                <span class="sr-only">文章标题</span>
                <input v-model="draft.title" type="text" placeholder="请输入文章标题">
            </label>

            <label class="editor-summary">
                <span>文章摘要</span>
                <textarea v-model="draft.summary" placeholder="用一两句话介绍这篇文章"></textarea>
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
                <p class="eyebrow">封面图</p>
                <img :src="draft.coverUrl" alt="文章封面预览">
                <button type="button" @click="changeCover">更换封面</button>
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
    border: 1px solid var(--line);
    border-radius: 8px;
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
    border-radius: 8px;
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
    border-radius: 8px;
}

.editor-preview-empty {
    padding: 22px 18px;
    background: var(--surface-soft);
    border: 1px dashed var(--line);
    border-radius: 8px;
}

@media (max-width: 760px) {
    .editor-recovery-banner {
        grid-template-columns: 1fr;
    }

    .editor-preview-head {
        flex-direction: column;
        align-items: stretch;
    }
}
</style>
