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
import {getMyGrowthApi} from '@/api/growth';
import {uploadImage} from '@/api/uploads';
import SiteHeader from '@/components/SiteHeader.vue';
import ArticleToc from '@/components/ArticleToc.vue';
import MarkdownPreview from '@/components/MarkdownPreview.vue';
import RichMarkdownEditor from '@/components/RichMarkdownEditor.vue';
import ConfirmDialog from '@/components/ConfirmDialog.vue';
import {topics} from '@/data/home';
import {resolveMediaUrl} from '@/utils/media';
import {findArticleWarnSensitiveWords, formatWarnSensitiveWords} from '@/utils/sensitiveWords';
import {useConfirmDialog} from '@/composables/useConfirmDialog';

const FEED_CACHE_PREFIX = 'my-blog:infinite-article-feed:';
function clearFeedCache() {
    try {
        const keys = Object.keys(sessionStorage);
        keys.forEach(key => {
            if (key.startsWith(FEED_CACHE_PREFIX)) {
                sessionStorage.removeItem(key);
            }
        });
    } catch {
        // Ignore storage errors
    }
}

const DRAFT_STORAGE_PREFIX = 'my-blog-editor-draft';
const DEFAULT_ARTICLE_COVER_URL = '/api/uploads/files/default/article-cover.svg';
const AUTO_SAVE_DELAY_MS = 15000;
const MIN_UNLOCK_POINT_PRICE = 10;
const MAX_UNLOCK_POINT_PRICE = 1000000;

const defaultDraft = {
    title: '',
    summary: '',
    content: `# 开始写作

用一篇文章把你最近解决的问题写清楚。`,
    category: '',
    tags: '',
    coverUrl: '',
    slug: '',
    seoTitle: '',
    seoDescription: '',
    needUnlock: false,
    unlockPointPrice: 0,
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
    title: computed(() => isEditMode.value ? '编辑文章 - 小蓝书' : '写文章 - 小蓝书')
});

const draft = reactive({ ...defaultDraft });
const statusMessage = ref('');
const validationErrors = ref([]);
const publishLoading = ref(false);
const publishedArticle = ref(null);
const feedbackType = ref('info');
const categoryOptions = ref(topics.slice(1).map(name => ({ name, groupName: '' })));
const tagOptions = ref([]);
const categorySearchKeyword = ref('');
const expandedCategoryGroups = ref(new Set());
const unavailableCategory = computed(() =>
    draft.category !== '' && !categoryOptions.value.some(c => c.name === draft.category)
);

const normalizeCategoryText = (value) => String(value || '').trim().toLowerCase();

const selectedCategoryGroupName = computed(() => {
    if (!draft.category) return '';
    const cat = categoryOptions.value.find(c => c.name === draft.category);
    return cat?.groupName || '';
});

const groupedCategoryOptions = computed(() => {
    const keyword = normalizeCategoryText(categorySearchKeyword.value);
    const groupedMap = new Map();
    const groupOrder = [];
    categoryOptions.value.forEach((category) => {
        if (keyword && !normalizeCategoryText(category.name).includes(keyword)) {
            return;
        }
        const groupName = category.groupName || '其他';
        if (!groupedMap.has(groupName)) {
            groupedMap.set(groupName, []);
            groupOrder.push(groupName);
        }
        groupedMap.get(groupName).push(category);
    });
    return groupOrder
        .map((name) => ({ name, items: groupedMap.get(name) }))
        .filter((group) => group.items.length > 0);
});

const isCategoryGroupOpen = (groupName) => (
    Boolean(categorySearchKeyword.value.trim())
    || expandedCategoryGroups.value.has(groupName)
    || selectedCategoryGroupName.value === groupName
);

const toggleCategoryGroup = (groupName) => {
    const next = new Set(expandedCategoryGroups.value);
    if (next.has(groupName)) {
        next.delete(groupName);
    } else {
        next.add(groupName);
    }
    expandedCategoryGroups.value = next;
};

const selectPresetCategory = (topic) => {
    draft.category = topic.name;
    expandedCategoryGroups.value = new Set([...expandedCategoryGroups.value, topic.groupName || '其他']);
};
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
const growthAccess = ref(null);
const creatorPrivilegeCodes = computed(() => growthAccess.value?.ownedPrivilegeCodes || []);
const hasPaidArticlePrivilege = computed(() => creatorPrivilegeCodes.value.includes('PAID_ARTICLE_PUBLISH'));
const unlockSwitchDisabled = computed(() => !hasPaidArticlePrivilege.value && !draft.needUnlock);

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
        || draft.needUnlock
        || Number(draft.unlockPointPrice || 0) > 0
        || draft.scheduledPublishAt.trim()
    );
}

function canServerAutoSave() {
    return !isEditMode.value || currentArticleStatus.value === 'DRAFT';
}

function isUnlockConfigValid() {
    return !draft.needUnlock || Number(draft.unlockPointPrice || 0) >= MIN_UNLOCK_POINT_PRICE;
}

