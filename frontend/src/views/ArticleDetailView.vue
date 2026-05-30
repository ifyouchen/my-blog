<script setup>
import {computed, inject, nextTick, onMounted, onUnmounted, ref, watch} from 'vue';
import {RouterLink, useRoute} from 'vue-router';
import {useHead} from '@unhead/vue';
import {getArticleApi, getArticleNeighborsApi, getArticleRecommendationsApi} from '@/api/articles';
import {getColumnArticlesApi, getColumnArticleNeighborsApi} from '@/api/columns';
import {getTopicArticlesApi, getTopicArticleNeighborsApi} from '@/api/topic';
import {likeArticleApi, unlikeArticleApi} from '@/api/likes';
import {favoriteArticleApi, unfavoriteArticleApi} from '@/api/favorites';
import SiteHeader from '@/components/SiteHeader.vue';
import ArticleToc from '@/components/ArticleToc.vue';
import CommentList from '@/components/CommentList.vue';
import CommentComposer from '@/components/CommentComposer.vue';
import MarkdownPreview from '@/components/MarkdownPreview.vue';
import AuthorFollowButton from '@/components/AuthorFollowButton.vue';
import ReportDialog from '@/components/ReportDialog.vue';
import ConfirmDialog from '@/components/ConfirmDialog.vue';
import AdBanner from '@/components/AdBanner.vue';
import UserEquippedBadge from '@/components/UserEquippedBadge.vue';
import UserLevelBadge from '@/components/UserLevelBadge.vue';
import {useSession} from '@/stores/session';
import ArticleLockBadge from '@/components/ArticleLockBadge.vue';
import ArticleUnlockModal from '@/components/ArticleUnlockModal.vue';
import { useGrowthStore } from '@/stores/growth';
import {createCommentApi} from '@/api/comments';
import {useConfirmDialog} from '@/composables/useConfirmDialog';
import {findWarnSensitiveWords, formatWarnSensitiveWords} from '@/utils/sensitiveWords';

const route = useRoute();
const loginModal = inject('loginModal', { requireLogin: () => false });
const toast = inject('toast', { error: () => {}, success: () => {} });
const { state } = useSession();
const {
    confirmDialog,
    openConfirmDialog,
    closeConfirmDialog,
    executeConfirmDialog
} = useConfirmDialog();

// ===== 成长模块 - 文章解锁 =====
const growthStore = useGrowthStore();
const unlockModalVisible = ref(false);
const unlockStatus = ref(null);
const unlocking = ref(false);

const buildLockedStatusFromArticle = (articleData, id) => {
    const shouldTreatAsLocked = Boolean(articleData?.contentLocked)
        || (!state.user && Boolean(articleData?.needUnlock));
    if (!articleData || !shouldTreatAsLocked) {
        return null;
    }
    return {
        articleId: id || articleData.id,
        needUnlock: Boolean(articleData.needUnlock || articleData.contentLocked),
        unlockPointPrice: Number(articleData.unlockPointPrice || 0),
        unlocked: false,
        currentBalance: undefined,
        reason: articleData.unlockReason || 'CONTENT_LOCKED'
    };
};

const syncUnlockStatusFromArticle = (articleData, id) => {
    unlockStatus.value = buildLockedStatusFromArticle(articleData, id);
};

const loadUnlockStatus = async (id) => {
    if (!state.user) {
        syncUnlockStatusFromArticle(article.value, id);
        return;
    }
    try {
        unlockStatus.value = await growthStore.fetchUnlockStatus(id);
    } catch {
        syncUnlockStatusFromArticle(article.value, id);
    }
};

const effectiveUnlockStatus = computed(() =>
    unlockStatus.value || buildLockedStatusFromArticle(article.value, articleId.value)
);

const canReadArticleContent = computed(() => {
    const status = effectiveUnlockStatus.value;
    return Boolean(articleMarkdown.value)
        && (!status || !status.needUnlock || status.unlocked);
});

const openUnlockModal = () => {
    if (!state.user) {
        loginModal.requireLogin();
        return;
    }
    unlockModalVisible.value = true;
};

const handleUnlockConfirm = async () => {
    unlocking.value = true;
    const targetArticleId = articleId.value;
    try {
        const result = await growthStore.unlockArticle(articleId.value);
        growthStore.invalidateUnlockCache(targetArticleId);
        const detail = await getArticleApi(targetArticleId);
        if (articleId.value === targetArticleId) {
            remoteArticle.value = detail;
            const status = await growthStore.fetchUnlockStatus(targetArticleId);
            unlockStatus.value = {
                ...status,
                unlocked: true,
                currentBalance: result?.balanceAfter ?? status?.currentBalance
            };
        }
        unlockModalVisible.value = false;
        toast.success('解锁成功！');
    } catch (err) {
        toast.error(err?.message || '解锁失败，请重试');
    } finally {
        unlocking.value = false;
    }
};

const remoteArticle = ref(null);
const isLoading = ref(false);
const loadError = ref('');
const recommendationSections = ref([]);
const relatedLoading = ref(false);
const neighborPrev = ref(null);
const neighborNext = ref(null);

// ===== 来源上下文（从专栏/专题进入时携带的 query 参数）=====
const sourceFrom = computed(() => route.query.from || '');
const sourceColumnId = computed(() => route.query.columnId ? String(route.query.columnId) : '');
const sourceColumnTitle = computed(() => route.query.columnTitle ? String(route.query.columnTitle) : '');
const sourceTopicId = computed(() => route.query.topicId ? String(route.query.topicId) : '');
const sourceTopicTitle = computed(() => route.query.topicTitle ? String(route.query.topicTitle) : '');
const isFromColumn = computed(() => sourceFrom.value === 'column' && !!sourceColumnId.value);
const isFromTopic = computed(() => sourceFrom.value === 'topic' && !!sourceTopicId.value);
const isInContext = computed(() => isFromColumn.value || isFromTopic.value);
const isPublishedArticleForSidebar = computed(() => {
    const routeArticleId = String(route.params.id || '').replace(/-.+$/, '');
    return String(remoteArticle.value?.id || '') === routeArticleId
        && remoteArticle.value?.status === 'PUBLISHED';
});
const showRelatedSidebar = computed(() => (
    isPublishedArticleForSidebar.value
        && !isInContext.value
        && (relatedLoading.value || recommendationSections.value.length > 0)
));

// 面包屑信息
const contextLabel = computed(() => {
    if (isFromColumn.value) return sourceColumnTitle.value || '专栏';
    if (isFromTopic.value) return sourceTopicTitle.value || '专题';
    return '';
});
const contextBackTo = computed(() => {
    if (isFromColumn.value) return `/columns/${sourceColumnId.value}`;
    if (isFromTopic.value) return `/topics/${sourceTopicId.value}`;
    return '';
});
const contextType = computed(() => {
    if (isFromColumn.value) return '专栏';
    if (isFromTopic.value) return '专题';
    return '';
});

// 专栏/专题内文章列表（侧边栏用）
const contextArticles = ref([]);
const contextArticlesLoading = ref(false);
const contextArticlesExpanded = ref(false); // 是否已展开全部文章

// 上下篇跳转时携带的 query 参数
const neighborQuery = computed(() => {
    if (isFromColumn.value) {
        return { from: 'column', columnId: sourceColumnId.value, columnTitle: sourceColumnTitle.value };
    }
    if (isFromTopic.value) {
        return { from: 'topic', topicId: sourceTopicId.value, topicTitle: sourceTopicTitle.value };
    }
    return null;
});

const buildNeighborTo = (neighborArticle) => {
    if (!neighborArticle) return null;
    const path = `/articles/${neighborArticle.id}`;
    return neighborQuery.value ? { path, query: neighborQuery.value } : path;
};
const articleId = computed(() => String(route.params.id || '').replace(/-.+$/, ''));
const articleUrl = computed(() => `${window.location.origin}/articles/${articleId.value}`);
const article = computed(() => remoteArticle.value);
const isCurrentArticleLoaded = computed(() => (
    Boolean(article.value?.id) && String(article.value.id) === articleId.value
));
const isNonPublicArticle = computed(() => (
    isCurrentArticleLoaded.value
        && Boolean(article.value?.status)
        && article.value.status !== 'PUBLISHED'
));
const articleMarkdown = computed(() => {
    if (!article.value) {
        return '';
    }
    if (article.value.rawContent) {
        return article.value.rawContent;
    }
    if (Array.isArray(article.value.content)) {
        return article.value.content.join('\n\n');
    }
    return article.value.content || '';
});
const pageTitle = computed(() => {
    if (article.value?.title) {
        return `${article.value.title} - DevNotes`;
    }
    if (isLoading.value) {
        return '加载中 - DevNotes';
    }
    return '文章详情 - DevNotes';
});

useHead({
    title: pageTitle,
    meta: computed(() => {
        if (!article.value?.seoDescription && !article.value?.summary) {
            return [];
        }
        return [{
            name: 'description',
            content: article.value.seoDescription || article.value.summary
        }];
    })
});

const liked = ref(false);
const favorited = ref(false);
const likeCount = ref(0);
const favoriteCount = ref(0);
const commentCount = ref(0);
const articleContentRef = ref(null);
const commentSectionRef = ref(null);
const commentListRef = ref(null);
const inlineQuoteComposerRef = ref(null);
const pendingCommentQuote = ref(null);
const selectionComment = ref({
    visible: false,
    composing: false,
    quoteText: '',
    quotePrefix: '',
    quoteSuffix: '',
    draft: '',
    feedback: '',
    submitting: false,
    x: 0,
    y: 0
});
const showBackToTop = ref(false);
const readingProgress = ref(0);
const likeSubmitting = ref(false);
const favoriteSubmitting = ref(false);
const reportDialogVisible = ref(false);
const showShareMenu = ref(false);
const shareCopied = ref(false);
const showExportMenu = ref(false);
const exportingFormat = ref('');
const tocDrawerOpen = ref(false);
const mobileTocCloseButtonRef = ref(null);
let shareHoverTimer = null;
let exportHoverTimer = null;
let pendingCommentScrollTimer = null;
const SHARE_HOVER_DELAY = 280;
const RECENT_READING_KEY = 'my-blog:recent-reading';
const RECENT_READING_LIMIT = 20;
const ARTICLE_LAYOUT_PREF_KEY = 'my-blog:article-detail-layout';
const leftSidebarCollapsed = ref(false);
const rightSidebarCollapsed = ref(false);
const sidebarTooltip = ref({
    visible: false,
    text: '',
    x: 0,
    y: 0,
    side: 'left'
});
const hasLeftSidebar = computed(() => isInContext.value || showRelatedSidebar.value);
const detailLayoutClasses = computed(() => [
    'page-shell',
    'detail-layout',
    {
        'detail-layout--context': isInContext.value,
        'detail-layout--related-left': showRelatedSidebar.value,
        'detail-layout--non-public': isNonPublicArticle.value,
        'detail-layout--left-collapsed': hasLeftSidebar.value && leftSidebarCollapsed.value,
        'detail-layout--right-collapsed': rightSidebarCollapsed.value
    }
]);

const readArticleLayoutPreferences = () => {
    try {
        const stored = JSON.parse(localStorage.getItem(ARTICLE_LAYOUT_PREF_KEY) || '{}');
        leftSidebarCollapsed.value = Boolean(stored.leftCollapsed);
        rightSidebarCollapsed.value = Boolean(stored.rightCollapsed);
    } catch {
        localStorage.removeItem(ARTICLE_LAYOUT_PREF_KEY);
    }
};

const saveArticleLayoutPreferences = () => {
    try {
        localStorage.setItem(ARTICLE_LAYOUT_PREF_KEY, JSON.stringify({
            leftCollapsed: leftSidebarCollapsed.value,
            rightCollapsed: rightSidebarCollapsed.value
        }));
    } catch {
        // Ignore storage failures; layout controls should still work for the current session.
    }
};

const showSidebarTooltip = (event, text, side = 'left') => {
    if (!text) {
        sidebarTooltip.value.visible = false;
        return;
    }
    const rect = event.currentTarget.getBoundingClientRect();
    const gap = 12;
    sidebarTooltip.value = {
        visible: true,
        text,
        x: side === 'right' ? rect.left - gap : rect.right + gap,
        y: Math.min(Math.max(rect.top + (rect.height / 2), 56), window.innerHeight - 56),
        side
    };
};

const hideSidebarTooltip = () => {
    sidebarTooltip.value.visible = false;
};

