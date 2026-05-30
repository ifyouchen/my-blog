<script setup>
import {computed, inject, nextTick, ref, watch} from 'vue';
import { createCommentApi, pageCommentsApi } from '@/api/comments';
import CommentComposer from '@/components/CommentComposer.vue';
import CommentRootItem from '@/components/CommentRootItem.vue';
import ConfirmDialog from '@/components/ConfirmDialog.vue';
import { useStableListRequest } from '@/composables/useStableListRequest';
import { useConfirmDialog } from '@/composables/useConfirmDialog';
import { useSession } from '@/stores/session';
import {findWarnSensitiveWords, formatWarnSensitiveWords} from '@/utils/sensitiveWords';

const props = defineProps({
    articleId: {
        type: [Number, String],
        required: true
    },
    initialCount: {
        type: Number,
        default: 0
    },
    pendingQuote: {
        type: Object,
        default: null
    }
});

const emit = defineEmits(['count-change', 'quote-clear', 'quote-jump']);

const { state, isLoggedIn } = useSession();
const loginModal = inject('loginModal', { requireLogin: () => false });
const {
    confirmDialog,
    openConfirmDialog,
    closeConfirmDialog,
    executeConfirmDialog
} = useConfirmDialog();

const composerDraft = ref('');
const composerFeedback = ref('');
const composerSubmitting = ref(false);
const composerRef = ref(null);
let lastLoginPromptAt = 0;

const comments = ref([]);
const {
    initialLoading,
    refreshing,
    hasLoadedOnce,
    errorMessage,
    inlineError,
    loading,
    runStableRequest,
    resetStableRequest
} = useStableListRequest();
const sort = ref('latest');
const currentPage = ref(1);
const pageSize = 10;
const rootTotal = ref(0);
const commentCount = ref(props.initialCount || 0);

watch(() => props.initialCount, (value) => {
    commentCount.value = Number(value) || 0;
});

const currentUser = computed(() => state.user || null);
const totalPages = computed(() => Math.max(1, Math.ceil(rootTotal.value / pageSize)));
const avatarUrl = computed(() => currentUser.value?.avatar || currentUser.value?.avatarUrl);
const activeQuote = computed(() => {
    const quoteText = props.pendingQuote?.quoteText?.trim();
    if (!quoteText) {
        return null;
    }
    return {
        quoteText,
        quotePrefix: props.pendingQuote?.quotePrefix || '',
        quoteSuffix: props.pendingQuote?.quoteSuffix || ''
    };
});

function promptCommentLogin(onSuccess = () => {}) {
    const now = Date.now();
    if (now - lastLoginPromptAt < 500) {
        return false;
    }
    lastLoginPromptAt = now;
    return loginModal.requireLogin(onSuccess, {
        title: '登录后发表评论',
        message: '登录后可以参与讨论，和其他读者一起交流文章里的技术细节。',
        actionText: '登录并评论'
    });
}

async function fetchComments(page = currentPage.value, { reset = false } = {}) {
    if (reset) {
        resetStableRequest();
        comments.value = [];
        currentPage.value = 1;
        rootTotal.value = 0;
    }

    const { result } = await runStableRequest(
        () => pageCommentsApi(props.articleId, {
            page,
            pageSize,
            sort: sort.value
        }),
        {
            silent: hasLoadedOnce.value,
            initialErrorMessage: '评论加载失败',
            refreshErrorMessage: '评论刷新失败，请稍后重试'
        }
    );

    if (!result) {
        return;
    }

    const nextComments = result.items || [];
    const nextTotal = result.total || 0;
    const nextPage = result.page || page;
    if (!nextComments.length && nextPage > 1 && nextTotal > 0) {
        const fallbackPage = Math.max(1, Math.ceil(nextTotal / pageSize));
        if (fallbackPage !== nextPage) {
            await fetchComments(fallbackPage);
            return;
        }
    }

    comments.value = nextComments;
    rootTotal.value = nextTotal;
    currentPage.value = nextPage;
}

async function confirmSensitiveComment(content, actionText, onConfirm) {
    const sensitiveHits = await findWarnSensitiveWords(content);
    if (!sensitiveHits.length) {
        return false;
    }
    const words = formatWarnSensitiveWords(sensitiveHits);
    composerFeedback.value = `存在敏感词 ${words}，请修改或确认继续${actionText}`;
    openConfirmDialog({
        eyebrow: '敏感词提醒',
        title: '评论包含警告词',
        message: `存在敏感词 ${words}，发送后会自动替换为 ***。是否确认${actionText}？`,
        confirmText: `确认${actionText}`,
        cancelText: '返回修改',
        tone: 'warning',
        onConfirm
    });
    return true;
}