function ensureUnlockConfigValid() {
    if (draft.needUnlock && !hasPaidArticlePrivilege.value) {
        statusMessage.value = '当前等级未解锁付费文章发布权限，达到 Lv.4 后可开启积分解锁';
        feedbackType.value = 'warning';
        return false;
    }
    clampDraftUnlockPointPrice();
    if (isUnlockConfigValid()) {
        return true;
    }
    statusMessage.value = `开启积分解锁后，请设置至少 ${MIN_UNLOCK_POINT_PRICE} 的解锁积分`;
    feedbackType.value = 'warning';
    return false;
}

function clampDraftUnlockPointPrice() {
    if (!draft.needUnlock) {
        return;
    }
    const price = Number.parseInt(draft.unlockPointPrice, 10);
    draft.unlockPointPrice = Number.isNaN(price)
        ? MIN_UNLOCK_POINT_PRICE
        : Math.min(MAX_UNLOCK_POINT_PRICE, Math.max(MIN_UNLOCK_POINT_PRICE, price));
}

function onNeedUnlockToggle() {
    if (draft.needUnlock && !hasPaidArticlePrivilege.value) {
        draft.needUnlock = false;
        statusMessage.value = '当前等级未解锁付费文章发布权限，达到 Lv.4 后可开启积分解锁';
        feedbackType.value = 'warning';
        return;
    }
    clampDraftUnlockPointPrice();
}

function onUnlockPointPriceCommit() {
    clampDraftUnlockPointPrice();
}