const saveRecentArticle = (source) => {
    if (!source?.id) {
        return;
    }
    try {
        const current = JSON.parse(localStorage.getItem(RECENT_READING_KEY) || '[]');
        const safeCurrent = Array.isArray(current) ? current : [];
        const item = {
            id: source.id,
            title: source.title,
            summary: source.summary,
            cover: source.cover,
            category: source.category,
            publishedText: source.publishedText,
            authorName: source.author?.name || '',
            readAt: Date.now()
        };
        const next = [
            item,
            ...safeCurrent.filter((entry) => String(entry.id) !== String(source.id))
        ].slice(0, RECENT_READING_LIMIT);
        localStorage.setItem(RECENT_READING_KEY, JSON.stringify(next));
    } catch {
        localStorage.removeItem(RECENT_READING_KEY);
    }
};

const openShareMenu = () => {
    if (shareHoverTimer) clearTimeout(shareHoverTimer);
    showExportMenu.value = false;
    shareHoverTimer = window.setTimeout(() => {
        showShareMenu.value = true;
    }, SHARE_HOVER_DELAY);
};

const closeShareMenu = () => {
    if (shareHoverTimer) clearTimeout(shareHoverTimer);
    shareHoverTimer = window.setTimeout(() => {
        showShareMenu.value = false;
    }, SHARE_HOVER_DELAY);
};

const openExportMenu = () => {
    if (exportHoverTimer) clearTimeout(exportHoverTimer);
    showShareMenu.value = false;
    exportHoverTimer = window.setTimeout(() => {
        showExportMenu.value = true;
    }, SHARE_HOVER_DELAY);
};

const closeExportMenu = () => {
    if (exportHoverTimer) clearTimeout(exportHoverTimer);
    exportHoverTimer = window.setTimeout(() => {
        showExportMenu.value = false;
    }, SHARE_HOVER_DELAY);
};

const toggleExportMenu = () => {
    if (exportHoverTimer) clearTimeout(exportHoverTimer);
    showShareMenu.value = false;
    showExportMenu.value = !showExportMenu.value;
};

const EXPORT_FORMAT_LABELS = {
    md: 'Markdown',
    html: 'HTML',
    pdf: 'PDF'
};

const handleExportArticle = async (format) => {
    if (exportingFormat.value) {
        return;
    }
    const markdown = articleMarkdown.value;
    if (!markdown.trim()) {
        toast.error('正文为空，无法导出');
        return;
    }

    exportingFormat.value = format;
    showExportMenu.value = false;

    try {
        let result;
        const {
            exportArticleAsHtml,
            exportArticleAsMarkdown,
            exportArticleAsPdf
        } = await import('@/utils/articleExport');
        const payload = {
            markdown,
            title: article.value?.title || `article-${articleId.value}`
        };
        if (format === 'md') {
            result = await exportArticleAsMarkdown(payload);
        } else if (format === 'html') {
            result = await exportArticleAsHtml(payload);
        } else {
            result = await exportArticleAsPdf(payload);
        }

        const label = EXPORT_FORMAT_LABELS[format] || '文件';
        const failedImageCount = result?.failedImageCount || 0;
        if (format === 'pdf' && result?.printRequested) {
            if (failedImageCount > 0) {
                toast.success(`PDF 打印窗口已打开，${failedImageCount} 张图片未能内联，已保留原链接`);
            } else {
                toast.success('PDF 打印窗口已打开，请选择“另存为 PDF”保存');
            }
        } else if (failedImageCount > 0) {
            const imageAction = format === 'md' ? '打包' : '内联';
            toast.success(`${label} 导出已完成，${failedImageCount} 张图片未能${imageAction}，已保留原链接`);
        } else {
            toast.success(`${label} 导出已完成`);
        }
    } catch (error) {
        toast.error(error.message || '导出失败，请稍后再试');
    } finally {
        exportingFormat.value = '';
    }
};
const currentUserId = computed(() => state.user?.id || null);
const currentUserAvatar = computed(() => state.user?.avatar || state.user?.avatarUrl || '');
const showAuthorFollow = computed(() => {
    if (!article.value?.author?.id) {
        return false;
    }
    return currentUserId.value !== article.value.author.id;
});
const showArticleReport = computed(() => {
    if (!article.value?.author?.id) {
        return false;
    }
    return currentUserId.value !== article.value.author.id;
});

const applyInteractionState = ({
    nextLiked = liked.value,
    nextFavorited = favorited.value,
    nextLikeCount = likeCount.value,
    nextFavoriteCount = favoriteCount.value,
    syncRemote = true
} = {}) => {
    liked.value = nextLiked;
    favorited.value = nextFavorited;
    likeCount.value = Math.max(0, nextLikeCount);
    favoriteCount.value = Math.max(0, nextFavoriteCount);

    if (!syncRemote || !remoteArticle.value) {
        return;
    }
    remoteArticle.value.liked = liked.value;
    remoteArticle.value.favorited = favorited.value;
    remoteArticle.value.likeCount = likeCount.value;
    remoteArticle.value.favoriteCount = favoriteCount.value;
};

const syncArticleState = () => {
    if (!article.value) {
        applyInteractionState({
            nextLiked: false,
            nextFavorited: false,
            nextLikeCount: 0,
            nextFavoriteCount: 0,
            syncRemote: false
        });
        commentCount.value = 0;
        return;
    }
    applyInteractionState({
        nextLiked: Boolean(article.value.liked),
        nextFavorited: Boolean(article.value.favorited),
        nextLikeCount: article.value.likeCount || 0,
        nextFavoriteCount: article.value.favoriteCount || 0,
        syncRemote: false
    });
    commentCount.value = article.value.commentCount || 0;
};

const handleCommentCountChange = (delta) => {
    commentCount.value = Math.max(0, commentCount.value + delta);
    if (remoteArticle.value) {
        remoteArticle.value.commentCount = commentCount.value;
    }
};

const normalizeSelectionText = (value = '') => String(value).replace(/\s+/g, ' ').trim();

const hideSelectionComment = ({ force = false } = {}) => {
    if (!force && selectionComment.value.composing) {
        return;
    }
    selectionComment.value.visible = false;
    selectionComment.value.composing = false;
    selectionComment.value.feedback = '';
};

const getArticleQuoteRoot = () =>
    articleContentRef.value?.querySelector('.markdown-preview') || articleContentRef.value;

const getSelectionContext = (root, range, selectedText) => {
    if (!root || !range) {
        return { quotePrefix: '', quoteSuffix: '' };
    }
    try {
        const prefixRange = document.createRange();
        prefixRange.selectNodeContents(root);
        prefixRange.setEnd(range.startContainer, range.startOffset);

        const suffixRange = document.createRange();
        suffixRange.selectNodeContents(root);
        suffixRange.setStart(range.endContainer, range.endOffset);

        return {
            quotePrefix: normalizeSelectionText(prefixRange.toString()).slice(-80),
            quoteSuffix: normalizeSelectionText(suffixRange.toString()).slice(0, 80)
        };
    } catch {
        const rootText = normalizeSelectionText(root.textContent || '');
        const index = rootText.indexOf(selectedText);
        if (index < 0) {
            return { quotePrefix: '', quoteSuffix: '' };
        }
        return {
            quotePrefix: rootText.slice(Math.max(0, index - 80), index),
            quoteSuffix: rootText.slice(index + selectedText.length, index + selectedText.length + 80)
        };
    }
};

const handleArticleSelection = () => {
    window.setTimeout(() => {
        if (selectionComment.value.composing) {
            return;
        }
        const root = getArticleQuoteRoot();
        const selection = window.getSelection?.();
        if (!root || !selection || selection.rangeCount === 0 || selection.isCollapsed) {
            hideSelectionComment();
            return;
        }
        const anchorNode = selection.anchorNode;
        const focusNode = selection.focusNode;
        if (!root.contains(anchorNode) || !root.contains(focusNode)) {
            hideSelectionComment();
            return;
        }
        const quoteText = normalizeSelectionText(selection.toString()).slice(0, 300);
        if (quoteText.length < 2) {
            hideSelectionComment();
            return;
        }
        const range = selection.getRangeAt(0);
        const rect = range.getBoundingClientRect();
        if (!rect.width && !rect.height) {
            hideSelectionComment();
            return;
        }
        const context = getSelectionContext(root, range, quoteText);
        selectionComment.value = {
            visible: true,
            composing: false,
            quoteText,
            ...context,
            draft: '',
            feedback: '',
            submitting: false,
            x: Math.min(Math.max(rect.left + rect.width / 2, 56), window.innerWidth - 56),
            y: Math.max(rect.top - 46, 80)
        };
    }, 0);
};

const startInlineQuotedComment = async () => {
    if (!selectionComment.value.quoteText) {
        return;
    }
    const canContinue = loginModal.requireLogin(() => startInlineQuotedComment(), {
        title: '登录后发表评论',
        message: '登录后可以对选中的原文直接评论，和其他读者一起交流。',
        actionText: '登录并评论'
    });
    if (!canContinue) {
        return;
    }
    const composerWidth = Math.min(520, window.innerWidth - 32);
    const halfWidth = composerWidth / 2;
    selectionComment.value.composing = true;
    selectionComment.value.visible = true;
    selectionComment.value.feedback = '';
    selectionComment.value.x = Math.min(
        Math.max(selectionComment.value.x, halfWidth + 16),
        window.innerWidth - halfWidth - 16
    );
    window.getSelection?.()?.removeAllRanges();
    await nextTick();
    inlineQuoteComposerRef.value?.focus?.();
};

const cancelInlineQuotedComment = () => {
    hideSelectionComment({ force: true });
    selectionComment.value.draft = '';
};

const confirmSensitiveInlineComment = async (content, onConfirm) => {
    const sensitiveHits = await findWarnSensitiveWords(content);
    if (!sensitiveHits.length) {
        return false;
    }
    const words = formatWarnSensitiveWords(sensitiveHits);
    selectionComment.value.feedback = `存在敏感词 ${words}，请修改或确认继续发表`;
    openConfirmDialog({
        eyebrow: '敏感词提醒',
        title: '评论包含警告词',
        message: `存在敏感词 ${words}，发送后会自动替换为 ***。是否确认发表评论？`,
        confirmText: '确认发表',
        cancelText: '返回修改',
        tone: 'warning',
        onConfirm
    });
    return true;
};

const submitInlineQuotedComment = async (options = {}) => {
    const canContinue = loginModal.requireLogin(() => submitInlineQuotedComment(options), {
        title: '登录后发表评论',
        message: '登录后可以对选中的原文直接评论，和其他读者一起交流。',
        actionText: '登录并评论'
    });
    if (!canContinue) {
        selectionComment.value.feedback = '登录后可以发表评论';
        return;
    }
    const content = selectionComment.value.draft.trim();
    if (!content) {
        selectionComment.value.feedback = '评论内容不能为空';
        return;
    }
    if (options.skipSensitiveWarning !== true) {
        selectionComment.value.submitting = true;
        const waitingConfirm = await confirmSensitiveInlineComment(content, async () => {
            await submitInlineQuotedComment({ skipSensitiveWarning: true });
        });
        selectionComment.value.submitting = false;
        if (waitingConfirm) {
            return;
        }
    }
    selectionComment.value.submitting = true;
    try {
        const createdComment = await createCommentApi(articleId.value, {
            content,
            parentId: 0,
            rootCommentId: 0,
            quoteText: selectionComment.value.quoteText,
            quotePrefix: selectionComment.value.quotePrefix,
            quoteSuffix: selectionComment.value.quoteSuffix
        });
        handleCommentCountChange(1);
        commentListRef.value?.insertExternalRootComment?.(createdComment);
        toast.success('评论已发布');
        cancelInlineQuotedComment();
    } catch (error) {
        selectionComment.value.feedback = error.message || '发表评论失败';
    } finally {
        selectionComment.value.submitting = false;
    }
};

const clearPendingCommentQuote = () => {
    pendingCommentQuote.value = null;
};

