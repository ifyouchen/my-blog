<script setup>
import {computed, inject, onMounted, onUnmounted, reactive, ref, watch} from 'vue';
import {onBeforeRouteLeave, useRoute, useRouter} from 'vue-router';
import {useHead} from '@unhead/vue';
import {
    createArticleApi,
    getArticleVersionApi,
    getEditableArticleApi,
    listArticleVersionsApi,
    restoreArticleVersionApi,
    updateArticleApi,
    validateArticleForPublishApi
} from '@/api/articles';
import {getToken} from '@/api/http';
import {getCategoriesApi, getTagsApi} from '@/api/admin';
import {uploadImageApi} from '@/api/uploads';
import SiteHeader from '@/components/SiteHeader.vue';
import ArticleToc from '@/components/ArticleToc.vue';
import MarkdownPreview from '@/components/MarkdownPreview.vue';
import RichMarkdownEditor from '@/components/RichMarkdownEditor.vue';
import ConfirmDialog from '@/components/ConfirmDialog.vue';
import {topics} from '@/data/home';
import {resolveMediaUrl} from '@/utils/media';
import {findArticleWarnSensitiveWords, formatWarnSensitiveWords} from '@/utils/sensitiveWords';
import { useConfirmDialog } from '@/composables/useConfirmDialog';

const DRAFT_STORAGE_PREFIX = 'my-blog-editor-draft';
const DEFAULT_ARTICLE_COVER_URL = '/api/uploads/files/default/article-cover.svg';
const AUTO_SAVE_DELAY_MS = 15000;

const defaultDraft = {
    title: '',
    summary: '',
    content: `# 开始写作

用一篇文章把你最近解决的问题写清楚。`,
    category: 'Spring Boot',
    tags: 'Spring Boot, JWT',
    coverUrl: '',
    slug: '',
    seoTitle: '',
    seoDescription: '',
    scheduledPublishAt: ''
};

const route = useRoute();
const router = useRouter();
const loginModal = inject('loginModal', { requireLogin: () => false });
const {
    confirmDialog,
    openConfirmDialog,
    closeConfirmDialog,
    executeConfirmDialog
} = useConfirmDialog();
const pendingLeaveTarget = ref(null);

const isEditMode = computed(() => route.name === 'editorEdit');
const editorArticleId = computed(() => (isEditMode.value ? String(route.params.id || '') : 'new'));
const storageKey = computed(() => `${DRAFT_STORAGE_PREFIX}:${editorArticleId.value}`);

useHead({
    title: computed(() => isEditMode.value ? '编辑文章 - my-blog' : '写文章 - my-blog')
});

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
const currentArticleStatus = ref('DRAFT');
const previewVisible = ref(false);
const recoveryInfo = ref(null);
const hydratingDraft = ref(false);
const allowRouteLeave = ref(false);
const coverUploading = ref(false);
const coverInputRef = ref(null);
const richEditorRef = ref(null);
const coverPreviewFailed = ref(false);
const debouncedCoverUrl = ref('');
let coverUrlDebounceTimer = null;
const coverRetryCount = ref(0);
const coverRetryKey = ref(0);
let coverRetryTimer = null;
const showBackToTop = ref(false);
const publishValidation = ref({
    publishable: false,
    errors: [],
    warnings: [],
    checks: []
});
const publishValidationLoading = ref(false);
const publishValidationError = ref('');
let publishValidationTimer = null;

// ── 版本历史 ──────────────────────────────────────────────────────────
const showVersionDrawer = ref(false);
const versionList = ref([]);
const versionListLoading = ref(false);
const versionPreview = ref(null);
const versionPreviewLoading = ref(false);
const versionRestoring = ref(false);

// ── 自动保存 ──────────────────────────────────────────────────────────
const autoSaveStatus = ref(''); // '' | 'saving' | 'saved HH:mm' | 'error'
let autoSaveTimer = null;
let autoSaveInFlight = false;

function clearAutoSaveTimer() {
    if (autoSaveTimer) {
        clearTimeout(autoSaveTimer);
        autoSaveTimer = null;
    }
}