async function submitComment(options = {}) {
    const canContinue = promptCommentLogin(() => submitComment());
    if (!canContinue) {
        composerFeedback.value = '登录后可以发表评论';
        return;
    }
    const content = composerDraft.value.trim();
    if (!content) {
        composerFeedback.value = '评论内容不能为空';
        return;
    }
    if (options.skipSensitiveWarning !== true) {
        composerSubmitting.value = true;
        const waitingConfirm = await confirmSensitiveComment(content, '发表评论', async () => {
            await submitComment({ skipSensitiveWarning: true });
        });
        composerSubmitting.value = false;
        if (waitingConfirm) {
            return;
        }
    }
    composerSubmitting.value = true;
    try {
        const createdComment = await createCommentApi(props.articleId, {
            content,
            parentId: 0,
            rootCommentId: 0,
            quoteText: activeQuote.value?.quoteText || '',
            quotePrefix: activeQuote.value?.quotePrefix || '',
            quoteSuffix: activeQuote.value?.quoteSuffix || ''
        });
        composerDraft.value = '';
        composerFeedback.value = '评论已发布';
        emit('quote-clear');
        handleCountChange(1);
        insertCreatedRootComment(createdComment);
    } catch (error) {
        composerFeedback.value = error.message || '发表评论失败';
    } finally {
        composerSubmitting.value = false;
    }
}

function insertCreatedRootComment(comment) {
    if (!comment?.id) {
        return;
    }
    rootTotal.value += 1;
    const nextComments = [
        comment,
        ...comments.value.filter((item) => String(item.id) !== String(comment.id))
    ];
    comments.value = nextComments.slice(0, pageSize);
}

function insertExternalRootComment(comment) {
    if (!comment?.id) {
        return;
    }
    rootTotal.value += 1;
    comments.value = [
        comment,
        ...comments.value.filter((item) => String(item.id) !== String(comment.id))
    ].slice(0, pageSize);
}

async function handleRootDelete(payload) {
    const id = payload?.id;
    if (!id) {
        return;
    }
    comments.value = comments.value.filter((item) => String(item.id) !== String(id));
    rootTotal.value = Math.max(0, rootTotal.value - 1);
    const nextTotalPages = Math.max(1, Math.ceil(rootTotal.value / pageSize));
    currentPage.value = Math.min(currentPage.value, nextTotalPages);
    if (!comments.value.length && rootTotal.value > 0) {
        await fetchComments(currentPage.value);
    }
}

function handleCountChange(delta) {
    const next = Math.max(0, commentCount.value + delta);
    commentCount.value = next;
    emit('count-change', delta);
}

function changeSort(nextSort) {
    if (sort.value === nextSort) {
        return;
    }
    sort.value = nextSort;
    fetchComments(1);
}

function goPage(page) {
    if (page < 1 || page > totalPages.value || page === currentPage.value || loading.value) {
        return;
    }
    fetchComments(page);
}

watch(() => props.articleId, () => {
    fetchComments(1, { reset: true });
}, { immediate: true });

watch(() => props.pendingQuote, async (value) => {
    if (!value?.quoteText) {
        return;
    }
    await nextTick();
    composerRef.value?.focus?.();
}, { deep: true });

defineExpose({ insertExternalRootComment });
</script>

<template>
    <section class="comment-panel" data-testid="comment-panel">
        <header class="comment-panel-header">
            <div>
                <h2 class="comment-panel-title">评论 {{ commentCount }}</h2>
                <p class="comment-panel-subtitle">默认按最新讨论展示，楼中楼会在当前评论下展开。</p>
            </div>
            <div class="comment-sort-tabs" data-testid="comment-sort-tabs">
                <button
                    type="button"
                    :class="{ active: sort === 'latest' }"
                    @click="changeSort('latest')"
                >
                    最新
                </button>
                <button
                    type="button"
                    :class="{ active: sort === 'hot' }"
                    @click="changeSort('hot')"
                >
                    最热
                </button>
            </div>
        </header>

        <CommentComposer
            ref="composerRef"
            v-model="composerDraft"
            class="comment-panel-composer"
            :avatar-url="avatarUrl"
            :feedback="composerFeedback"
            :submitting="composerSubmitting"
            :readonly="!isLoggedIn"
            :placeholder="isLoggedIn ? '发一条友善的评论，和更多读者一起聊聊这篇文章。' : '登录后参与讨论，评论列表可直接浏览。'"
            submit-text="发表评论"
            @activate="promptCommentLogin(() => submitComment())"
            @submit="submitComment"
        />

        <div v-if="activeQuote" class="comment-pending-quote">
            <button
                type="button"
                class="comment-pending-quote-text"
                title="回到引用原文"
                @click="emit('quote-jump', activeQuote)"
            >
                <span>{{ activeQuote.quoteText }}</span>
            </button>
            <button type="button" class="comment-pending-quote-clear" @click="emit('quote-clear')">取消引用</button>
        </div>

        <div v-if="refreshing && comments.length" class="comment-panel-refresh">正在更新评论...</div>
        <div v-if="inlineError" class="comment-panel-state error">{{ inlineError }}</div>
        <div
            v-if="initialLoading || (refreshing && !comments.length)"
            class="comment-panel-state"
        >
            评论加载中...
        </div>
        <div v-else-if="errorMessage && !comments.length" class="comment-panel-state error">{{ errorMessage }}</div>
        <div v-else-if="hasLoadedOnce && !refreshing && !comments.length" class="comment-panel-state">
            还没有评论，来写下第一条想法。
        </div>
        <div v-else class="comment-panel-list" data-testid="comment-list">
            <CommentRootItem
                v-for="comment in comments"
                :key="comment.id"
                :article-id="articleId"
                :comment="comment"
                :current-user="currentUser"
                @count-change="handleCountChange"
                @root-delete="handleRootDelete"
                @quote-jump="emit('quote-jump', $event)"
            />
        </div>

        <footer v-if="rootTotal > 0" class="comment-panel-footer">
            <div class="comment-panel-meta">
                <span>一级评论 {{ rootTotal }} 条</span>
                <span>第 {{ currentPage }} / {{ totalPages }} 页</span>
            </div>
            <div v-if="totalPages > 1" class="comment-panel-pagination">
                <button type="button" :disabled="currentPage <= 1 || loading" @click="goPage(1)">首页</button>
                <button
                    type="button"
                    :disabled="currentPage <= 1 || loading"
                    @click="goPage(currentPage - 1)"
                >
                    上一页
                </button>
                <button
                    type="button"
                    :disabled="currentPage >= totalPages || loading"
                    @click="goPage(currentPage + 1)"
                >
                    下一页
                </button>
                <button
                    type="button"
                    :disabled="currentPage >= totalPages || loading"
                    @click="goPage(totalPages)"
                >
                    末页
                </button>
            </div>
        </footer>

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
    </section>