const buildQuoteSearchCandidates = (text) => {
    const target = normalizeSelectionText(text).slice(0, 300);
    if (!target) {
        return [];
    }
    const candidates = new Set([target, target.slice(0, 120)]);
    target
        .split(/[\s，。！？、；;：:（）()[\]{}"'“”‘’<>《》]+/)
        .map((item) => item.trim())
        .filter((item) => item.length >= 6)
        .sort((first, second) => second.length - first.length)
        .slice(0, 6)
        .forEach((item) => {
            candidates.add(item.slice(0, 160));
        });
    return Array.from(candidates).filter(Boolean);
};

const buildNormalizedTextIndex = (root) => {
    const positions = [];
    let text = '';
    let lastWasSpace = false;
    const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
    let node = walker.nextNode();
    while (node) {
        const nodeText = node.textContent || '';
        for (let offset = 0; offset < nodeText.length; offset += 1) {
            const char = nodeText[offset];
            if (/\s/.test(char)) {
                if (text && !lastWasSpace) {
                    text += ' ';
                    positions.push({ node, offset });
                    lastWasSpace = true;
                }
            } else {
                text += char;
                positions.push({ node, offset });
                lastWasSpace = false;
            }
        }
        node = walker.nextNode();
    }
    if (lastWasSpace) {
        text = text.slice(0, -1);
        positions.pop();
    }
    return { text, positions };
};

const findCandidateIndexes = (text, candidate) => {
    const indexes = [];
    let searchFrom = 0;
    while (searchFrom < text.length) {
        const index = text.indexOf(candidate, searchFrom);
        if (index < 0) {
            break;
        }
        indexes.push(index);
        searchFrom = index + Math.max(1, candidate.length);
    }
    return indexes;
};

const commonPrefixLength = (first, second) => {
    const limit = Math.min(first.length, second.length);
    let length = 0;
    while (length < limit && first[length] === second[length]) {
        length += 1;
    }
    return length;
};

const commonSuffixLength = (first, second) => {
    const limit = Math.min(first.length, second.length);
    let length = 0;
    while (
        length < limit
        && first[first.length - 1 - length] === second[second.length - 1 - length]
    ) {
        length += 1;
    }
    return length;
};

const scoreQuoteMatch = (normalizedText, index, candidate, quote) => {
    const quoteText = normalizeSelectionText(quote?.quoteText || '').slice(0, 300);
    const quotePrefix = normalizeSelectionText(quote?.quotePrefix || '').slice(-80);
    const quoteSuffix = normalizeSelectionText(quote?.quoteSuffix || '').slice(0, 80);
    const matchedLength = normalizedText.slice(index, index + quoteText.length) === quoteText
        ? quoteText.length
        : candidate.length;
    let score = candidate.length;

    if (quotePrefix) {
        const before = normalizedText.slice(Math.max(0, index - quotePrefix.length), index);
        score += commonSuffixLength(before, quotePrefix) * 4;
        if (before === quotePrefix) {
            score += 400;
        }
    }

    if (quoteSuffix) {
        const after = normalizedText.slice(index + matchedLength, index + matchedLength + quoteSuffix.length);
        score += commonPrefixLength(after, quoteSuffix) * 4;
        if (after === quoteSuffix) {
            score += 400;
        }
    }

    if (candidate === quoteText) {
        score += 80;
    }

    return { score, matchedLength };
};

const findTextNodeContainingQuote = (root, quote) => {
    const candidates = buildQuoteSearchCandidates(quote?.quoteText || '');
    if (!root || !candidates.length) {
        return null;
    }

    const normalizedIndex = buildNormalizedTextIndex(root);
    if (!normalizedIndex.text) {
        return null;
    }

    let bestMatch = null;
    for (const candidate of candidates) {
        const indexes = findCandidateIndexes(normalizedIndex.text, candidate);
        for (const index of indexes) {
            const scored = scoreQuoteMatch(normalizedIndex.text, index, candidate, quote);
            if (
                !bestMatch
                || scored.score > bestMatch.score
                || (scored.score === bestMatch.score && index < bestMatch.index)
            ) {
                bestMatch = {
                    index,
                    text: candidate,
                    score: scored.score,
                    matchedLength: scored.matchedLength
                };
            }
        }
    }
    if (!bestMatch) {
        return null;
    }

    const position = normalizedIndex.positions[bestMatch.index];
    return position ? { node: position.node, index: position.offset, text: bestMatch.text } : null;
};

const findQuoteBlockElement = (node) => {
    const root = articleContentRef.value;
    let element = node?.parentElement || null;
    const blockSelector = 'p, li, blockquote, pre, figure, h1, h2, h3, h4, h5, h6, table, hr';
    while (element && element !== root) {
        if (element.matches?.(blockSelector)) {
            return element;
        }
        element = element.parentElement;
    }
    return node?.parentElement || null;
};

const flashQuoteElement = (element) => {
    if (!element) {
        return;
    }
    const rect = element.getBoundingClientRect();
    const fixedHeaderOffset = 92;
    const contextOffset = Math.min(220, Math.round(window.innerHeight * 0.28));
    const targetTop = Math.max(
        0,
        window.scrollY + rect.top - fixedHeaderOffset - contextOffset
    );
    window.scrollTo({ top: targetTop, behavior: 'smooth' });
    element.classList.add('article-quote-flash');
    window.setTimeout(() => element.classList.remove('article-quote-flash'), 1800);
};

const scrollToQuote = (quote) => {
    const root = getArticleQuoteRoot();
    const found = findTextNodeContainingQuote(root, quote);
    if (!found) {
        commentSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' });
        return;
    }
    const targetElement = findQuoteBlockElement(found.node);
    flashQuoteElement(targetElement);
};

const getCommentJumpTarget = () => {
    const hash = String(route.hash || '');
    const hashCommentMatch = hash.match(/^#comment-(\d+)$/);
    const queryCommentId = route.query.commentId ? String(route.query.commentId) : '';
    const shouldScrollToComments = route.query.scrollTo === 'comments'
        || hash === '#comments'
        || Boolean(hashCommentMatch);
    if (!shouldScrollToComments) {
        return null;
    }
    return {
        commentId: queryCommentId || (hashCommentMatch ? hashCommentMatch[1] : '')
    };
};

const scrollToCommentsFromRoute = async () => {
    const target = getCommentJumpTarget();
    if (!target || !commentSectionRef.value) {
        return;
    }
    commentSectionRef.value.scrollIntoView({ behavior: 'smooth', block: 'start' });
    if (!target.commentId) {
        return;
    }
    for (let attempt = 0; attempt < 8; attempt++) {
        const found = await commentListRef.value?.scrollToComment?.(target.commentId);
        if (found) {
            return;
        }
        await new Promise((resolve) => window.setTimeout(resolve, 180));
    }
};

const scheduleScrollToCommentsFromRoute = () => {
    if (pendingCommentScrollTimer) {
        window.clearTimeout(pendingCommentScrollTimer);
    }
    pendingCommentScrollTimer = window.setTimeout(() => {
        pendingCommentScrollTimer = null;
        scrollToCommentsFromRoute();
    }, 120);
};

const handleAuthorFollowChange = (nextFollowed) => {
    if (!remoteArticle.value?.author) {
        return;
    }
    const author = remoteArticle.value.author;
    const wasFollowed = Boolean(author.followed);
    const delta = nextFollowed ? 1 : -1;
    author.followed = nextFollowed;
    if (wasFollowed !== nextFollowed) {
        author.followerCount = Math.max(0, (author.followerCount || 0) + delta);
    }
};

const fetchArticle = async () => {
    const routeId = String(route.params.id);
    const numericId = routeId.replace(/-.+$/, '');
    // 有旧文章时保留 remoteArticle（避免整个 main 卸载导致左侧栏闪烁）
    // 首次加载时才 null（走骨架屏分支）
    isLoading.value = true;
    loadError.value = '';
    recommendationSections.value = [];
    relatedLoading.value = false;
    try {
        const detail = await getArticleApi(numericId);
        if (String(route.params.id) === routeId) {
            remoteArticle.value = detail;
            if (state.user) {
                loadUnlockStatus(numericId);
            } else {
                syncUnlockStatusFromArticle(detail, numericId);
            }
            scheduleScrollToCommentsFromRoute();
        }
    } catch (error) {
        if (String(route.params.id) === routeId) {
            remoteArticle.value = null;
            loadError.value = error.message || '文章不存在';
        }
    } finally {
        if (String(route.params.id) === routeId) {
            isLoading.value = false;
        }
    }
};

const fetchRecommendations = async (id) => {
    // 从专栏/专题进入时，不显示相关推荐（侧边栏改为专栏/专题文章列表）
    if (isInContext.value || String(id) !== articleId.value || !isPublishedArticleForSidebar.value) {
        recommendationSections.value = [];
        relatedLoading.value = false;
        return;
    }
    relatedLoading.value = true;
    try {
        const result = await getArticleRecommendationsApi(id, 12);
        recommendationSections.value = result.sections || [];
    } catch {
        recommendationSections.value = [];
    } finally {
        relatedLoading.value = false;
    }
};

const fetchNeighbors = async (id) => {
    try {
        let nb;
        if (isFromColumn.value) {
            nb = await getColumnArticleNeighborsApi(sourceColumnId.value, id);
        } else if (isFromTopic.value) {
            nb = await getTopicArticleNeighborsApi(sourceTopicId.value, id);
        } else {
            nb = await getArticleNeighborsApi(id);
        }
        neighborPrev.value = nb.prev || null;
        neighborNext.value = nb.next || null;
    } catch {
        neighborPrev.value = null;
        neighborNext.value = null;
    }
};

// 获取专栏/专题内文章列表（侧边栏显示，默认最多50条，展开后加载全量）
const fetchContextArticles = async (pageSize = 50) => {
    if (!isInContext.value) {
        contextArticles.value = [];
        return;
    }
    contextArticlesLoading.value = true;
    try {
        let result;
        if (isFromColumn.value) {
            result = await getColumnArticlesApi(sourceColumnId.value, { page: 1, pageSize });
        } else {
            result = await getTopicArticlesApi(sourceTopicId.value, { page: 1, pageSize });
        }
        contextArticles.value = result.items || [];
    } catch {
        contextArticles.value = [];
    } finally {
        contextArticlesLoading.value = false;
    }
};

// 展开全部文章列表
const expandAllContextArticles = async () => {
    contextArticlesExpanded.value = true;
    await fetchContextArticles(500);
};

const toggleLike = async () => {
    if (likeSubmitting.value || !article.value) {
        return;
    }
    const canContinue = loginModal.requireLogin(() => toggleLike(), {
        title: '登录后点赞',
        message: '登录后可以把喜欢的文章同步到你的账号，并继续刚才的点赞操作。',
        actionText: '登录并点赞'
    });
    if (!canContinue) {
        return;
    }
    const previousLiked = liked.value;
    const previousLikeCount = likeCount.value;
    applyInteractionState({
        nextLiked: !previousLiked,
        nextLikeCount: previousLikeCount + (previousLiked ? -1 : 1)
    });
    likeSubmitting.value = true;
    try {
        if (previousLiked) {
            await unlikeArticleApi(article.value.id);
        } else {
            await likeArticleApi(article.value.id);
        }
    } catch (error) {
        applyInteractionState({
            nextLiked: previousLiked,
            nextLikeCount: previousLikeCount
        });
        toast.error(error.message || (previousLiked ? '取消点赞失败，请稍后再试' : '点赞失败，请稍后再试'));
    } finally {
        likeSubmitting.value = false;
    }
};

const toggleFavorite = async () => {
    if (favoriteSubmitting.value || !article.value) {
        return;
    }
    const canContinue = loginModal.requireLogin(() => toggleFavorite(), {
        title: '登录后收藏',
        message: '登录后可以把文章加入收藏夹，之后在个人中心继续阅读。',
        actionText: '登录并收藏'
    });
    if (!canContinue) {
        return;
    }
    const previousFavorited = favorited.value;
    const previousFavoriteCount = favoriteCount.value;
    applyInteractionState({
        nextFavorited: !previousFavorited,
        nextFavoriteCount: previousFavoriteCount + (previousFavorited ? -1 : 1)
    });
    favoriteSubmitting.value = true;
    try {
        if (previousFavorited) {
            await unfavoriteArticleApi(article.value.id);
        } else {
            await favoriteArticleApi(article.value.id);
        }
    } catch (error) {
        applyInteractionState({
            nextFavorited: previousFavorited,
            nextFavoriteCount: previousFavoriteCount
        });
        toast.error(error.message || (previousFavorited ? '取消收藏失败，请稍后再试' : '收藏失败，请稍后再试'));
    } finally {
        favoriteSubmitting.value = false;
    }
};

const openReportArticle = () => {
    if (!article.value) {
        return;
    }
    const canContinue = loginModal.requireLogin(() => openReportArticle(), {
        title: '登录后举报',
        message: '登录后可以提交内容举报，管理员会在后台处理。',
        actionText: '登录并举报'
    });
    if (!canContinue) {
        return;
    }
    reportDialogVisible.value = true;
};

const handleReportSuccess = () => {};

const handleImmersiveKeydown = (e) => {
    if (e.key === 'Escape' && tocDrawerOpen.value) {
        tocDrawerOpen.value = false;
    }
};

const copyArticleLink = async () => {
    const url = articleUrl.value;
    try {
        if (navigator.clipboard?.writeText) {
            await navigator.clipboard.writeText(url);
        } else {
            const textarea = document.createElement('textarea');
            textarea.value = url;
            document.body.appendChild(textarea);
            textarea.select();
            document.execCommand('copy');
            document.body.removeChild(textarea);
        }
        shareCopied.value = true;
        showShareMenu.value = false;
        window.setTimeout(() => { shareCopied.value = false; }, 2000);
    } catch (_) {
        toast.error('复制失败，请手动复制链接');
    }
};

const shareToWeibo = () => {
    if (!article.value) return;
    const url = encodeURIComponent(articleUrl.value);
    const title = encodeURIComponent(article.value.title || '');
    window.open(`https://service.weibo.com/share/share.php?url=${url}&title=${title}`, '_blank');
    showShareMenu.value = false;
};

const scrollToTop = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
};

const handleScroll = () => {
    const scrollY = window.scrollY;
    showBackToTop.value = scrollY > 500;
    const docHeight = document.documentElement.scrollHeight - window.innerHeight;
    readingProgress.value = docHeight > 0 ? Math.min(100, Math.round((scrollY / docHeight) * 100)) : 0;
    if (selectionComment.value.visible) {
        hideSelectionComment();
    }
};

watch(article, syncArticleState, { immediate: true });
watch(() => route.params.id, fetchArticle, { immediate: true });
watch(() => state.user?.id, () => {
    if (!article.value) {
        unlockStatus.value = null;
        return;
    }
    if (state.user) {
        growthStore.invalidateUnlockCache(articleId.value);
        fetchArticle();
        return;
    }
    syncUnlockStatusFromArticle(article.value, articleId.value);
});
watch(() => remoteArticle.value?.id, (id) => {
    const currentArticle = remoteArticle.value;
    if (id && currentArticle) {
        saveRecentArticle(currentArticle);
    }
});
watch(
    () => [remoteArticle.value?.id, route.query.from, route.query.columnId, route.query.topicId],
    ([id]) => {
        if (id && remoteArticle.value) {
            fetchRecommendations(id);
            fetchNeighbors(id);
        }
    }
);

watch([leftSidebarCollapsed, rightSidebarCollapsed], saveArticleLayoutPreferences);

onMounted(() => {
    readArticleLayoutPreferences();
    window.addEventListener('scroll', handleScroll);
    window.addEventListener('keydown', handleImmersiveKeydown);
});

onUnmounted(() => {
    window.removeEventListener('scroll', handleScroll);
    window.removeEventListener('keydown', handleImmersiveKeydown);
    if (shareHoverTimer) clearTimeout(shareHoverTimer);
    if (exportHoverTimer) clearTimeout(exportHoverTimer);
    if (pendingCommentScrollTimer) clearTimeout(pendingCommentScrollTimer);
    document.body.classList.remove('mobile-toc-open');
});

// 监听来源上下文变化，重新加载专栏/专题文章列表
// 只有专栏/专题 ID 真正变化时才重新 fetch，避免同栏内切换文章时列表闪烁
let _lastContextKey = '';
watch(
    () => [route.query.from, route.query.columnId, route.query.topicId],
    ([from, columnId, topicId]) => {
        const newKey = `${from}|${columnId || ''}|${topicId || ''}`;
        const contextChanged = newKey !== _lastContextKey;
        _lastContextKey = newKey;

        if (contextChanged) {
            // 专栏/专题切换：重置展开状态并重新加载列表
            contextArticlesExpanded.value = false;
            fetchContextArticles(50);
        }
        // 文章 ID 变化时刷新上下篇和推荐（由 route.params.id 的 watch 处理，此处无需重复）
    },
    { immediate: true }
);

watch(() => route.fullPath, () => {
    tocDrawerOpen.value = false;
    pendingCommentQuote.value = null;
    hideSelectionComment({ force: true });
    scheduleScrollToCommentsFromRoute();
});

watch(tocDrawerOpen, (open) => {
    if (open) {
        document.body.classList.add('mobile-toc-open');
        window.setTimeout(() => {
            mobileTocCloseButtonRef.value?.focus();
        }, 0);
        return;
    }
    document.body.classList.remove('mobile-toc-open');
});

</script>

<template>
    <SiteHeader />
    <!-- 专栏/专题面包屑导航 -->
    <div v-if="isInContext && contextLabel" class="article-context-bar">
        <div class="article-context-bar-inner page-shell">
            <RouterLink :to="contextBackTo" class="context-back-link">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor"
                    stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M19 12H5M12 5l-7 7 7 7"/>
                </svg>
                <span class="context-type-badge">{{ contextType }}</span>
                <span class="context-back-title">{{ contextLabel }}</span>
            </RouterLink>
        </div>
    </div>
    <div
        v-if="article && readingProgress > 0"
        class="reading-progress-bar"
        role="progressbar"
        :aria-valuenow="readingProgress"
        aria-valuemin="0"
        aria-valuemax="100"
        :style="{ width: `${readingProgress}%` }"
    ></div>
    <main
        v-if="article"
        :class="detailLayoutClasses"
        data-testid="article-detail-page"
    >
        <!-- 左侧：专栏/专题文章列表或相关推荐 -->
        <aside
            v-if="hasLeftSidebar"
            :class="[
                'detail-left-drawer',
                {
                    'detail-left-drawer--collapsed': leftSidebarCollapsed,
                    'detail-left-drawer--context': isInContext,
                    'detail-left-drawer--related': showRelatedSidebar
                }
            ]"
            :aria-label="isInContext ? '专栏文章列表' : '相关推荐'"
        >
            <button
                type="button"
                class="detail-sidebar-handle detail-sidebar-handle--left"
                :aria-pressed="leftSidebarCollapsed"
                :aria-label="leftSidebarCollapsed ? '展开左侧栏' : '隐藏左侧栏'"
                :title="leftSidebarCollapsed ? '展开左侧栏' : '隐藏左侧栏'"
                @click="leftSidebarCollapsed = !leftSidebarCollapsed"
            >
                <svg
                    v-if="leftSidebarCollapsed"
                    class="detail-sidebar-icon"
                    viewBox="0 0 24 24"
                    aria-hidden="true"
                >
                    <path d="M5 7h14M5 12h14M5 17h14"></path>
                </svg>
                <svg v-else class="detail-sidebar-icon" viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M15 6l-6 6 6 6"></path>
                    <path d="M20 6l-6 6 6 6"></path>
                </svg>
            </button>

            <div
                :class="[
                    'detail-sidebar-panel',
                    isInContext ? 'detail-context-side' : 'detail-related-side'
                ]"
                :aria-hidden="leftSidebarCollapsed"
                :inert="leftSidebarCollapsed ? '' : null"
            >
                <template v-if="isInContext">
                    <div class="context-side-inner">
                        <div class="context-side-header">
                            <RouterLink :to="contextBackTo" class="context-side-title-link">
                                <span class="context-side-type">{{ contextType }}</span>
                                <span class="context-side-name">{{ contextLabel }}</span>
                            </RouterLink>
                        </div>
                        <div v-if="contextArticlesLoading && contextArticles.length === 0" class="context-side-loading">
                            加载中...
                        </div>
                        <nav v-else class="context-side-list" :aria-label="`${contextType}文章列表`">
                            <RouterLink
                                v-for="(item, index) in contextArticles"
                                :key="item.id"
                                :to="{ path: `/articles/${item.id}`, query: neighborQuery }"
                                :class="[
                                    'context-side-item',
                                    { 'context-side-item--active': String(item.id) === String(articleId) }
                                ]"
                                @mouseenter="showSidebarTooltip($event, item.title, 'left')"
                                @mouseleave="hideSidebarTooltip"
                                @focus="showSidebarTooltip($event, item.title, 'left')"
                                @blur="hideSidebarTooltip"
                            >
                                <span class="context-side-index">{{ index + 1 }}</span>
                                <span class="context-side-item-title">{{ item.title }}</span>
                            </RouterLink>
                        </nav>
                        <button
                            v-if="!contextArticlesExpanded && contextArticles.length === 50"
                            type="button"
                            class="context-side-more"
                            :disabled="contextArticlesLoading"
                            @click="expandAllContextArticles"
                        >
                            {{ contextArticlesLoading ? '加载中...' : '查看全部文章 ↓' }}
                        </button>
                    </div>
                </template>

                <section v-else class="side-related" data-testid="article-related-section">
                    <h2 class="side-related-title">相关推荐</h2>
                    <div
                        v-if="relatedLoading && recommendationSections.length === 0"
                        class="side-related-loading"
                    >
                        加载中...
                    </div>
                    <div v-else class="side-related-sections">
                        <section
                            v-for="section in recommendationSections"
                            :key="section.key"
                            class="side-related-group"
                        >
                            <h3>{{ section.title }}</h3>
                            <div class="side-related-list">
                                <RouterLink
                                    v-for="rel in section.items.slice(0, 5)"
                                    :key="rel.id"
                                    class="side-related-item article-related-item"
                                    :to="`/articles/${rel.id}`"
                                    @mouseenter="showSidebarTooltip($event, rel.title, 'left')"
                                    @mouseleave="hideSidebarTooltip"
                                    @focus="showSidebarTooltip($event, rel.title, 'left')"
                                    @blur="hideSidebarTooltip"
                                >
                                    <img
                                        v-if="rel.cover"
                                        class="side-related-thumb"
                                        :src="rel.cover"
                                        :alt="rel.title"
                                        loading="lazy"
                                        decoding="async"
                                    >
                                    <div class="side-related-info">
                                        <p class="side-related-item-title">{{ rel.title }}</p>
                                        <div class="side-related-stats">
                                            <span v-if="rel.category">{{ rel.category }}</span>
                                            <span>{{ rel.viewCount }} 阅读</span>
                                        </div>
                                    </div>
                                </RouterLink>
                            </div>
                        </section>
                    </div>
                </section>
            </div>
        </aside>

        <article :class="['article-main', { 'article-main--loading': isLoading && remoteArticle }]" data-testid="article-detail-main">
            <div class="article-body">
                <section class="article-heading-panel">
                    <div class="article-heading-top">
                        <div class="post-meta article-meta-row">
                            <span v-if="article.category">{{ article.category }}</span>
                            <span v-if="article.readingTime">{{ article.readingTime }}</span>
                        </div>
                        <div class="article-tag-list">
                            <span v-for="tag in article.tags" :key="tag" class="article-tag-chip">
                                {{ tag }}
                            </span>
                        </div>
                    </div>
                    <h1>{{ article.title }}</h1>
                    <ArticleLockBadge
                        v-if="effectiveUnlockStatus && effectiveUnlockStatus.needUnlock"
                        :need-unlock="true"
                        :unlocked="effectiveUnlockStatus.unlocked"
                        :point-price="effectiveUnlockStatus.unlockPointPrice"
                        class="article-lock-badge"
                    />
                    <p class="article-summary">{{ article.summary }}</p>

                    <div class="article-heading-bottom">
                        <div class="article-heading-meta">
                            <RouterLink class="article-byline" :to="`/users/${article.author.id}`">
                                <img
                                    v-if="article.author.avatar"
                                    class="byline-avatar"
                                    :src="article.author.avatar"
                                    :alt="article.author.name"
                                    loading="lazy"
                                    decoding="async"
                                >
                                <span class="byline-avatar-fallback" v-else>{{ article.author.name[0] }}</span>
                                <div class="byline-info">
                                    <div class="byline-name-row">
                                        <strong class="byline-name">{{ article.author.name }}</strong>
                                        <UserLevelBadge :level="article.author.currentLevel" compact />
                                        <UserEquippedBadge :badge="article.author.equippedBadge" compact />
                                    </div>
                                    <span class="byline-meta">
                                        <span class="byline-date">{{ article.publishedText }}</span>
                                        <span class="byline-separator">·</span>
                                        <span class="byline-followers">{{ article.author.followerCount || 0 }} 粉丝</span>
                                    </span>
                                </div>
                            </RouterLink>
                            <div class="article-stats-row">
                                <span class="article-stat"><strong>{{ article.viewCount }}</strong> 阅读</span>
                                <span class="article-stat"><strong>{{ likeCount }}</strong> 点赞</span>
                                <span class="article-stat"><strong>{{ commentCount }}</strong> 评论</span>
                                <span class="article-stat"><strong>{{ favoriteCount }}</strong> 收藏</span>
                            </div>
                        </div>

                        <div class="article-quick-actions">
                            <button
                                type="button"
                                :class="['article-quick-button', { active: liked }]"
                                :disabled="likeSubmitting"
                                @click="toggleLike"
                            >
                                <span class="article-quick-label">{{ liked ? '已点赞' : '点赞' }}</span>
                            </button>
                            <button
                                type="button"
                                :class="['article-quick-button', { active: favorited }]"
                                :disabled="favoriteSubmitting"
                                @click="toggleFavorite"
                            >
                                <span class="article-quick-label">{{ favorited ? '已收藏' : '收藏' }}</span>
                            </button>
                            <AuthorFollowButton
                                v-if="showAuthorFollow"
                                class="article-follow-button"
                                :user-id="article.author.id"
                                :followed="article.author.followed"
                                detail
                                @change="handleAuthorFollowChange"
                            />
                            <button
                                v-if="showArticleReport"
                                type="button"
                                class="article-quick-button subtle"
                                @click="openReportArticle"
                            >
                                <span class="article-quick-label">举报</span>
                            </button>
                            <div
                                class="article-export-wrap"
                                @mouseenter="openExportMenu"
                                @mouseleave="closeExportMenu"
                            >
                                <button
                                    type="button"
                                    class="article-quick-button"
                                    data-testid="article-export-button"
                                    :disabled="Boolean(exportingFormat) || !articleMarkdown"
                                    :aria-expanded="showExportMenu ? 'true' : 'false'"
                                    title="导出正文"
                                    @click="toggleExportMenu"
                                >
                                    <span class="article-quick-label">
                                        {{ exportingFormat ? '导出中' : '导出' }}
                                    </span>
                                </button>
                                <div
                                    v-if="showExportMenu"
                                    class="article-share-card article-export-card"
                                    data-testid="article-export-menu"
                                >
                                    <div class="share-card-arrow"></div>
                                    <div class="share-card-body">
                                        <button
                                            type="button"
                                            class="share-option export-option"
                                            data-testid="article-export-md"
                                            :disabled="Boolean(exportingFormat)"
                                            @click="handleExportArticle('md')"
                                        >
                                            <svg
                                                class="share-icon"
                                                viewBox="0 0 24 24"
                                                width="20"
                                                height="20"
                                                fill="none"
                                                stroke="currentColor"
                                                stroke-width="2"
                                                stroke-linecap="round"
                                                stroke-linejoin="round"
                                            >
                                                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                                                <path d="M14 2v6h6"/>
                                                <path d="M8 13h8M8 17h5"/>
                                            </svg>
                                            <span>Markdown</span>
                                        </button>
                                        <button
                                            type="button"
                                            class="share-option export-option"
                                            data-testid="article-export-html"
                                            :disabled="Boolean(exportingFormat)"
                                            @click="handleExportArticle('html')"
                                        >
                                            <svg
                                                class="share-icon"
                                                viewBox="0 0 24 24"
                                                width="20"
                                                height="20"
                                                fill="none"
                                                stroke="currentColor"
                                                stroke-width="2"
                                                stroke-linecap="round"
                                                stroke-linejoin="round"
                                            >
                                                <path d="M16 18l6-6-6-6"/>
                                                <path d="M8 6l-6 6 6 6"/>
                                            </svg>
                                            <span>HTML</span>
                                        </button>
                                        <button
                                            type="button"
                                            class="share-option export-option"
                                            data-testid="article-export-pdf"
                                            :disabled="Boolean(exportingFormat)"
                                            @click="handleExportArticle('pdf')"
                                        >
                                            <svg
                                                class="share-icon"
                                                viewBox="0 0 24 24"
                                                width="20"
                                                height="20"
                                                fill="none"
                                                stroke="currentColor"
                                                stroke-width="2"
                                                stroke-linecap="round"
                                                stroke-linejoin="round"
                                            >
                                                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                                                <path d="M14 2v6h6"/>
                                                <path d="M9 15h6"/>
                                            </svg>
                                            <span>PDF</span>
                                        </button>
                                    </div>
                                </div>
                            </div>
                            <!-- 分享 -->
                            <div
                                class="article-share-wrap"
                                @mouseenter="openShareMenu"
                                @mouseleave="closeShareMenu"
                            >
                                <button
                                    type="button"
                                    :class="['article-quick-button', { active: shareCopied }]"
                                    :title="shareCopied ? '已复制链接' : '分享文章'"
                                >
                                    <span class="article-quick-label">{{ shareCopied ? '已复制' : '分享' }}</span>
                                </button>
                                <div v-if="showShareMenu" class="article-share-card">
                                    <div class="share-card-arrow"></div>
                                    <div class="share-card-body">
                                        <div class="share-option" @click="copyArticleLink">
                                            <svg class="share-icon" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg>
                                            <span>复制链接</span>
                                        </div>
                                        <div class="share-option" @click="shareToWeibo">
                                            <svg class="share-icon" viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M10.98 13.9c-.77-.3-1.58-.28-2.3-.06.48.28.98.53 1.5.74.84.34 1.7.62 2.58.84.2-.5.26-1.03-.06-1.48-.26-.33-.68-.37-1.08-.48l-.64-.56zm1.06-2.3c.4.22.88.26 1.3.1.42-.17.72-.56.8-1 .04-.23-.02-.46-.17-.64-.35-.42-1-.58-1.5-.52-.44.06-.84.36-1.06.74-.22.38-.24.84-.04 1.24.02.03.04.06.06.08z"/><path d="M21.86 4.84c-.54-1.62-2.3-2.9-4.48-3.23-2.9-.44-6.1.7-8.58 2.97-.98.9-1.8 1.92-2.4 3.01L3.2 8.82c-.65.28-1.2.7-1.64 1.22-.8.93-1 2.14-.42 3.08.27.44.65.77 1.1.98l-.78.66c-.44.38-.72.9-.78 1.48-.12 1.06.52 2.08 1.56 2.6.25.12.52.2.8.26 1.18.28 2.44.54 3.6.86.1.64.54 1.2 1.18 1.64 1.66 1.14 4.08 1.4 6.4.54 1.42-.52 2.7-1.4 3.78-2.55 1.86-2 3.2-4.6 3.56-7.04.18-1.18.08-2.3-.22-3.3-.1-.32-.2-.64-.32-.94zM7.3 16.68c-1.26-.38-3.68-.82-4.66-1.7-.36-.32-.58-.74-.58-1.18 0-.74.56-1.38 1.32-1.5.52-.08 1.04.08 1.48.34.82.48 1.5 1.14 2.2 1.76.92.82 1.93 1.56 3.08 2.06-1.16-.02-2.26-.4-2.84-.78zm4.7 2.2c-2.1.16-4.06-.82-5.06-2.22-1-1.4-.74-3.08.9-3.88.86-.42 1.86-.4 2.74-.02 1.26.54 2.14 1.64 2.62 2.88.3.78.36 1.62.1 2.48-.08.28-.24.54-.44.76h-.86zm6.22-5.32c-1.1 1.48-2.94 2.36-4.44 2.6-.36.06-.72.08-1.08.06.32-.3.6-.64.8-1.02.4-.7.56-1.5.4-2.28-.26-1.28-1.14-2.42-2.3-3.2-1.12-.76-2.48-1.06-3.86-.82-.7.12-1.38.4-1.96.8-.28.2-.54.42-.78.66.46-1.22 1.18-2.3 2.1-3.16 2.06-1.88 4.76-2.8 7.18-2.46 1.72.24 3.16 1.14 3.64 2.32.02.04.04.08.06.12.5 1.38.5 3.12-.18 4.86-.14.36-.3.72-.48 1.06z"/></svg>
                                            <span>微博</span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </section>

                <!-- 已解锁 or 无需解锁：正常显示正文 -->
                <div
                    v-if="canReadArticleContent"
                    class="article-unlocked-content"
                    ref="articleContentRef"
                    @mouseup="handleArticleSelection"
                    @keyup="handleArticleSelection"
                    @touchend="handleArticleSelection"
                >
                    <!-- 作者本人查看文章 -->
                    <div
                        v-if="effectiveUnlockStatus && effectiveUnlockStatus.reason === 'AUTHOR_SELF'"
                        class="article-author-self-notice"
                    >
                        作者本人可直接阅读
                    </div>
                    <!-- 管理员免积分查看 -->
                    <div
                        v-if="effectiveUnlockStatus && effectiveUnlockStatus.reason === 'ADMIN_BYPASS'"
                        class="article-admin-bypass-notice"
                    >
                        管理员可直接查看
                    </div>
                    <MarkdownPreview :content="articleMarkdown" />
                    <button
                        v-if="selectionComment.visible"
                        v-show="!selectionComment.composing"
                        type="button"
                        class="article-selection-comment"
                        :style="{ left: `${selectionComment.x}px`, top: `${selectionComment.y}px` }"
                        @mousedown.prevent
                        @click="startInlineQuotedComment"
                    >
                        评论
                    </button>
                    <div
                        v-if="selectionComment.visible && selectionComment.composing"
                        class="article-selection-composer"
                        :style="{ left: `${selectionComment.x}px`, top: `${selectionComment.y}px` }"
                        @mousedown.stop
                        @click.stop
                    >
                        <div class="article-selection-quote">
                            <strong>引用原文</strong>
                            <span>{{ selectionComment.quoteText }}</span>
                        </div>
                        <CommentComposer
                            ref="inlineQuoteComposerRef"
                            v-model="selectionComment.draft"
                            compact
                            show-cancel
                            :avatar-url="currentUserAvatar"
                            :feedback="selectionComment.feedback"
                            :submitting="selectionComment.submitting"
                            placeholder="写下你对这段原文的想法..."
                            submit-text="发表"
                            @cancel="cancelInlineQuotedComment"
                            @submit="submitInlineQuotedComment"
                        />
                    </div>
                </div>
                <!-- 需要解锁且未解锁：显示遮罩 -->
                <div
                    v-else-if="effectiveUnlockStatus && effectiveUnlockStatus.needUnlock && !effectiveUnlockStatus.unlocked"
                    class="article-lock-wall"
                >
                    <div class="article-lock-wall-preview" aria-hidden="true">
                        <!-- 显示一小段摘要作为预览 -->
                        <MarkdownPreview v-if="article && article.summary" :content="article.summary" />
                    </div>
                    <div class="article-lock-wall-overlay">
                        <div class="article-lock-wall-card">
                            <svg class="lock-wall-icon" viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                                <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                            </svg>
                            <p class="lock-wall-title">本文为付费内容</p>
                            <p class="lock-wall-desc">
                                解锁需消耗 <strong>{{ effectiveUnlockStatus.unlockPointPrice }}</strong> 积分
                                <span v-if="effectiveUnlockStatus.currentBalance !== undefined">
                                    （当前余额：{{ effectiveUnlockStatus.currentBalance }} 积分）
                                </span>
                            </p>
                            <button type="button" class="lock-wall-btn" @click="openUnlockModal">
                                立即解锁
                            </button>
                        </div>
                    </div>
                </div>
                <section v-else class="article-content-empty">
                    <p>正文暂时为空，稍后再来看一眼。</p>
                </section>

                <!-- 上下篇导航 -->
                <section v-if="neighborPrev || neighborNext" class="article-neighbors">
                    <div v-if="isInContext" class="article-neighbors-context">
                        <span class="article-neighbors-context-label">{{ contextType }}「{{ contextLabel }}」</span>
                    </div>
                    <div class="article-neighbors-row">
                        <RouterLink
                            v-if="neighborPrev"
                            :to="buildNeighborTo(neighborPrev)"
                            class="article-neighbor article-neighbor--prev"
                        >
                            <span class="article-neighbor-label">上一篇</span>
                            <span class="article-neighbor-title">{{ neighborPrev.title }}</span>
                        </RouterLink>
                        <span v-else class="article-neighbor article-neighbor--prev article-neighbor--empty" />
                        <RouterLink
                            v-if="neighborNext"
                            :to="buildNeighborTo(neighborNext)"
                            class="article-neighbor article-neighbor--next"
                        >
                            <span class="article-neighbor-label">下一篇</span>
                            <span class="article-neighbor-title">{{ neighborNext.title }}</span>
                        </RouterLink>
                        <span v-else class="article-neighbor article-neighbor--next article-neighbor--empty" />
                    </div>
                </section>

                <section ref="commentSectionRef" class="article-comment" data-testid="article-comments-section">
                    <CommentList
                        v-if="remoteArticle"
                        ref="commentListRef"
                        :article-id="article.id"
                        :initial-count="commentCount"
                        :pending-quote="pendingCommentQuote"
                        @count-change="handleCommentCountChange"
                        @quote-clear="clearPendingCommentQuote"
                        @quote-jump="scrollToQuote"
                    />
                    <div v-else class="comment-placeholder">
                        <p>登录后发表评论</p>
                    </div>
                </section>
            </div>
        </article>

        <aside :class="['detail-side', 'detail-right-drawer', { 'detail-right-drawer--collapsed': rightSidebarCollapsed }]">
            <div class="detail-right-shell">
                <div class="detail-sidebar-control-row detail-sidebar-control-row--right">
                    <button
                        type="button"
                        class="detail-sidebar-handle detail-sidebar-handle--right"
                        :aria-pressed="rightSidebarCollapsed"
                        :aria-label="rightSidebarCollapsed ? '展开目录侧栏' : '隐藏目录侧栏'"
                        :title="rightSidebarCollapsed ? '展开目录侧栏' : '隐藏目录侧栏'"
                        @click="rightSidebarCollapsed = !rightSidebarCollapsed"
                    >
                        <svg
                            v-if="rightSidebarCollapsed"
                            class="detail-sidebar-icon"
                            viewBox="0 0 24 24"
                            aria-hidden="true"
                        >
                            <path d="M5 7h14M5 12h14M5 17h14"></path>
                        </svg>
                        <svg v-else class="detail-sidebar-icon" viewBox="0 0 24 24" aria-hidden="true">
                            <path d="M9 6l6 6-6 6"></path>
                            <path d="M4 6l6 6-6 6"></path>
                        </svg>
                    </button>
                </div>

                <div
                    class="detail-right-panel"
                    :aria-hidden="rightSidebarCollapsed"
                    :inert="rightSidebarCollapsed ? '' : null"
                >
                    <div v-if="isCurrentArticleLoaded" class="detail-toc">
                        <ArticleToc
                            :content="articleMarkdown"
                        />
                    </div>

                    <AdBanner
                        v-if="isPublishedArticleForSidebar && !isInContext"
                        class="article-sidebar-ad"
                        :slot-code="'article_sidebar'"
                    />
                </div>
            </div>
        </aside>
    </main>
    <!-- 加载骨架屏 -->
    <main
        v-else-if="isLoading"
        class="page-shell detail-layout detail-layout--loading"
        aria-busy="true"
        aria-label="文章加载中"
    >
        <div class="article-main">
            <div class="article-skeleton">
                <div class="article-body">
                    <div class="skeleton-panel">
                        <div class="skeleton-meta-row">
                            <div class="skeleton-chip"></div>
                            <div class="skeleton-chip"></div>
                            <div class="skeleton-chip"></div>
                        </div>
                        <div class="skeleton-heading"></div>
                        <div class="skeleton-heading skeleton-heading-short"></div>
                        <div class="skeleton-summary"></div>
                        <div class="skeleton-summary skeleton-summary-short"></div>
                        <div class="skeleton-author-row">
                            <div class="skeleton-avatar-sm"></div>
                            <div class="skeleton-author-meta">
                                <div class="skeleton-name"></div>
                                <div class="skeleton-date"></div>
                            </div>
                        </div>
                    </div>
                    <div class="skeleton-content-block">
                        <div v-for="i in 6" :key="i" class="skeleton-paragraph" :style="{ width: `${70 + (i % 3) * 10}%` }"></div>
                        <div class="skeleton-paragraph skeleton-short"></div>
                        <div v-for="i in 4" :key="`b${i}`" class="skeleton-paragraph" :style="{ width: `${65 + (i % 4) * 8}%` }"></div>
                    </div>
                </div>
            </div>
        </div>
    </main>

    <!-- 错误状态 -->
    <main v-else class="page-shell detail-layout">
        <section class="article-main empty-state">
            <p class="eyebrow">未找到文章</p>
            <h1>这篇文章暂时不可访问</h1>
            <p>{{ loadError || '请稍后刷新，或返回首页继续浏览其他内容。' }}</p>
            <RouterLink to="/">返回首页</RouterLink>
        </section>
    </main>

    <button
        v-show="showBackToTop"
        class="back-to-top"
        type="button"
        @click="scrollToTop"
    >
        ↑
    </button>
    <button
        v-if="remoteArticle"
        class="mobile-toc-trigger"
        type="button"
        @click="tocDrawerOpen = true"
    >
        目录
    </button>
    <div v-if="tocDrawerOpen" class="mobile-toc-overlay" @click.self="tocDrawerOpen = false">
        <section class="mobile-toc-drawer" aria-label="文章目录抽屉">
            <div class="mobile-toc-header">
                <h3>文章目录</h3>
                <button ref="mobileTocCloseButtonRef" type="button" @click="tocDrawerOpen = false">关闭</button>
            </div>
            <ArticleToc
                :content="articleMarkdown"
                mobile-visible
                @navigate="tocDrawerOpen = false"
            />
        </section>
    </div>
    <ReportDialog
        v-if="article"
        :visible="reportDialogVisible"
        target-type="ARTICLE"
        :target-id="article.id"
        :target-title="article.title"
        @close="reportDialogVisible = false"
        @success="handleReportSuccess"
    />
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
    <div
        v-if="sidebarTooltip.visible"
        :class="['sidebar-title-tooltip', `sidebar-title-tooltip--${sidebarTooltip.side}`]"
        :style="{ left: `${sidebarTooltip.x}px`, top: `${sidebarTooltip.y}px` }"
        role="tooltip"
    >
        {{ sidebarTooltip.text }}
    </div>
    <!-- 文章解锁弹窗 -->
    <ArticleUnlockModal
        :visible="unlockModalVisible"
        :article-id="articleId"
        :article-title="article && article.title"
        :point-price="effectiveUnlockStatus && effectiveUnlockStatus.unlockPointPrice"
        :current-balance="effectiveUnlockStatus && effectiveUnlockStatus.currentBalance"
        :unlocking="unlocking"
        @close="unlockModalVisible = false"
        @confirm="handleUnlockConfirm"
    />