function formatAutoSaveTime() {
    const now = new Date();
    return `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
}

function hasAutoSaveableContent() {
    const defaultContent = defaultDraft.content.trim();
    return Boolean(
        draft.title.trim()
        || draft.summary.trim()
        || draft.content.trim() !== defaultContent
        || draft.coverUrl.trim()
        || draft.slug.trim()
        || draft.seoTitle.trim()
        || draft.seoDescription.trim()
        || draft.scheduledPublishAt.trim()
    );
}

function canServerAutoSave() {
    return !isEditMode.value || currentArticleStatus.value === 'DRAFT';
}

async function runAutoSaveDraft() {
    if (publishLoading.value || pageLoading.value || recoveryInfo.value || autoSaveInFlight) {
        return;
    }
    if (!canServerAutoSave() || !hasUnsavedChanges.value || !hasAutoSaveableContent() || !getToken()) {
        return;
    }
    const sensitiveHits = await findArticleWarnSensitiveWords(draft);
    if (sensitiveHits.length) {
        return;
    }

    const previousStorageKey = storageKey.value;
    autoSaveInFlight = true;
    autoSaveStatus.value = 'saving';
    try {
        const article = isEditMode.value
            ? await updateArticleApi(route.params.id, draft, 'DRAFT')
            : await createArticleApi(draft, 'DRAFT');

        hydratingDraft.value = true;
        applyDraft({
            title: article.title,
            summary: article.summary,
            content: article.rawContent,
            category: article.category,
            tags: article.tags,
            coverUrl: article.coverUrl,
            slug: article.slug || '',
            seoTitle: article.seoTitle || '',
            seoDescription: article.seoDescription || '',
            scheduledPublishAt: normalizeDateTimeLocal(article.scheduledPublishAt || draft.scheduledPublishAt || '')
        });
        hydratingDraft.value = false;
        currentArticleStatus.value = article.status || 'DRAFT';
        syncSavedSnapshot();
        clearStoredDraft(previousStorageKey);

        if (!isEditMode.value && article.id) {
            allowRouteLeave.value = true;
            await router.replace(`/editor/${article.id}`);
            allowRouteLeave.value = false;
        }

        autoSaveStatus.value = `saved ${formatAutoSaveTime()}`;
    } catch {
        autoSaveStatus.value = 'error';
    } finally {
        autoSaveInFlight = false;
    }
}

function scheduleAutoSave() {
    clearAutoSaveTimer();
    if (pageLoading.value || recoveryInfo.value || !hasUnsavedChanges.value || !hasAutoSaveableContent()) {
        return;
    }
    if (!canServerAutoSave()) {
        return;
    }
    autoSaveTimer = setTimeout(runAutoSaveDraft, AUTO_SAVE_DELAY_MS);
}

const autoSaveLabel = computed(() => {
    if (autoSaveStatus.value === 'saving') return '自动保存中...';
    if (autoSaveStatus.value === 'error') return '自动保存失败';
    if (autoSaveStatus.value.startsWith('saved ')) return `已自动保存 ${autoSaveStatus.value.replace('saved ', '')}`;
    return '';
});

// ── 文章统计 ──────────────────────────────────────────────────────────
const wordCount = computed(() => {
    const text = draft.content.trim();
    if (!text) return 0;
    const chineseCount = (text.match(/[\u4e00-\u9fa5]/g) || []).length;
    const englishWords = text.replace(/[\u4e00-\u9fa5]/g, ' ').trim().split(/\s+/).filter(Boolean).length;
    return chineseCount + englishWords;
});
const readingMinutes = computed(() => Math.max(1, Math.ceil(wordCount.value / 300)));
const hasUnsavedChanges = computed(() => createDraftSnapshot(draft) !== lastSavedSnapshot.value);
const previewTags = computed(() => parseTags(draft.tags));
const configuredTagNameSet = computed(() => {
    return new Set(
        tagOptions.value
            .map((item) => String(item || '').trim().toLowerCase())
            .filter(Boolean)
    );
});
const unconfiguredDraftTags = computed(() => {
    if (!configuredTagNameSet.value.size) {
        return [];
    }
    const seen = new Set();
    return previewTags.value.filter((tag) => {
        const key = tag.toLowerCase();
        if (configuredTagNameSet.value.has(key) || seen.has(key)) {
            return false;
        }
        seen.add(key);
        return true;
    });
});
const previewPublishedText = computed(() => (isEditMode.value ? '预览当前编辑内容' : '预览待发布内容'));
const plainContent = computed(() => stripMarkdown(draft.content));
const seoPreviewTitle = computed(() => (draft.seoTitle || draft.title || '未填写标题').trim());
const seoPreviewDescription = computed(() => {
    const source = draft.seoDescription || draft.summary || plainContent.value;
    const text = source.trim();
    return text ? truncateText(text, 160) : '发布前补充摘要或 SEO 描述后，这里会展示搜索结果摘要。';
});
const seoPreviewPath = computed(() => {
    const id = isEditMode.value ? editorArticleId.value : '发布后文章ID';
    return `/articles/${id}${draft.slug ? '-' + draft.slug : ''}`;
});
const seoPreviewUrl = computed(() => {
    if (typeof window === 'undefined') {
        return seoPreviewPath.value;
    }
    return `${window.location.origin}${seoPreviewPath.value}`;
});
const seoKeywords = computed(() => {
    const text = (draft.title + ' ' + plainContent.value).toLowerCase();
    const words = text.match(/[一-龥]{2,}|[a-zA-Z]{3,}/g) || [];
    const freq = {};
    for (const w of words) {
        freq[w] = (freq[w] || 0) + 1;
    }
    const minFreq = Math.max(2, Math.ceil(words.length * 0.005));
    return Object.entries(freq)
        .filter(([, count]) => count >= minFreq)
        .sort(([, a], [, b]) => b - a)
        .slice(0, 10)
        .map(([word, count]) => ({ word, count }));
});
const seoHealthChecks = computed(() => {
    const titleLen = seoPreviewTitle.value.length;
    const descLen = seoPreviewDescription.value.length;
    const contentLen = plainContent.value.length;
    const hasHeadings = /^#{1,3}\s/m.test(draft.content);
    const hasImages = /!\[.*?]\(.*?\)/.test(draft.content);
    return [
        { key: 'title', label: 'SEO 标题', ok: titleLen > 0 && titleLen <= 60,
            hint: titleLen === 0 ? '未设置，将使用文章标题' : `${titleLen} 字符` },
        { key: 'description', label: 'SEO 描述', ok: descLen > 0 && descLen <= 160,
            hint: descLen === 0 ? '未设置，将使用摘要或正文开头' : `${descLen} 字符` },
        { key: 'slug', label: 'URL Slug', ok: Boolean(draft.slug),
            hint: draft.slug ? draft.slug : '未设置，将使用文章 ID' },
        { key: 'headings', label: '小标题结构', ok: hasHeadings,
            hint: hasHeadings ? '包含小标题' : '建议使用 ## 小标题组织内容' },
        { key: 'images', label: '配图', ok: hasImages,
            hint: hasImages ? '包含图片' : '建议适当插入图片' },
        { key: 'length', label: '内容长度', ok: contentLen >= 200,
            hint: contentLen < 200 ? `${contentLen} 字，建议至少 200 字` : `${contentLen} 字` }
    ];
});
const showPersistentStatus = computed(() => ['warning', 'error'].includes(feedbackType.value));
const showQuietStatus = computed(() => !showPersistentStatus.value && Boolean(statusMessage.value));
const displayCoverUrl = computed(() => {
    const source = debouncedCoverUrl.value || DEFAULT_ARTICLE_COVER_URL;
    if (coverPreviewFailed.value) {
        return resolveMediaUrl(DEFAULT_ARTICLE_COVER_URL, '');
    }
    let url = resolveMediaUrl(source, '');
    if (coverRetryKey.value > 0) {
        url += (url.includes('?') ? '&' : '?') + `_t=${coverRetryKey.value}`;
    }
    return url;
});
const isUsingDefaultCover = computed(() => !draft.coverUrl || draft.coverUrl === DEFAULT_ARTICLE_COVER_URL);

watch(() => draft.coverUrl, (newUrl) => {
    coverPreviewFailed.value = false;
    coverRetryCount.value = 0;
    coverRetryKey.value = 0;
    if (coverRetryTimer) {
        clearTimeout(coverRetryTimer);
        coverRetryTimer = null;
    }
    if (coverUrlDebounceTimer) clearTimeout(coverUrlDebounceTimer);
    // 外部设置（上传/加载）直接更新预览，用户输入则等待停笔后再加载
    if (hydratingDraft.value) {
        debouncedCoverUrl.value = newUrl;
    } else {
        coverUrlDebounceTimer = setTimeout(() => {
            debouncedCoverUrl.value = newUrl;
        }, 2000);
    }
});

function generateSlug(text) {
    if (!text) {
        return '';
    }
    return text
        .toLowerCase()
        .replace(/[^\w一-鿿]+/g, '-')
        .replace(/^-+|-+$/g, '')
        .substring(0, 200);
}

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
        coverUrl: currentDraft.coverUrl || '',
        slug: currentDraft.slug || '',
        seoTitle: currentDraft.seoTitle || '',
        seoDescription: currentDraft.seoDescription || '',
        scheduledPublishAt: currentDraft.scheduledPublishAt || ''
    });
}

watch(() => draft.title, (newTitle) => {
    if (!draft.slug && newTitle) {
        draft.slug = generateSlug(newTitle);
    }
});

function parseTags(sourceTags) {
    const source = Array.isArray(sourceTags) ? sourceTags : String(sourceTags || '').split(',');
    return source.map((item) => String(item).trim()).filter(Boolean);
}

function appendDraftTag(tagName) {
    const tag = String(tagName || '').trim();
    if (!tag) {
        return;
    }
    const currentTags = parseTags(draft.tags);
    if (currentTags.some((item) => item.toLowerCase() === tag.toLowerCase())) {
        return;
    }
    draft.tags = [...currentTags, tag].join(', ');
}

function stripMarkdown(source) {
    return String(source || '')
        .replace(/!\[[^\]]*]\([^)]+\)/g, '')
        .replace(/\[[^\]]*]\(([^)]+)\)/g, '$1')
        .replace(/```[\s\S]*?```/g, ' ')
        .replace(/`([^`]+)`/g, '$1')
        .replace(/^#{1,6}\s+/gm, '')
        .replace(/^[>\-*+\d.\s]+/gm, '')
        .replace(/[*_~|>#-]/g, ' ')
        .replace(/\s+/g, ' ')
        .trim();
}

function truncateText(source, maxLength) {
    if (!source || source.length <= maxLength) {
        return source;
    }
    return `${source.slice(0, maxLength - 1).trim()}...`;
}

function applyDraft(source = {}) {
    draft.title = source.title || '';
    draft.summary = source.summary || '';
    draft.content = source.content || '';
    draft.category = source.category || categoryOptions.value[0] || '';
    draft.tags = Array.isArray(source.tags) ? source.tags.join(', ') : (source.tags || '');
    draft.coverUrl = source.coverUrl || '';
    debouncedCoverUrl.value = source.coverUrl || '';
    draft.slug = source.slug || '';
    draft.seoTitle = source.seoTitle || '';
    draft.seoDescription = source.seoDescription || '';
    draft.scheduledPublishAt = normalizeDateTimeLocal(source.scheduledPublishAt || '');
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

function clearStoredDraft(targetKey = storageKey.value) {
    localStorage.removeItem(targetKey);
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

function normalizeDateTimeLocal(value) {
    if (!value) {
        return '';
    }
    const normalized = String(value).trim().replace(' ', 'T');
    const matched = normalized.match(/^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2})/);
    return matched ? matched[1] : normalized;
}

function formatScheduledPublishAt(value) {
    const normalized = normalizeDateTimeLocal(value);
    if (!normalized) {
        return '';
    }
    const date = new Date(normalized);
    if (Number.isNaN(date.getTime())) {
        return normalized.replace('T', ' ');
    }
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
}

function ensureScheduledTimeValid() {
    if (!draft.scheduledPublishAt) {
        statusMessage.value = '请先选择定时发布时间';
        feedbackType.value = 'warning';
        return false;
    }
    const scheduledTime = new Date(draft.scheduledPublishAt).getTime();
    if (Number.isNaN(scheduledTime) || scheduledTime <= Date.now()) {
        statusMessage.value = '定时发布时间必须晚于当前时间';
        feedbackType.value = 'warning';
        return false;
    }
    return true;
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
    const defaultSource = createDefaultDraft();
    const storedDraft = readStoredDraft(storageKey.value);
    currentArticleStatus.value = 'DRAFT';

    hydratingDraft.value = true;
    applyDraft(defaultSource);
    hydratingDraft.value = false;
    syncSavedSnapshot();

    if (storedDraft && createDraftSnapshot(storedDraft) !== createDraftSnapshot(defaultSource)) {
        recoveryInfo.value = {
            draft: storedDraft,
            savedAt: storedDraft.savedAt,
            storageKey: storageKey.value
        };
        statusMessage.value = `检测到一份未发布本地草稿，保存时间 ${formatSavedAt(storedDraft.savedAt)}`;
        feedbackType.value = 'warning';
        return;
    }

    recoveryInfo.value = null;
    statusMessage.value = '';
    feedbackType.value = 'info';
}

async function fetchArticle() {
    if (!isEditMode.value) {
        await setupNewDraft();
        return;
    }
    pageLoading.value = true;
    try {
        const article = await getEditableArticleApi(route.params.id);
        currentArticleStatus.value = article.status || 'DRAFT';
        const serverDraft = {
            title: article.title || '',
            summary: article.summary || '',
            content: article.rawContent || '',
            category: article.category || categoryOptions.value[0] || '',
            tags: Array.isArray(article.tags) ? article.tags.join(', ') : '',
            coverUrl: article.coverUrl || defaultDraft.coverUrl,
            slug: article.slug || '',
            seoTitle: article.seoTitle || '',
            seoDescription: article.seoDescription || '',
            scheduledPublishAt: article.scheduledPublishAt || ''
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
        currentArticleStatus.value = 'DRAFT';
        statusMessage.value = error.message || '文章加载失败';
        feedbackType.value = 'error';
    } finally {
        pageLoading.value = false;
    }
}

async function refreshPublishValidation(options = {}) {
    const silent = options.silent === true;
    publishValidationLoading.value = !silent;
    publishValidationError.value = '';
    try {
        const result = await validateArticleForPublishApi(draft);
        publishValidation.value = {
            publishable: Boolean(result?.publishable),
            errors: result?.errors || [],
            warnings: result?.warnings || [],
            checks: result?.checks || []
        };
        validationErrors.value = publishValidation.value.errors;
    } catch (error) {
        publishValidationError.value = error.message || '发布检查暂时不可用';
    } finally {
        publishValidationLoading.value = false;
    }
}

function schedulePublishValidation() {
    if (!getToken()) {
        publishValidationError.value = '';
        validationErrors.value = [];
        return;
    }
    if (publishValidationTimer) {
        window.clearTimeout(publishValidationTimer);
    }
    publishValidationTimer = window.setTimeout(() => {
        refreshPublishValidation({ silent: true });
    }, 450);
}

async function ensurePublishValidationPassed() {
    await refreshPublishValidation();
    if (!publishValidation.value.publishable) {
        statusMessage.value = '发布前请先处理校验项中的阻塞问题';
        feedbackType.value = 'warning';
        return false;
    }
    return true;
}

async function persistArticle(status, options = {}) {
    if (publishLoading.value) {
        return null;
    }
    clearAutoSaveTimer();
    const actionText = status === 'PUBLISHED'
        ? '发布'
        : (status === 'SCHEDULED' ? '定时发布' : '保存草稿');
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
    if (status === 'SCHEDULED' && !ensureScheduledTimeValid()) {
        return null;
    }
    if (status === 'PUBLISHED' || status === 'SCHEDULED') {
        const canPublish = await ensurePublishValidationPassed();
        if (!canPublish) {
            return null;
        }
    }
    if (options.skipSensitiveWarning !== true) {
        const sensitiveHits = await findArticleWarnSensitiveWords(draft);
        if (sensitiveHits.length) {
            const words = formatWarnSensitiveWords(sensitiveHits);
            statusMessage.value = `存在敏感词 ${words}，请修改或确认继续${actionText}`;
            feedbackType.value = 'warning';
            openConfirmDialog({
                eyebrow: '敏感词提醒',
                title: '内容包含警告词',
                message: `存在敏感词 ${words}，提交后会自动替换为 ***。是否确认${actionText}？`,
                confirmText: `确认${actionText}`,
                cancelText: '返回修改',
                tone: 'warning',
                onConfirm: async () => {
                    await persistArticle(status, { skipSensitiveWarning: true });
                }
            });
            return null;
        }
    }

    publishLoading.value = true;
    statusMessage.value = status === 'PUBLISHED'
        ? '正在发布文章，请稍候...'
        : (status === 'SCHEDULED' ? '正在设置定时发布，请稍候...' : '正在保存草稿，请稍候...');
    feedbackType.value = 'info';
    publishedArticle.value = null;
    const previousStorageKey = storageKey.value;

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
            coverUrl: article.coverUrl,
            slug: article.slug || '',
            seoTitle: article.seoTitle || '',
            seoDescription: article.seoDescription || '',
            scheduledPublishAt: article.scheduledPublishAt || ''
        });
        hydratingDraft.value = false;
        currentArticleStatus.value = article.status || status;
        if (!isEditMode.value) {
            allowRouteLeave.value = true;
            await router.replace(`/editor/${article.id}`);
            allowRouteLeave.value = false;
        }
        clearStoredDraft(previousStorageKey);
        recoveryInfo.value = null;
        if (status === 'PUBLISHED') {
            publishedArticle.value = article;
            statusMessage.value = `发布成功，文章 ID：${article.id}`;
            feedbackType.value = 'success';
        } else if (status === 'SCHEDULED') {
            statusMessage.value = `已设置定时发布：${formatScheduledPublishAt(article.scheduledPublishAt || draft.scheduledPublishAt)}`;
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

async function scheduleArticle() {
    await persistArticle('SCHEDULED');
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
        debouncedCoverUrl.value = draft.coverUrl;
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
    coverRetryCount.value = 0;
    coverRetryKey.value = 0;
    if (coverRetryTimer) {
        clearTimeout(coverRetryTimer);
        coverRetryTimer = null;
    }
}

function handleCoverPreviewError() {
    if (!debouncedCoverUrl.value) {
        return;
    }
    if (coverRetryCount.value >= 5) {
        coverPreviewFailed.value = true;
        statusMessage.value = '封面图片暂时无法访问，请检查链接或稍后重试';
        feedbackType.value = 'error';
        return;
    }
    coverRetryCount.value++;
    const delay = coverRetryCount.value * 1000;
    if (coverRetryTimer) clearTimeout(coverRetryTimer);
    coverRetryTimer = setTimeout(() => {
        coverRetryKey.value++;
    }, delay);
}

function viewPublishedArticle() {
    if (publishedArticle.value?.id) {
        allowRouteLeave.value = true;
        const slug = publishedArticle.value.slug;
        const url = slug ? `/articles/${publishedArticle.value.id}-${slug}` : `/articles/${publishedArticle.value.id}`;
        router.push(url);
    }
}

function continueWriting() {
    publishedArticle.value = null;
    statusMessage.value = '可以继续编辑当前内容';
    feedbackType.value = 'info';
}

async function togglePreview() {
    previewVisible.value = !previewVisible.value;
    if (previewVisible.value) {
        await refreshPublishValidation({ silent: true });
    }
}

function handleTocNavigate({ index }) {
    if (previewVisible.value) {
        return;
    }
    richEditorRef.value?.scrollToHeadingByIndex(index);
}

// ── 版本历史处理 ──────────────────────────────────────────────────────
async function openVersionDrawer() {
    if (!isEditMode.value) return;
    showVersionDrawer.value = true;
    versionPreview.value = null;
    versionListLoading.value = true;
    try {
        versionList.value = await listArticleVersionsApi(route.params.id);
    } catch {
        versionList.value = [];
    } finally {
        versionListLoading.value = false;
    }
}

function closeVersionDrawer() {
    showVersionDrawer.value = false;
    versionPreview.value = null;
}

async function previewVersion(versionNo) {
    versionPreviewLoading.value = true;
    versionPreview.value = null;
    try {
        versionPreview.value = await getArticleVersionApi(route.params.id, versionNo);
    } catch {
        versionPreview.value = null;
    } finally {
        versionPreviewLoading.value = false;
    }
}

async function doRestoreVersion(versionNo) {
    versionRestoring.value = true;
    try {
        const article = await restoreArticleVersionApi(route.params.id, versionNo);
        hydratingDraft.value = true;
        applyDraft({
            title: article.title,
            summary: article.summary,
            content: article.rawContent,
            category: article.category,
            tags: article.tags,
            coverUrl: article.coverUrl,
            slug: article.slug || '',
            seoTitle: article.seoTitle || '',
            seoDescription: article.seoDescription || '',
            scheduledPublishAt: article.scheduledPublishAt || ''
        });
        hydratingDraft.value = false;
        currentArticleStatus.value = article.status || 'DRAFT';
        syncSavedSnapshot();
        statusMessage.value = `已恢复为版本 v${versionNo} 的草稿，请确认后保存`;
        feedbackType.value = 'success';
        closeVersionDrawer();
    } catch (err) {
        statusMessage.value = err.message || '版本恢复失败';
        feedbackType.value = 'error';
    } finally {
        versionRestoring.value = false;
    }
}

function scrollToTop() {
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function handleScroll() {
    showBackToTop.value = window.scrollY > 500;
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
        clearAutoSaveTimer();
        autoSaveStatus.value = '';
        publishedArticle.value = null;
        previewVisible.value = false;
        await fetchArticle();
    }
);

watch(draft, () => {
    if (!pageLoading.value && !hydratingDraft.value) {
        persistLocalDraft();
        schedulePublishValidation();
        scheduleAutoSave();
    }
}, { deep: true });

onBeforeRouteLeave((to) => {
    if (allowRouteLeave.value || !hasUnsavedChanges.value) {
        return true;
    }
    pendingLeaveTarget.value = to.fullPath;
    openConfirmDialog({
        eyebrow: '离开编辑页确认',
        title: '还有未保存改动',
        message: '当前内容还没有保存，离开后可能丢失刚刚的修改。确定继续离开吗？',
        confirmText: '仍然离开',
        tone: 'warning',
        onConfirm: async () => {
            allowRouteLeave.value = true;
            if (pendingLeaveTarget.value) {
                await router.push(pendingLeaveTarget.value);
            } else {
                await router.back();
            }
        }
    });
    return false;
});

onMounted(async () => {
    window.addEventListener('beforeunload', handleBeforeUnload);
    window.addEventListener('scroll', handleScroll);
    await fetchMetadata();
    await fetchArticle();
    if (getToken()) {
        await refreshPublishValidation({ silent: true });
    }
});

onUnmounted(() => {
    window.removeEventListener('beforeunload', handleBeforeUnload);
    window.removeEventListener('scroll', handleScroll);
    if (publishValidationTimer) {
        window.clearTimeout(publishValidationTimer);
    }
    if (autoSaveTimer) {
        clearAutoSaveTimer();
    }
    if (coverUrlDebounceTimer) {
        clearTimeout(coverUrlDebounceTimer);
    }
    if (coverRetryTimer) {
        clearTimeout(coverRetryTimer);
    }
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
                    <button
                        v-if="isEditMode"
                        type="button"
                        class="editor-versions-btn"
                        :disabled="publishLoading || pageLoading"
                        data-testid="editor-versions-button"
                        @click="openVersionDrawer"
                    >历史版本</button>
                    <button type="button" :disabled="publishLoading || pageLoading" data-testid="editor-preview-button" @click="togglePreview">
                        {{ previewVisible ? '收起预览' : '发布前预览' }}
                    </button>
                    <button type="button" :disabled="publishLoading || pageLoading" data-testid="editor-save-draft" @click="saveDraft">保存草稿</button>
                    <button type="button" :disabled="publishLoading || pageLoading" data-testid="editor-schedule-button" @click="scheduleArticle">
                        定时发布
                    </button>
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

            <p v-if="statusMessage && showPersistentStatus" :class="['editor-action-status', feedbackType]">
                {{ statusMessage }}
            </p>

            <section v-if="recoveryInfo" class="editor-recovery-banner">
                <div>
                    <p class="eyebrow">检测到本地恢复内容</p>
                    <strong>{{ isEditMode ? '这篇文章有一份未同步的本地编辑稿。' : '你上次有一份未发布草稿。' }}</strong>
                    <span>
                        保存时间 {{ formatSavedAt(recoveryInfo.savedAt) }}，你可以选择恢复继续写，或丢弃它{{ isEditMode ? '继续使用当前内容' : '从空白文章开始' }}。
                    </span>
                </div>
                <div class="editor-recovery-actions">
                    <button class="primary-action" type="button" @click="restoreRecoveredDraft">恢复本地草稿</button>
                    <button class="recovery-secondary-action" type="button" @click="discardRecoveredDraft">
                        {{ isEditMode ? '使用当前版本' : '新建空白文章' }}
                    </button>
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

            <div v-else-if="showQuietStatus" class="editor-status-inline" :class="feedbackType">
                <span class="editor-status-dot"></span>
                <span>{{ statusMessage }}</span>
            </div>

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
                <section class="editor-seo-preview-section" aria-label="SEO 预览">
                    <div class="editor-seo-preview-card">
                        <p class="editor-seo-preview-label">Google 搜索结果预览</p>
                        <div class="editor-seo-google-card">
                            <p class="editor-seo-google-url">{{ seoPreviewUrl }}</p>
                            <h3 class="editor-seo-google-title">{{ seoPreviewTitle }}</h3>
                            <p class="editor-seo-google-desc">{{ seoPreviewDescription }}</p>
                        </div>
                    </div>
                    <div class="editor-seo-health-section">
                        <p class="editor-seo-preview-label">SEO 健康检查</p>
                        <ul class="editor-seo-health-list">
                            <li v-for="check in seoHealthChecks" :key="check.key" :class="['editor-seo-health-item', check.ok ? 'ok' : 'warn']">
                                <span class="editor-seo-health-icon">{{ check.ok ? '✓' : '○' }}</span>
                                <span class="editor-seo-health-label">{{ check.label }}</span>
                                <span class="editor-seo-health-hint">{{ check.hint }}</span>
                            </li>
                        </ul>
                    </div>
                    <div v-if="seoKeywords.length" class="editor-seo-keywords-section">
                        <p class="editor-seo-preview-label">关键词密度</p>
                        <div class="editor-seo-keywords-list">
                            <span v-for="kw in seoKeywords" :key="kw.word" class="editor-seo-keyword-tag">
                                {{ kw.word }}<em class="editor-seo-keyword-count">{{ kw.count }}</em>
                            </span>
                        </div>
                    </div>
                </section>
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
                 decoding="async">
                <MarkdownPreview v-if="draft.content.trim()" :content="draft.content" />
                <div v-else class="editor-preview-empty">正文还没有内容，继续往下写就好。</div>
            </section>

            <section v-if="!previewVisible">
            <label class="editor-title">
                <span class="sr-only">文章标题</span>
                <input v-model="draft.title" type="text" placeholder="请输入文章标题" maxlength="120" data-testid="editor-title-input">
            </label>

            <label class="editor-summary">
                <span>文章摘要</span>
                <textarea v-model="draft.summary" placeholder="用一两句话介绍这篇文章" maxlength="300" data-testid="editor-summary-input"></textarea>
                <span class="editor-charcount">{{ draft.summary.length }} / 300</span>
            </label>

            <RichMarkdownEditor ref="richEditorRef" v-model="draft.content" />
            <div class="editor-feedback">
                <span>正文 {{ wordCount }} 字 · 预计阅读 {{ readingMinutes }} 分钟</span>
                <span v-if="autoSaveLabel" class="editor-autosave-status">{{ autoSaveLabel }}</span>
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
                :target-selector="previewVisible ? '.editor-preview-panel .markdown-preview' : '.rich-markdown-editor .ProseMirror'"
                :use-custom-navigation="!previewVisible"
                :refresh-on-content-change="!previewVisible"
                @navigate="handleTocNavigate"
            />
            <section class="editor-category-section">
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
            <section class="editor-side-block">
                <p class="eyebrow">标签</p>
                <input
                    v-model="draft.tags"
                    type="text"
                    maxlength="500"
                    list="editor-tag-options"
                    :placeholder="tagOptions.length ? `例如：${tagOptions.slice(0, 3).join(', ')}` : '多个标签用英文逗号分隔'"
                >
                <datalist id="editor-tag-options">
                    <option v-for="tag in tagOptions" :key="tag" :value="tag"></option>
                </datalist>
                <div v-if="tagOptions.length" class="editor-tag-suggestions">
                    <button
                        v-for="tag in tagOptions.slice(0, 8)"
                        :key="tag"
                        type="button"
                        @click="appendDraftTag(tag)"
                    >
                        {{ tag }}
                    </button>
                </div>
                <p v-if="unconfiguredDraftTags.length" class="editor-tag-warning">
                    未收录标签「{{ unconfiguredDraftTags.join('、') }}」不会出现在搜索页标签筛选项里。
                </p>
            </section>
            <section class="editor-side-block editor-schedule-section">
                <div>
                    <p class="eyebrow">定时发布</p>
                    <span v-if="currentArticleStatus === 'SCHEDULED'" class="editor-schedule-status">
                        已计划 {{ formatScheduledPublishAt(draft.scheduledPublishAt) }}
                    </span>
                </div>
                <input v-model="draft.scheduledPublishAt" type="datetime-local">
            </section>
            <section class="upload-box">
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
                     decoding="async">
                    <div class="cover-card-overlay">
                        <span>{{ isUsingDefaultCover ? '未上传时将使用系统默认封面' : '这张图会作为文章封面展示' }}</span>
                    </div>
                </div>
                <p v-if="isUsingDefaultCover" class="cover-help-text">
                    当前会使用系统默认封面，你可以随时上传一张更贴合主题的封面。
                </p>
                <label class="cover-path-field">
                    <span>图片路径</span>
                    <small>如果你已经有现成的封面地址，也可以直接填写公开图片链接。</small>
                    <input
                        v-model.trim="draft.coverUrl"
                        type="text"
                        maxlength="500"
                        placeholder="/api/uploads/files/2026/04/cover.jpg 或 https://example.com/cover.jpg"
                    >
                </label>
                <button
                    type="button"
                    class="cover-upload-button"
                    :disabled="coverUploading || publishLoading || pageLoading"
                    @click="triggerCoverPicker"
                >
                    {{ coverUploading ? '上传中...' : (isUsingDefaultCover ? '上传封面' : '更换封面') }}
                </button>
            </section>
            <details class="editor-seo-section">
                <summary class="editor-seo-summary">
                    <span>SEO 设置</span>
                    <span class="editor-seo-summary-hint">Slug、标题与描述</span>
                </summary>
                <div class="editor-seo-body">
                    <label class="editor-seo-field">
                        <span>URL Slug</span>
                        <small>URL 中标识这篇文章的友好名称，用于搜索引擎和分享链接。</small>
                        <input v-model="draft.slug" type="text" maxlength="255" placeholder="my-article-slug">
                        <code class="editor-seo-preview">/articles/{{ editorArticleId }}{{ draft.slug ? '-' + draft.slug : '' }}</code>
                    </label>
                    <label class="editor-seo-field">
                        <span>SEO 标题</span>
                        <small>显示在搜索引擎结果中的标题。留空则使用文章标题。</small>
                        <input v-model="draft.seoTitle" type="text" maxlength="255" :placeholder="draft.title || '文章标题'">
                        <span class="editor-charcount">{{ draft.seoTitle.length }} / 255</span>
                    </label>
                    <label class="editor-seo-field">
                        <span>SEO 描述</span>
                        <small>显示在搜索引擎结果中的描述文本。推荐 50&#x2013;160 个字符。</small>
                        <textarea v-model="draft.seoDescription" placeholder="用一句话概括这篇文章的内容..." maxlength="500" rows="3"></textarea>
                        <span class="editor-seo-charcount">{{ draft.seoDescription.length }} / 500</span>
                    </label>
                </div>
            </details>
        </aside>
    </main>

    <button
        v-show="showBackToTop"
        class="back-to-top"
        type="button"
        @click="scrollToTop"
    >
        ↑
    </button>

    <ConfirmDialog
        :visible="confirmDialog.visible"
        :eyebrow="confirmDialog.eyebrow"
        :title="confirmDialog.title"
        :message="confirmDialog.message"
        :confirm-text="confirmDialog.confirmText"
        :cancel-text="confirmDialog.cancelText"
        :tone="confirmDialog.tone"
        :loading="confirmDialog.loading"
        @close="closeConfirmDialog"
        @confirm="executeConfirmDialog"
    />

    <!-- 版本历史抽屉 -->
    <Teleport to="body">
        <Transition name="version-drawer-fade">
            <div v-if="showVersionDrawer" class="version-drawer-backdrop" data-testid="version-drawer" @click.self="closeVersionDrawer">
                <div class="version-drawer">
                    <div class="version-drawer-header">
                        <div>
                            <p class="eyebrow">历史版本</p>
                            <h2>文章版本记录</h2>
                        </div>
                        <button type="button" class="version-drawer-close" @click="closeVersionDrawer">✕</button>
                    </div>
                    <div class="version-drawer-body">
                        <div class="version-list">
                            <p v-if="versionListLoading" class="version-list-tip">正在加载版本列表...</p>
                            <p v-else-if="!versionList.length" class="version-list-tip">暂无历史版本，保存草稿或发布后会自动记录版本。</p>
                            <ul v-else class="version-items">
                                <li
                                    v-for="v in versionList"
                                    :key="v.versionNo"
                                    :class="['version-item', versionPreview && versionPreview.versionNo === v.versionNo ? 'active' : '']"
                                    @click="previewVersion(v.versionNo)"
                                >
                                    <div class="version-item-meta">
                                        <span class="version-badge">v{{ v.versionNo }}</span>
                                        <span class="version-time">{{ v.savedAt }}</span>
                                    </div>
                                    <p class="version-title">{{ v.title || '（无标题）' }}</p>
                                </li>
                            </ul>
                        </div>
                        <div class="version-preview-panel">
                            <p v-if="!versionPreview && !versionPreviewLoading" class="version-list-tip">点击左侧版本可预览内容。</p>
                            <p v-if="versionPreviewLoading" class="version-list-tip">加载版本内容...</p>
                            <template v-if="versionPreview && !versionPreviewLoading">
                                <div class="version-preview-header">
                                    <div>
                                        <span class="version-badge">v{{ versionPreview.versionNo }}</span>
                                        <strong class="version-preview-title">{{ versionPreview.title }}</strong>
                                    </div>
                                    <button
                                        type="button"
                                        class="primary-action version-restore-btn"
                                        :disabled="versionRestoring"
                                        @click="doRestoreVersion(versionPreview.versionNo)"
                                    >{{ versionRestoring ? '恢复中...' : '恢复到此版本' }}</button>
                                </div>
                                <p v-if="versionPreview.summary" class="version-preview-summary">{{ versionPreview.summary }}</p>
                                <div class="version-preview-content">
                                    <MarkdownPreview v-if="versionPreview.content" :content="versionPreview.content" />
                                    <p v-else class="version-list-tip">该版本无正文内容。</p>
                                </div>
                            </template>
                        </div>
                    </div>
                </div>
            </div>
        </Transition>
    </Teleport>
</template>

<style scoped>
.editor-recovery-banner,
.editor-preview-panel {
    display: grid;
    gap: 14px;
    padding: 18px 20px;
    background: var(--surface);
    border: 1px solid rgba(37, 99, 235, 0.08);
    border-radius: var(--radius-lg);
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

.recovery-secondary-action {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-height: 42px;
    padding: 0 16px;
    color: var(--text);
    font-size: 14px;
    font-weight: 700;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    box-shadow: 0 8px 18px rgba(15, 23, 42, 0.06);
    cursor: pointer;
    transition: background 0.16s ease, border-color 0.16s ease, color 0.16s ease, transform 0.16s ease;
}

.recovery-secondary-action:hover {
    color: var(--brand-strong);
    background: rgba(37, 99, 235, 0.06);
    border-color: rgba(37, 99, 235, 0.22);
    transform: translateY(-1px);
}

.recovery-secondary-action:focus-visible {
    outline: 3px solid rgba(37, 99, 235, 0.16);
    outline-offset: 2px;
}

.editor-preview-head {
    display: flex;
    gap: 14px;
    align-items: end;
    justify-content: space-between;
}

.editor-seo-preview-section {
    display: grid;
    gap: 16px;
}

.editor-seo-preview-card {
    display: grid;
    gap: 6px;
    padding: 14px 16px;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.editor-seo-google-card {
    padding: 10px 12px;
    background: #ffffff;
    border: 1px solid #e0e0e0;
    border-radius: 8px;
}

.editor-seo-google-url {
    margin: 0 0 2px;
    color: #006621;
    font-size: 13px;
    line-height: 1.4;
    word-break: break-all;
}

.editor-seo-google-title {
    margin: 0 0 2px;
    color: #1a0dab;
    font-size: 18px;
    line-height: 1.35;
    font-weight: 600;
    cursor: pointer;
}

.editor-seo-google-title:hover {
    text-decoration: underline;
}

.editor-seo-google-desc {
    margin: 0;
    color: #545454;
    font-size: 13px;
    line-height: 1.55;
    word-break: break-word;
}

.editor-seo-preview-label {
    margin: 0 0 8px;
    color: var(--muted);
    font-size: 12px;
    font-weight: 700;
}

.editor-seo-health-section {
    padding: 14px 16px;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.editor-seo-health-list {
    display: grid;
    gap: 6px;
    margin: 0;
    padding: 0;
    list-style: none;
}

.editor-seo-health-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 4px 0;
    font-size: 13px;
    line-height: 1.4;
}

.editor-seo-health-icon {
    flex-shrink: 0;
    width: 20px;
    height: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    font-size: 11px;
    font-weight: 700;
}

.editor-seo-health-item.ok .editor-seo-health-icon {
    background: #d1fae5;
    color: #065f46;
}

.editor-seo-health-item.warn .editor-seo-health-icon {
    background: #fef3c7;
    color: #92400e;
}

.editor-seo-health-label {
    font-weight: 600;
    color: var(--text);
    white-space: nowrap;
}

.editor-seo-health-hint {
    color: var(--muted);
    font-size: 12px;
}

.editor-seo-keywords-section {
    padding: 14px 16px;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.editor-seo-keywords-list {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
}

.editor-seo-keyword-tag {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 3px 8px;
    font-size: 12px;
    color: var(--text);
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: 4px;
}

.editor-seo-keyword-count {
    font-style: normal;
    font-size: 10px;
    color: var(--muted);
    padding: 1px 3px;
    background: var(--surface-soft);
    border-radius: 3px;
}
.editor-seo-preview-desc {
    color: var(--text);
    font-size: 13px;
    line-height: 1.7;
}

.editor-seo-preview-metrics {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    color: var(--muted);
    font-size: 12px;
}

.editor-seo-preview-metrics .warning {
    color: #b45309;
}

.editor-preview-meta span,
.editor-preview-tags span {
    display: inline-flex;
    align-items: center;
    min-height: 30px;
    padding: 0 10px;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
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
    border-radius: var(--radius-lg);
}

.editor-preview-empty {
    padding: 22px 18px;
    background: var(--surface-soft);
    border: 1px dashed var(--line);
    border-radius: var(--radius-lg);
}

.upload-box {
    display: grid;
    gap: 14px;
    padding: 18px;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    background: var(--surface);
    box-shadow: none;
}

.editor-side {
    display: grid;
    gap: 14px;
}

.editor-side-block {
    display: grid;
    gap: 10px;
}

.editor-tag-suggestions {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
}

.editor-tag-suggestions button {
    min-height: 28px;
    padding: 0 8px;
    color: var(--brand);
    font-size: 12px;
    font-weight: 700;
    background: var(--brand-soft);
    border: 1px solid rgba(37, 99, 235, 0.18);
    border-radius: var(--radius-sm);
    cursor: pointer;
}

.editor-tag-suggestions button:hover {
    color: var(--brand-strong);
    border-color: rgba(37, 99, 235, 0.32);
}

.editor-tag-warning {
    margin: 0;
    color: var(--warning);
    font-size: 12px;
    line-height: 1.6;
}

.editor-schedule-section {
    padding: 14px 16px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.editor-schedule-section > div {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
}

.editor-schedule-section input {
    width: 100%;
}

.editor-schedule-status {
    color: var(--brand-strong);
    font-size: 12px;
    font-weight: 700;
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
    border-radius: var(--radius-sm);
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
    background: rgba(37, 99, 235, 0.12);
    border: 1px solid rgba(37, 99, 235, 0.16);
}

.cover-card-preview {
    position: relative;
    overflow: hidden;
    border-radius: var(--radius-lg);
    border: 1px solid rgba(15, 23, 42, 0.08);
    background: var(--surface-soft);
    aspect-ratio: 16 / 10;
    box-shadow: none;
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
    background: linear-gradient(180deg, rgba(15, 23, 42, 0.02), rgba(15, 23, 42, 0.46));
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
    background: rgba(37, 99, 235, 0.05);
    border: 1px solid rgba(37, 99, 235, 0.08);
    border-radius: var(--radius-lg);
}

.cover-path-field {
    display: grid;
    gap: 8px;
}

.cover-path-field span {
    color: var(--text);
    font-size: 13px;
    font-weight: 600;
}

.cover-path-field small {
    color: var(--muted);
    font-size: 12px;
    line-height: 1.6;
}

.cover-path-field input {
    width: 100%;
}

.cover-upload-button {
    display: inline-flex;
    justify-content: center;
    align-items: center;
    min-height: 44px;
    padding: 0 16px;
    border: 0;
    border-radius: var(--radius-sm);
    background: var(--brand);
    color: #ffffff;
    font-size: 14px;
    font-weight: 700;
    cursor: pointer;
    transition: filter 0.16s ease;
    box-shadow: none;
}

.cover-upload-button:hover {
    filter: brightness(1.03);
}

.cover-upload-button:disabled {
    opacity: 0.58;
    cursor: not-allowed;
    transform: none;
    filter: none;
    box-shadow: none;
}

.editor-seo-section {
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    background: var(--surface);
    overflow: hidden;
}

.editor-seo-summary {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 14px 18px;
    cursor: pointer;
    font-weight: 600;
    color: var(--text);
    list-style: none;
}

.editor-seo-summary::-webkit-details-marker {
    display: none;
}

.editor-seo-summary-hint {
    color: var(--muted);
    font-weight: 400;
    font-size: 12px;
}

.editor-seo-body {
    display: grid;
    gap: 14px;
    padding: 0 18px 18px;
    border-top: 1px solid var(--line);
}

.editor-seo-field {
    display: grid;
    gap: 6px;
    padding-top: 14px;
}

.editor-seo-field input,
.editor-seo-field textarea {
    padding: 8px 10px;
    font-size: 13px;
    line-height: 1.5;
    color: var(--text);
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    outline: none;
    transition: border-color 0.15s, box-shadow 0.15s;
}

.editor-seo-field input:focus,
.editor-seo-field textarea:focus {
    border-color: var(--brand);
    box-shadow: 0 0 0 2px var(--brand-soft);
}

.editor-seo-field textarea {
    resize: vertical;
    min-height: 72px;
    font-family: inherit;
}

.editor-seo-field input::placeholder,
.editor-seo-field textarea::placeholder {
    color: var(--muted);
}

.editor-seo-field span {
    color: var(--text);
    font-size: 13px;
    font-weight: 600;
}

.editor-seo-field small {
    color: var(--muted);
    font-size: 12px;
    line-height: 1.6;
}

.editor-seo-preview {
    padding: 8px 10px;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    font-size: 12px;
    color: var(--muted);
    word-break: break-all;
}

.editor-seo-charcount,
.editor-charcount {
    text-align: right;
    font-size: 12px;
    color: var(--muted);
}

.editor-status-inline {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    margin: 0 0 14px;
    padding: 6px 0 2px;
    color: var(--muted);
    font-size: 13px;
}

.editor-status-inline.success {
    color: var(--success);
}

.editor-status-inline.info {
    color: var(--brand);
}

.editor-status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: currentColor;
    opacity: 0.85;
}

@media (max-width: 760px) {
    .editor-recovery-banner {
        grid-template-columns: 1fr;
    }

    .editor-recovery-actions .primary-action,
    .recovery-secondary-action {
        width: 100%;
    }

    .editor-preview-head {
        flex-direction: column;
        align-items: stretch;
    }

    .cover-card-head {
        flex-direction: column;
    }
}

/* ── 版本历史按钮 ─────────────────────────────────────────────────────── */
.editor-versions-btn {
    color: var(--brand);
    background: rgba(37, 99, 235, 0.08);
    border: 1px solid rgba(37, 99, 235, 0.18);
    font-size: 13px;
}

.editor-versions-btn:hover {
    background: rgba(37, 99, 235, 0.14);
}

/* ── 自动保存状态 ─────────────────────────────────────────────────────── */
.editor-autosave-status {
    color: var(--success, #16a34a);
    font-size: 12px;
}

/* ── 版本历史抽屉 ─────────────────────────────────────────────────────── */
.version-drawer-backdrop {
    position: fixed;
    inset: 0;
    z-index: 1000;
    background: rgba(15, 23, 42, 0.36);
    display: flex;
    justify-content: flex-end;
}

.version-drawer {
    width: min(860px, 92vw);
    height: 100%;
    display: flex;
    flex-direction: column;
    background: var(--surface);
    box-shadow: -4px 0 24px rgba(15, 23, 42, 0.12);
}

.version-drawer-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    padding: 22px 24px 18px;
    border-bottom: 1px solid var(--line);
    flex-shrink: 0;
}

.version-drawer-header h2 {
    margin: 4px 0 0;
    font-size: 20px;
    color: var(--text);
}

.version-drawer-close {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    background: var(--surface);
    color: var(--muted);
    font-size: 14px;
    cursor: pointer;
    flex-shrink: 0;
}

.version-drawer-close:hover {
    background: var(--surface-soft);
    color: var(--text);
}

.version-drawer-body {
    flex: 1;
    display: grid;
    grid-template-columns: 260px 1fr;
    overflow: hidden;
}

.version-list {
    border-right: 1px solid var(--line);
    overflow-y: auto;
    padding: 14px 0;
}

.version-list-tip {
    padding: 14px 18px;
    color: var(--muted);
    font-size: 13px;
    line-height: 1.7;
}

.version-items {
    list-style: none;
    margin: 0;
    padding: 0;
}

.version-item {
    padding: 12px 18px;
    cursor: pointer;
    border-left: 3px solid transparent;
    transition: background 0.14s ease;
}

.version-item:hover {
    background: var(--surface-soft);
}

.version-item.active {
    background: rgba(37, 99, 235, 0.06);
    border-left-color: var(--brand);
}

.version-item-meta {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 4px;
}

.version-badge {
    display: inline-flex;
    align-items: center;
    min-height: 20px;
    padding: 0 7px;
    background: rgba(37, 99, 235, 0.12);
    color: var(--brand-strong);
    border-radius: var(--radius-sm);
    font-size: 11px;
    font-weight: 700;
}

.version-time {
    color: var(--muted);
    font-size: 12px;
}

.version-title {
    margin: 0;
    font-size: 13px;
    color: var(--text);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.version-preview-panel {
    overflow-y: auto;
    padding: 18px 24px;
    display: flex;
    flex-direction: column;
    gap: 14px;
}

.version-preview-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
}

.version-preview-title {
    margin-left: 8px;
    font-size: 18px;
    color: var(--text);
}

.version-restore-btn {
    flex-shrink: 0;
}

.version-preview-summary {
    margin: 0;
    color: var(--muted);
    font-size: 14px;
    line-height: 1.7;
}

.version-preview-content {
    flex: 1;
}

/* 抽屉入场动画 */
.version-drawer-fade-enter-active,
.version-drawer-fade-leave-active {
    transition: opacity 0.22s ease;
}

.version-drawer-fade-enter-active .version-drawer,
.version-drawer-fade-leave-active .version-drawer {
    transition: transform 0.22s ease;
}

.version-drawer-fade-enter-from,
.version-drawer-fade-leave-to {
    opacity: 0;
}

.version-drawer-fade-enter-from .version-drawer,
.version-drawer-fade-leave-to .version-drawer {
    transform: translateX(40px);
}

@media (max-width: 640px) {
    .version-drawer-body {
        grid-template-columns: 1fr;
        grid-template-rows: auto 1fr;
    }

    .version-list {
        border-right: none;
        border-bottom: 1px solid var(--line);
        max-height: 200px;
    }
}
</style>