</template>

<style scoped>
.comment-panel {
    display: grid;
    gap: 18px;
    overflow: visible;
}

.comment-panel-header,
.comment-panel-footer {
    display: flex;
    gap: 16px;
    align-items: flex-start;
    justify-content: space-between;
}

.comment-panel-title {
    margin: 0;
    color: var(--text);
    font-size: 22px;
    font-weight: 700;
}

.comment-panel-subtitle {
    margin: 6px 0 0;
    color: var(--muted);
    font-size: 13px;
    line-height: 1.6;
}

.comment-sort-tabs {
    display: inline-flex;
    gap: 8px;
    align-items: center;
    padding: 4px;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
}

.comment-sort-tabs button {
    min-height: 34px;
    padding: 0 14px;
    color: var(--muted);
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    background: transparent;
    border: 0;
    border-radius: var(--radius-sm);
}

.comment-sort-tabs button.active {
    color: var(--brand-strong);
    background: var(--surface);
    box-shadow: 0 6px 18px rgba(15, 23, 42, 0.06);
}

.comment-panel-composer {
    margin-top: 2px;
}

.comment-pending-quote {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 10px;
    align-items: center;
    padding: 10px 12px;
    background: var(--brand-soft);
    border: 1px solid var(--brand-hover);
    border-radius: var(--radius-md);
}

.comment-pending-quote-text {
    min-width: 0;
    padding: 0;
    color: var(--text);
    text-align: left;
    cursor: pointer;
    background: transparent;
    border: 0;
}

.comment-pending-quote-text span {
    display: -webkit-box;
    overflow: hidden;
    font-size: 13px;
    line-height: 1.6;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
}

.comment-pending-quote-clear {
    min-height: 30px;
    padding: 0 10px;
    color: var(--brand-strong);
    font-size: 13px;
    cursor: pointer;
    background: var(--surface);
    border: 1px solid var(--brand-hover);
    border-radius: var(--radius-sm);
}

.comment-panel-list {
    display: grid;
    gap: 20px;
    overflow: visible;
}

.comment-panel-state {
    padding: 18px 20px;
    color: var(--muted);
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
}

.comment-panel-state.error {
    color: #b42318;
}

.comment-panel-refresh {
    padding: 8px 12px;
    color: var(--brand-strong);
    font-size: 13px;
    background: var(--brand-soft);
    border: 1px solid var(--brand-hover);
    border-radius: var(--radius-sm);
}

.comment-panel-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    color: var(--muted);
    font-size: 13px;
}

.comment-panel-pagination {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    justify-content: flex-end;
}

.comment-panel-pagination button {
    min-height: 34px;
    padding: 0 12px;
    color: var(--text);
    font-size: 13px;
    cursor: pointer;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
}

.comment-panel-pagination button:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

@media (max-width: 720px) {
    .comment-panel-header,
    .comment-panel-footer {
        align-items: stretch;
        flex-direction: column;
    }

    .comment-sort-tabs,
    .comment-panel-pagination {
        width: fit-content;
    }

    .comment-pending-quote {
        grid-template-columns: minmax(0, 1fr);
    }

    .comment-pending-quote-clear {
        width: fit-content;
    }
}
</style>