</template>

<style scoped>
/* ===== 文章解锁锁墙 ===== */
.article-lock-badge {
    margin: 8px 0 16px;
    display: inline-flex;
}

.article-unlocked-content {
    margin: 24px 0;
}

.article-author-self-notice,
.article-admin-bypass-notice {
    padding: 8px 14px;
    margin-bottom: 16px;
    background: #f0f9ff;
    border: 1px solid #bae6fd;
    border-radius: 8px;
    color: #0369a1;
    font-size: 13px;
    font-weight: 500;
}

.article-admin-bypass-notice {
    background: #f5f3ff;
    border-color: #c4b5fd;
    color: #6d28d9;
}

.article-lock-wall {
    position: relative;
    overflow: hidden;
    border-radius: 12px;
    margin: 24px 0;
    min-height: 300px;
}

.article-lock-wall-preview {
    max-height: 200px;
    overflow: hidden;
    pointer-events: none;
    -webkit-mask-image: linear-gradient(to bottom, rgba(0,0,0,1) 0%, rgba(0,0,0,0) 100%);
    mask-image: linear-gradient(to bottom, rgba(0,0,0,1) 0%, rgba(0,0,0,0) 100%);
}

.article-lock-wall-overlay {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    top: 100px;
    display: flex;
    align-items: flex-end;
    justify-content: center;
    padding-bottom: 32px;
    background: linear-gradient(to bottom, transparent 0%, var(--bg) 40%);
}