async function runAutoSaveDraft() {
    if (publishLoading.value || pageLoading.value || recoveryInfo.value || autoSaveInFlight) {
        return;
    }
    if (!canServerAutoSave() || !hasUnsavedChanges.value || !hasAutoSaveableContent() || !getToken()) {
        return;
    }
    if (!isUnlockConfigValid()) {
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
            needUnlock: Boolean(article.needUnlock),
            unlockPointPrice: Number(article.unlockPointPrice || 0),
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
        { key: 'slug', label: 'URL Slug', ok: true,
            hint: draft.slug ? draft.slug : '可选，不填则使用文章 ID，避免同名文章冲突' },
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
        needUnlock: Boolean(currentDraft.needUnlock),
        unlockPointPrice: Number(currentDraft.unlockPointPrice || 0),
        scheduledPublishAt: currentDraft.scheduledPublishAt || ''
    });
}

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
    const sourceCategory = source.category || '';
    draft.category = sourceCategory;
    const sourceCategoryObj = categoryOptions.value.find(c => c.name === sourceCategory);
    if (sourceCategoryObj) {
        expandedCategoryGroups.value = new Set([
            ...expandedCategoryGroups.value,
            sourceCategoryObj.groupName || '其他'
        ]);
    }
    draft.tags = Array.isArray(source.tags) ? source.tags.join(', ') : (source.tags || '');
    draft.coverUrl = source.coverUrl || '';
    debouncedCoverUrl.value = source.coverUrl || '';
    draft.slug = source.slug || '';
    draft.seoTitle = source.seoTitle || '';
    draft.seoDescription = source.seoDescription || '';
    draft.needUnlock = Boolean(source.needUnlock);
    draft.unlockPointPrice = Number(source.unlockPointPrice || 0);
    clampDraftUnlockPointPrice();
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
            categoryOptions.value = categories.map((item) => ({ name: item.name, groupName: item.groupName || '' }));
        }
        if (tags?.length) {
            tagOptions.value = tags.map((item) => item.name);
        }
    } catch (error) {
        categoryOptions.value = topics.slice(1).map(name => ({ name, groupName: '' }));
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
            category: article.category || categoryOptions.value[0]?.name || '',
            tags: Array.isArray(article.tags) ? article.tags.join(', ') : '',
            coverUrl: article.coverUrl || defaultDraft.coverUrl,
            slug: article.slug || '',
            seoTitle: article.seoTitle || '',
            seoDescription: article.seoDescription || '',
            needUnlock: Boolean(article.needUnlock),
            unlockPointPrice: Number(article.unlockPointPrice || 0),
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

async function loadGrowthAccess() {
    if (!getToken()) {
        growthAccess.value = null;
        return;
    }
    try {
        growthAccess.value = await getMyGrowthApi();
    } catch {
        growthAccess.value = null;
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
    if (!ensureUnlockConfigValid()) {
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
            needUnlock: Boolean(article.needUnlock),
            unlockPointPrice: Number(article.unlockPointPrice || 0),
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
            clearFeedCache();
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
        const message = error.message || `${actionText}失败`;
        statusMessage.value = message.includes('URL Slug')
            ? `${message}。如果不需要自定义链接，可以清空右侧 SEO 设置里的 URL Slug 后再提交。`
            : message;
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
        const result = await uploadImage(file, 'cover');
        draft.coverUrl = result.mediumUrl || result.url || defaultDraft.coverUrl;
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

async function writeAnotherArticle() {
    clearStoredDraft(`${DRAFT_STORAGE_PREFIX}:new`);
    publishedArticle.value = null;
    recoveryInfo.value = null;
    statusMessage.value = '';
    feedbackType.value = 'info';
    allowRouteLeave.value = true;
    await router.push('/editor/new');
    allowRouteLeave.value = false;
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
            needUnlock: Boolean(article.needUnlock),
            unlockPointPrice: Number(article.unlockPointPrice || 0),
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

watch(
    () => [
        draft.title, draft.summary, draft.content, draft.category, draft.tags,
        draft.coverUrl, draft.slug, draft.seoTitle, draft.seoDescription,
        draft.needUnlock, draft.unlockPointPrice, draft.scheduledPublishAt
    ],
    () => {
        if (!pageLoading.value && !hydratingDraft.value) {
            persistLocalDraft();
            schedulePublishValidation();
            scheduleAutoSave();
        }
    }
);

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
    await loadGrowthAccess();
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
                    <button type="button" @click="writeAnotherArticle">继续写新文章</button>
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
                        <span :class="['editor-preview-unlock', { active: draft.needUnlock }]">
                            {{ draft.needUnlock ? `${Number(draft.unlockPointPrice || 0)} 积分解锁` : '免费阅读' }}
                        </span>
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
            <section id="article-permission" class="editor-side-block editor-unlock-section">
                <div class="editor-unlock-head">
                    <div>
                        <p class="eyebrow">文章权限</p>
                        <strong>{{ draft.needUnlock ? '积分解锁' : '免费阅读' }}</strong>
                        <p v-if="!hasPaidArticlePrivilege" class="editor-unlock-tip">
                            Lv.4 解锁付费文章发布权限
                        </p>
                    </div>
                    <label class="editor-unlock-switch">
                        <input
                            v-model="draft.needUnlock"
                            type="checkbox"
                            data-testid="editor-need-unlock-toggle"
                            :disabled="unlockSwitchDisabled"
                            @change="onNeedUnlockToggle"
                        >
                        <span>{{ draft.needUnlock ? '已开启' : '关闭' }}</span>
                    </label>
                </div>
                <label class="editor-unlock-price" :class="{ disabled: !draft.needUnlock }">
                    <span>解锁积分</span>
                    <input
                        v-model.number="draft.unlockPointPrice"
                        type="number"
                        :min="MIN_UNLOCK_POINT_PRICE"
                        :max="MAX_UNLOCK_POINT_PRICE"
                        step="1"
                        :disabled="!draft.needUnlock || !hasPaidArticlePrivilege"
                        data-testid="editor-unlock-price-input"
                        @change="onUnlockPointPriceCommit"
                        @blur="onUnlockPointPriceCommit"
                    >
                </label>
            </section>
            <ArticleToc
                :content="draft.content"
                :target-selector="previewVisible ? '.editor-preview-panel .markdown-preview' : '.rich-markdown-editor .ProseMirror'"
                :use-custom-navigation="!previewVisible"
                :refresh-on-content-change="!previewVisible"
                collapsible
                @navigate="handleTocNavigate"
            />
            <section class="editor-category-section">
                <div class="editor-category-head">
                    <div>
                        <p class="eyebrow">分类</p>
                        <strong class="editor-category-title">选择文章分类</strong>
                    </div>
                    <span :class="['editor-category-value', { empty: !draft.category }]">
                        {{ draft.category || '未选择' }}
                    </span>
                </div>
                <label class="editor-category-search">
                    <span class="sr-only">搜索分类</span>
                    <input
                        v-model.trim="categorySearchKeyword"
                        type="search"
                        maxlength="50"
                        placeholder="搜索分类，例如 MySQL、Redis、AI"
                    >
                </label>
                <div class="editor-category-groups">
                    <section
                        v-for="group in groupedCategoryOptions"
                        :key="group.name"
                        class="editor-category-group"
                    >
                        <button
                            type="button"
                            class="editor-category-group-toggle"
                            :aria-expanded="isCategoryGroupOpen(group.name)"
                            @click="toggleCategoryGroup(group.name)"
                        >
                            <span>{{ group.name }}</span>
                            <em>{{ group.items.length }}</em>
                            <i aria-hidden="true"></i>
                        </button>
                        <div v-show="isCategoryGroupOpen(group.name)" class="editor-category-pills">
                            <button
                                v-for="topic in group.items"
                                :key="topic.name"
                                type="button"
                                :class="['editor-category-pill', { active: draft.category === topic.name }]"
                                @click="selectPresetCategory(topic)"
                            >
                                {{ topic.name }}
                            </button>
                        </div>
                    </section>
                    <p v-if="!groupedCategoryOptions.length" class="editor-category-empty">
                        没有匹配的启用分类，请调整搜索词或在后台维护分类。
                    </p>
                </div>
                <p v-if="unavailableCategory" class="editor-category-hint">
                    当前分类已不可用，请重新选择一个启用分类。
                </p>
                <p v-else-if="!draft.category" class="editor-category-hint">请选择一个启用分类</p>
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

<style scoped src="@/styles/views/EditorView.part-1.css"></style>
<style scoped src="@/styles/views/EditorView.part-2.css"></style>
<style scoped src="@/styles/views/EditorView.part-3.css"></style>
<style scoped src="@/styles/views/EditorView.part-4.css"></style>