.article-lock-wall-card {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
    text-align: center;
    padding: 28px 32px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: 16px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.10);
    max-width: 420px;
    width: 100%;
}

.lock-wall-icon {
    color: var(--accent);
    opacity: 0.8;
}

.lock-wall-title {
    font-size: 18px;
    font-weight: 700;
    color: var(--text-strong);
    margin: 0;
}

.lock-wall-desc {
    font-size: 14px;
    color: var(--text-muted);
    margin: 0;
}

.lock-wall-desc strong {
    color: var(--accent);
    font-size: 16px;
}

.lock-wall-btn {
    margin-top: 4px;
    padding: 10px 32px;
    background: var(--accent);
    color: #fff;
    border: none;
    border-radius: 8px;
    font-size: 15px;
    font-weight: 600;
    cursor: pointer;
    transition: opacity 0.18s, transform 0.18s;
}

.lock-wall-btn:hover {
    opacity: 0.88;
    transform: translateY(-1px);
}

.lock-wall-btn:active {
    opacity: 1;
    transform: translateY(0);
}

.detail-layout {
    max-width: var(--layout-article-max-width);
    grid-template-columns: minmax(0, 1fr) 280px;
    position: relative;
    transition: grid-template-columns 0.36s cubic-bezier(0.22, 1, 0.36, 1);
}

.detail-layout--non-public {
    max-width: var(--layout-article-max-width);
}

.detail-layout--loading {
    max-width: min(1180px, calc(100vw - 48px));
    grid-template-columns: minmax(0, 1fr);
}

/* ===== 三列布局：从专栏/专题进入时，左=文章列表 / 中=正文 / 右=目录 ===== */
.detail-layout--context {
    max-width: var(--layout-article-max-width);
    grid-template-columns: 260px minmax(0, 1fr) 260px;
}

/* ===== 三列布局：普通入口，左=相关推荐 / 中=正文 / 右=目录 ===== */
.detail-layout--related-left {
    max-width: var(--layout-article-max-width);
    grid-template-columns: 280px minmax(0, 1fr) 260px;
}

.detail-layout--left-collapsed:not(.detail-layout--right-collapsed) {
    grid-template-columns: 44px minmax(0, 1fr) 260px;
}

.detail-layout--right-collapsed {
    grid-template-columns: minmax(0, 1fr) 44px;
}

.detail-layout--context.detail-layout--right-collapsed {
    grid-template-columns: 260px minmax(0, 1fr) 44px;
}

.detail-layout--related-left.detail-layout--right-collapsed {
    grid-template-columns: 280px minmax(0, 1fr) 44px;
}

.detail-layout--left-collapsed.detail-layout--right-collapsed {
    grid-template-columns: 44px minmax(0, 1fr) 44px;
}

.detail-layout--left-collapsed .article-main,
.detail-layout--right-collapsed .article-main {
    width: 100%;
    max-width: 1120px;
    justify-self: center;
}

.detail-left-drawer,
.detail-right-drawer {
    position: sticky;
    top: 72px;
    height: calc(100vh - 88px);
    min-width: 0;
    min-height: 56px;
    overflow: hidden;
    transition: opacity 0.24s ease, transform 0.36s cubic-bezier(0.22, 1, 0.36, 1);
}

.detail-left-drawer--collapsed,
.detail-right-drawer--collapsed {
    opacity: 0.96;
}

.detail-sidebar-panel {
    width: 100%;
    height: 100%;
    min-height: 0;
    transform-origin: center;
    opacity: 1;
    transform: translateX(0) scale(1);
    transition: opacity 0.28s ease, transform 0.36s cubic-bezier(0.22, 1, 0.36, 1);
}

.detail-left-drawer--collapsed .detail-sidebar-panel {
    opacity: 0;
    transform: translateX(-12px) scale(0.98);
    pointer-events: none;
}

.detail-right-drawer--collapsed .detail-right-panel {
    opacity: 0;
    transform: translateX(12px) scale(0.98);
    pointer-events: none;
}

.detail-right-panel {
    display: flex;
    flex-direction: column;
    gap: 12px;
    flex: 1 1 auto;
    height: auto;
    min-height: 0;
    opacity: 1;
    overflow: hidden;
    transform: translateX(0) scale(1);
    transition: opacity 0.28s ease, transform 0.36s cubic-bezier(0.22, 1, 0.36, 1);
}

.detail-right-shell {
    position: relative;
    display: flex;
    flex-direction: column;
    height: 100%;
    min-height: 56px;
}

.detail-sidebar-control-row {
    position: relative;
    z-index: 13;
    display: flex;
    flex: 0 0 44px;
    align-items: center;
    min-height: 44px;
    padding: 6px;
}

.detail-sidebar-control-row--right {
    justify-content: flex-end;
}

.detail-sidebar-handle {
    position: absolute;
    top: 10px;
    z-index: 12;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    color: var(--muted);
    cursor: pointer;
    isolation: isolate;
    background: var(--surface);
    border: 1px solid transparent;
    border-radius: 8px;
    box-shadow: none;
    transition: color 0.16s ease, background 0.16s ease, transform 0.24s cubic-bezier(0.22, 1, 0.36, 1);
}

.detail-sidebar-handle:hover,
.detail-sidebar-handle:focus-visible,
.detail-sidebar-handle[aria-pressed="true"] {
    color: var(--text);
    background: rgba(148, 163, 184, 0.12);
    border-color: transparent;
}

.detail-sidebar-handle:focus-visible {
    outline: 2px solid rgba(37, 99, 235, 0.2);
    outline-offset: 2px;
}

.detail-sidebar-handle--left {
    left: 8px;
}

.detail-sidebar-handle--right {
    right: 8px;
}

.detail-sidebar-control-row .detail-sidebar-handle {
    position: static;
    top: auto;
    right: auto;
    left: auto;
}

.detail-left-drawer--collapsed .detail-sidebar-handle--left {
    left: 8px;
}

.detail-right-drawer--collapsed .detail-sidebar-handle--right {
    right: 8px;
}

.detail-sidebar-icon {
    width: 21px;
    height: 21px;
    flex: 0 0 auto;
    fill: none;
    stroke: currentColor;
    stroke-width: 2.1;
    stroke-linecap: round;
    stroke-linejoin: round;
    shape-rendering: geometricPrecision;
}

.detail-context-side .context-side-header {
    min-height: 52px;
    padding-left: 48px;
}

.detail-left-drawer--related .side-related {
    padding-top: 48px;
}

.detail-right-panel .detail-toc {
    flex: 1 1 auto;
    height: 100%;
    min-height: 0;
    margin-top: 0;
}

.detail-right-panel .article-sidebar-ad {
    flex: 0 0 auto;
}

.sidebar-title-tooltip {
    position: fixed;
    z-index: 1300;
    max-width: min(480px, calc(100vw - 48px));
    padding: 10px 14px;
    color: #fff;
    font-size: 13px;
    font-weight: 600;
    line-height: 1.6;
    word-break: break-word;
    white-space: normal;
    pointer-events: none;
    background: rgba(17, 24, 39, 0.96);
    border-radius: 10px;
    box-shadow: 0 14px 32px rgba(15, 23, 42, 0.22);
    transform: translateY(-50%);
}

.sidebar-title-tooltip::before {
    position: absolute;
    top: 50%;
    width: 10px;
    height: 10px;
    content: "";
    background: rgba(17, 24, 39, 0.96);
    transform: translateY(-50%) rotate(45deg);
}

.sidebar-title-tooltip--left::before {
    left: -5px;
}

.sidebar-title-tooltip--right {
    transform: translate(-100%, -50%);
}

.sidebar-title-tooltip--right::before {
    right: -5px;
}

/* 左侧文章列表面板 */
.detail-context-side {
    min-width: 0;
    height: 100%;
    max-height: none;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow);
}

.context-side-inner {
    display: flex;
    flex-direction: column;
    min-height: 0;
    flex: 1;
    overflow: hidden;
}

.detail-related-side {
    min-width: 0;
    height: 100%;
    max-height: none;
    overflow: hidden;
    scrollbar-width: thin;
}

.detail-related-side::-webkit-scrollbar {
    width: 4px;
}

.detail-related-side::-webkit-scrollbar-thumb {
    background: var(--line-strong);
    border-radius: 999px;
}

.context-side-header {
    flex-shrink: 0;
    padding: 12px 14px 10px;
    border-bottom: 1px solid var(--line);
    background: var(--surface-soft);
}

.context-side-title-link {
    display: flex;
    align-items: center;
    gap: 6px;
    text-decoration: none;
    transition: opacity 0.12s;
}

.context-side-title-link:hover {
    opacity: 0.78;
}

.context-side-type {
    display: inline-flex;
    align-items: center;
    height: 18px;
    padding: 0 5px;
    font-size: 11px;
    font-weight: 600;
    color: var(--brand);
    background: var(--brand-soft);
    border-radius: var(--radius-sm);
    flex-shrink: 0;
}

.context-side-name {
    font-size: 13px;
    font-weight: 700;
    color: var(--text-strong);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.context-side-loading {
    padding: 12px 14px;
    font-size: 13px;
    color: var(--muted);
}

.context-side-list {
    display: flex;
    flex-direction: column;
    flex: 1;
    overflow-y: auto;
    scrollbar-width: thin;
}

.context-side-list::-webkit-scrollbar {
    width: 4px;
}

.context-side-list::-webkit-scrollbar-thumb {
    background: var(--line-strong);
    border-radius: 999px;
}

.context-side-item {
    display: grid;
    grid-template-columns: 20px minmax(0, 1fr);
    gap: 6px;
    align-items: baseline;
    padding: 8px 12px;
    text-decoration: none;
    border-bottom: 1px solid var(--line);
    transition: background 0.1s;
}

.context-side-item:last-child {
    border-bottom: 0;
}

.context-side-item:hover {
    background: var(--surface-soft);
}

.context-side-item--active {
    background: var(--brand-soft);
}

.context-side-item--active .context-side-index,
.context-side-item--active .context-side-item-title {
    color: var(--brand-strong);
    font-weight: 700;
}

.context-side-index {
    font-size: 11px;
    color: var(--muted);
    text-align: right;
    line-height: 1.6;
    flex-shrink: 0;
}

.context-side-item-title {
    font-size: 13px;
    color: var(--text);
    line-height: 1.5;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.context-side-more {
    display: block;
    flex-shrink: 0;
    width: 100%;
    padding: 10px 14px;
    font-size: 12px;
    font-family: inherit;
    color: var(--brand);
    background: transparent;
    text-decoration: none;
    text-align: center;
    border: 0;
    border-top: 1px solid var(--line);
    cursor: pointer;
    transition: background 0.12s, color 0.12s;
}

.context-side-more:hover:not(:disabled) {
    background: var(--brand-soft);
}

.context-side-more:disabled {
    color: var(--muted);
    cursor: default;
}

/* 文章切换时淡入过渡（避免硬切） */
.article-main--loading {
    opacity: 0.45;
    transition: opacity 0.15s;
}

.article-main {
    transition: opacity 0.15s;
}

/* 阅读进度条 */
.reading-progress-bar {
    position: fixed;
    top: 0;
    left: 0;
    z-index: 9999;
    height: 3px;
    background: linear-gradient(90deg, var(--brand), #60a5fa);
    border-radius: 0 2px 2px 0;
    transition: width 0.1s linear;
    pointer-events: none;
}

/* ===== 专栏/专题面包屑导航条 ===== */
/* 宽屏时左侧面板已承担导航，隐藏顶部面包屑条；窄屏（≤980px 左侧面板隐藏）时显示 */
.article-context-bar {
    display: none;
    background: var(--surface);
    border-bottom: 1px solid var(--line);
}

@media (max-width: 980px) {
    .article-context-bar {
        display: block;
    }
}

.article-context-bar-inner {
    display: flex;
    align-items: center;
    padding-top: 10px;
    padding-bottom: 10px;
}

.context-back-link {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    color: var(--muted);
    text-decoration: none;
    font-size: 13px;
    transition: color 0.12s;
}

.context-back-link:hover {
    color: var(--brand);
}

.context-type-badge {
    display: inline-flex;
    align-items: center;
    height: 20px;
    padding: 0 6px;
    font-size: 11px;
    font-weight: 600;
    color: var(--brand);
    background: var(--brand-soft);
    border-radius: var(--radius-sm);
    line-height: 1;
}

.context-back-title {
    font-weight: 500;
    color: var(--text);
    max-width: 280px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.context-back-link:hover .context-back-title {
    color: var(--brand);
}

/* ===== 侧边栏：专栏/专题文章列表 ===== */
.side-context-articles {
    display: grid;
    gap: 0;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    overflow: hidden;
    contain: layout paint;
}

.side-context-header {
    padding: 12px 14px 10px;
    border-bottom: 1px solid var(--line);
    background: var(--surface-soft);
}

.side-context-title-link {
    display: flex;
    align-items: center;
    gap: 6px;
    text-decoration: none;
    transition: opacity 0.12s;
}

.side-context-title-link:hover {
    opacity: 0.78;
}

.side-context-type {
    display: inline-flex;
    align-items: center;
    height: 18px;
    padding: 0 5px;
    font-size: 11px;
    font-weight: 600;
    color: var(--brand);
    background: var(--brand-soft);
    border-radius: var(--radius-sm);
    flex-shrink: 0;
}

.side-context-name {
    font-size: 13px;
    font-weight: 700;
    color: var(--text-strong);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.side-context-loading {
    padding: 12px 14px;
    font-size: 13px;
    color: var(--muted);
}

.side-context-list {
    display: flex;
    flex-direction: column;
    max-height: calc(100vh - 320px);
    overflow-y: auto;
    scrollbar-width: thin;
}

.side-context-list::-webkit-scrollbar {
    width: 4px;
}

.side-context-list::-webkit-scrollbar-thumb {
    background: var(--line-strong);
    border-radius: 999px;
}

.side-context-item {
    display: grid;
    grid-template-columns: 22px minmax(0, 1fr);
    gap: 6px;
    align-items: baseline;
    padding: 8px 14px;
    text-decoration: none;
    border-bottom: 1px solid var(--line);
    transition: background 0.1s;
}

.side-context-item:last-child {
    border-bottom: 0;
}

.side-context-item:hover {
    background: var(--surface-soft);
}

.side-context-item--active {
    background: var(--brand-soft);
}

.side-context-item--active .side-context-index,
.side-context-item--active .side-context-item-title {
    color: var(--brand-strong);
    font-weight: 700;
}

.side-context-index {
    font-size: 11px;
    color: var(--muted);
    text-align: right;
    line-height: 1.6;
    flex-shrink: 0;
}

.side-context-item-title {
    font-size: 13px;
    color: var(--text);
    line-height: 1.5;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.side-context-more {
    display: block;
    padding: 10px 14px;
    font-size: 12px;
    color: var(--brand);
    text-decoration: none;
    text-align: center;
    border-top: 1px solid var(--line);
    transition: background 0.12s;
}

.side-context-more:hover {
    background: var(--brand-soft);
}

.mobile-toc-trigger {
    position: fixed;
    right: 16px;
    bottom: 72px;
    z-index: 70;
    display: none;
    padding: 10px 14px;
    border: 0;
    border-radius: 999px;
    background: var(--brand);
    color: #fff;
    box-shadow: var(--shadow-md);
}

.mobile-toc-overlay {
    position: fixed;
    inset: 0;
    z-index: 120;
    display: grid;
    align-items: end;
    background: rgba(15, 23, 42, 0.38);
}

:global(body.mobile-toc-open) {
    overflow: hidden;
}

.mobile-toc-drawer {
    max-height: 72vh;
    overflow: auto;
    padding: 14px;
    background: var(--surface);
    border-radius: 14px 14px 0 0;
}

.mobile-toc-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
}

.mobile-toc-header h3 {
    margin: 0;
    font-size: 16px;
}

.mobile-toc-header button {
    border: 0;
    background: transparent;
    color: var(--brand);
}

@media (max-width: 980px) {
    .detail-sidebar-handle,
    .detail-left-drawer,
    .detail-right-drawer {
        display: none;
    }

    .mobile-toc-trigger {
        display: inline-flex;
    }

    .detail-layout,
    .detail-layout--context,
    .detail-layout--related-left,
    .detail-layout--left-collapsed,
    .detail-layout--right-collapsed,
    .detail-layout--left-collapsed.detail-layout--right-collapsed {
        grid-template-columns: minmax(0, 1fr);
    }

    .article-sidebar-ad {
        display: none;
    }

    .detail-related-side {
        display: none;
    }

    .detail-context-side {
        display: none;
    }
}

/* 骨架屏动画 */
@keyframes skeleton-shimmer {
    0% { background-position: 200% 0; }
    100% { background-position: -200% 0; }
}

.skeleton-chip,
.skeleton-heading,
.skeleton-summary,
.skeleton-avatar-sm,
.skeleton-name,
.skeleton-date,
.skeleton-paragraph,
.skeleton-toc-title,
.skeleton-toc-item {
    background: linear-gradient(90deg, var(--surface-muted, #f0f3f6) 25%, #e4eaf0 50%, var(--surface-muted, #f0f3f6) 75%);
    background-size: 200% 100%;
    border-radius: 6px;
    animation: skeleton-shimmer 1.4s ease-in-out infinite;
}

.skeleton-panel {
    display: grid;
    gap: 14px;
    padding: 20px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    margin-bottom: 16px;
}

.skeleton-meta-row {
    display: flex;
    gap: 8px;
}

.skeleton-chip {
    height: 20px;
    width: 64px;
    border-radius: 999px;
}

.skeleton-heading {
    height: 36px;
    width: 90%;
}

.skeleton-heading-short {
    width: 60%;
    height: 28px;
}

.skeleton-summary {
    height: 16px;
    width: 100%;
}

.skeleton-summary-short {
    width: 72%;
}

.skeleton-author-row {
    display: flex;
    gap: 12px;
    align-items: center;
    margin-top: 4px;
}

.skeleton-avatar-sm {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    flex-shrink: 0;
}

.skeleton-author-meta {
    display: grid;
    gap: 8px;
    flex: 1;
}

.skeleton-name {
    height: 14px;
    width: 120px;
}

.skeleton-date {
    height: 12px;
    width: 80px;
}

.skeleton-content-block {
    display: grid;
    gap: 12px;
    padding: 20px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.skeleton-paragraph {
    height: 14px;
}

.skeleton-short {
    width: 40% !important;
    height: 14px;
    margin-bottom: 10px;
}

.skeleton-toc {
    display: grid;
    gap: 10px;
    padding: 20px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.skeleton-toc-title {
    height: 18px;
    width: 60%;
    margin-bottom: 4px;
}

.skeleton-toc-item {
    height: 12px;
}

.article-heading-panel {
    display: grid;
    gap: 16px;
    margin-bottom: 24px;
    padding: 20px 20px 20px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    box-shadow: none;
}

.article-heading-top,
.article-heading-bottom {
    display: flex;
    gap: 12px;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
}

.article-meta-row {
    margin: 0;
}

.article-heading-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 12px 24px;
    flex: 1 1 460px;
    min-width: 0;
    align-items: center;
}

.article-byline {
    display: flex;
    align-items: center;
    gap: 10px;
    text-decoration: none;
    color: inherit;
    flex: 0 1 260px;
    min-width: 220px;
}

.byline-avatar,
.byline-avatar-fallback {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    object-fit: cover;
    flex-shrink: 0;
}

.byline-avatar-fallback {
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--surface-soft);
    color: var(--muted);
    font-size: 13px;
    font-weight: 700;
}

.byline-info {
    display: grid;
    gap: 1px;
    min-width: 0;
}

.byline-name-row {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    min-width: 0;
}

.byline-name {
    font-size: 13px;
    font-weight: 600;
    color: var(--text);
    line-height: 1.4;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.byline-meta {
    display: flex;
    gap: 6px;
    align-items: center;
    font-size: 12px;
    color: var(--muted);
    line-height: 1.4;
    white-space: nowrap;
}

.byline-date {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
}

.byline-separator {
    flex: none;
}

.byline-followers {
    flex: none;
    min-width: 52px;
}

.article-stats-row {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    align-items: center;
}

.article-stat {
    font-size: 12px;
    color: var(--muted);
    line-height: 1;
    white-space: nowrap;
}

.article-stat strong {
    color: var(--text-strong);
    font-size: 13px;
    font-weight: 600;
}

.article-quick-actions {
    display: flex;
    gap: 10px;
    align-items: center;
    flex-wrap: wrap;
    justify-content: flex-end;
    flex: 0 1 auto;
}

.article-quick-button {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 88px;
    min-width: 88px;
    min-height: 34px;
    padding: 0 10px;
    color: var(--text);
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    transition: color 0.12s, border-color 0.12s, background 0.12s;
}

.article-quick-button:hover:not(:disabled) {
    color: var(--brand);
    background: var(--brand-soft);
    border-color: var(--brand);
}

.article-quick-button.active {
    color: var(--brand);
    background: var(--brand-soft);
    border-color: var(--brand);
}

.article-quick-button.subtle {
    color: var(--muted);
}

.article-quick-label {
    flex: none;
}

.article-quick-button:disabled {
    cursor: not-allowed;
    opacity: 0.72;
}

.article-follow-button {
    flex: none;
    width: 88px;
}

.article-comment {
    margin-top: 40px;
    padding-top: 24px;
    border-top: 1px solid var(--line);
}

.article-selection-comment {
    position: fixed;
    z-index: 120;
    min-height: 34px;
    padding: 0 14px;
    color: #ffffff;
    font-size: 13px;
    font-weight: 700;
    cursor: pointer;
    background: var(--brand);
    border: 1px solid var(--brand-strong);
    border-radius: var(--radius-md);
    box-shadow: 0 10px 24px rgba(37, 99, 235, 0.22);
    transform: translateX(-50%);
}

.article-selection-comment:hover {
    background: var(--brand-strong);
}

.article-selection-composer {
    position: fixed;
    z-index: 130;
    width: min(520px, calc(100vw - 32px));
    padding: 12px;
    background: var(--surface);
    border: 1px solid rgba(37, 99, 235, 0.24);
    border-radius: var(--radius-md);
    box-shadow: 0 18px 48px rgba(15, 23, 42, 0.18);
    transform: translateX(-50%);
}

.article-selection-quote {
    display: grid;
    gap: 4px;
    margin-bottom: 10px;
    padding: 10px 12px;
    color: var(--muted);
    font-size: 13px;
    line-height: 1.6;
    background: rgba(37, 99, 235, 0.06);
    border-left: 3px solid var(--brand);
    border-radius: var(--radius-sm);
}

.article-selection-quote strong {
    color: var(--brand-strong);
    font-size: 13px;
}

.article-selection-quote span {
    display: -webkit-box;
    overflow: hidden;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
}

.article-unlocked-content :deep(.article-quote-flash) {
    animation: article-quote-flash 1.8s ease;
    border-radius: var(--radius-sm);
}

@keyframes article-quote-flash {
    0%,
    100% {
        background: transparent;
        box-shadow: none;
    }
    20%,
    70% {
        background: rgba(37, 99, 235, 0.12);
        box-shadow: 0 0 0 6px rgba(37, 99, 235, 0.08);
    }
}

.article-content-empty {
    padding: 24px 20px;
    color: var(--muted);
    background: var(--surface-soft);
    border: 1px dashed var(--line);
    border-radius: var(--radius-sm);
}

.article-content-empty p {
    margin: 0;
}

.article-tag-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.article-tag-chip {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-height: 28px;
    padding: 0 10px;
    line-height: 1;
    color: var(--text);
    font-size: 13px;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.comment-placeholder {
    padding: 32px;
    text-align: center;
    color: var(--muted);
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.side-related {
    display: flex;
    flex: 1 1 auto;
    flex-direction: column;
    gap: 12px;
    height: 100%;
    min-height: 0;
    padding: 16px;
    overflow: hidden;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    contain: layout paint;
}

.side-related-title {
    margin: 0;
    font-size: 15px;
    font-weight: 700;
    color: var(--text-strong);
}

.side-related-loading {
    color: var(--muted);
    font-size: 13px;
    padding: 8px 0;
}

.side-related-sections {
    display: grid;
    gap: 18px;
    min-height: 0;
    overflow-y: auto;
    scrollbar-width: thin;
}

.side-related-sections::-webkit-scrollbar {
    width: 4px;
}

.side-related-sections::-webkit-scrollbar-thumb {
    background: var(--line-strong);
    border-radius: 999px;
}

.side-related-group {
    display: grid;
    gap: 8px;
}

.side-related-group h3 {
    margin: 0;
    color: var(--muted);
    font-size: 12px;
    font-weight: 600;
}

.side-related-list {
    display: flex;
    flex-direction: column;
    gap: 0;
}

.side-related-item {
    display: grid;
    grid-template-columns: 56px minmax(0, 1fr);
    gap: 10px;
    align-items: center;
    text-decoration: none;
    padding: 8px 6px;
    margin: 0 -6px;
    border-radius: var(--radius-sm);
    transition: background 0.12s;
}

.side-related-item:hover {
    background: var(--surface-soft);
}

.side-related-thumb {
    width: 56px;
    height: 40px;
    object-fit: cover;
    border-radius: var(--radius-sm);
    flex-shrink: 0;
}

.side-related-info {
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 2px;
}

.side-related-item-title {
    margin: 0;
    font-size: 13px;
    font-weight: 600;
    color: var(--text-strong);
    line-height: 1.4;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.side-related-stats {
    display: flex;
    gap: 8px;
    font-size: 11px;
    color: var(--muted);
}

@media (max-width: 760px) {
    .detail-layout {
        grid-template-columns: 1fr;
    }

    .detail-layout--context {
        grid-template-columns: 1fr;
    }

    .detail-layout--related-left {
        grid-template-columns: 1fr;
    }

    .detail-context-side {
        display: none;
    }

    .article-heading-panel {
        padding: 18px;
    }

    .article-byline {
        flex-basis: 100%;
        min-width: 0;
    }

    .article-quick-actions {
        justify-content: flex-start;
    }

    .article-heading-meta {
        flex-basis: 100%;
    }

    .article-related-list {
        gap: 0;
    }
}

/* 分享卡片（B 站风格） */
.article-export-wrap,
.article-share-wrap {
    position: relative;
    flex: none;
    width: 88px;
}

.article-export-wrap .article-quick-button,
.article-share-wrap .article-quick-button {
    width: 100%;
    min-width: 100%;
}

.article-share-card {
    position: absolute;
    right: 0;
    bottom: calc(100% + 10px);
    z-index: 100;
    min-width: 150px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: 10px;
    box-shadow: 0 6px 24px rgba(0, 0, 0, 0.12);
    animation: share-card-in 0.2s ease;
}

.article-export-card {
    min-width: 168px;
}

@keyframes share-card-in {
    from {
        opacity: 0;
        transform: translateY(6px) scale(0.96);
    }
    to {
        opacity: 1;
        transform: translateY(0) scale(1);
    }
}

.share-card-arrow {
    position: absolute;
    bottom: -6px;
    right: 36px;
    width: 12px;
    height: 12px;
    background: var(--surface);
    border-right: 1px solid var(--line);
    border-bottom: 1px solid var(--line);
    transform: rotate(45deg);
}

.share-card-body {
    padding: 8px;
}

.share-option {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 9px 12px;
    font-size: 14px;
    color: var(--text-strong);
    cursor: pointer;
    border-radius: 8px;
    transition: background 0.15s;
    white-space: nowrap;
}

button.share-option {
    width: 100%;
    font-family: inherit;
    text-align: left;
    background: transparent;
    border: 0;
}

.share-option:disabled {
    cursor: not-allowed;
    opacity: 0.58;
}

.share-option:hover {
    background: var(--surface-soft);
}

.share-option:disabled:hover {
    background: transparent;
}

.share-icon {
    flex: none;
    color: var(--muted);
}

@media (max-width: 768px) {
    .article-share-wrap {
        display: none;
    }
}

/* ===== 上下篇导航 ===== */
.article-neighbors {
    margin-top: 40px;
    padding-top: 24px;
    border-top: 1px solid var(--line);
}

.article-neighbors-context {
    margin-bottom: 10px;
}

.article-neighbors-context-label {
    font-size: 12px;
    color: var(--muted);
    font-weight: 500;
}

.article-neighbors-row {
    display: flex;
    justify-content: space-between;
    gap: 16px;
}

.article-neighbor {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 4px;
    padding: 12px 16px;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    text-decoration: none;
    transition: border-color 0.12s, background 0.12s;
}

.article-neighbor:hover {
    border-color: var(--brand);
    background: var(--brand-soft);
}

.article-neighbor--empty {
    visibility: hidden;
}

.article-neighbor--next {
    text-align: right;
}

.article-neighbor-label {
    font-size: 12px;
    color: var(--muted);
}

.article-neighbor-title {
    font-size: 14px;
    color: var(--text);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}
</style>
