<script setup>
import { computed, inject, ref, watch } from 'vue';
import { createCommentApi, pageCommentsApi } from '@/api/comments';
import CommentComposer from '@/components/CommentComposer.vue';
import CommentRootItem from '@/components/CommentRootItem.vue';
import { useStableListRequest } from '@/composables/useStableListRequest';
import { useSession } from '@/stores/session';

const props = defineProps({
    articleId: {
        type: [Number, String],
        required: true
    },
    initialCount: {
        type: Number,
        default: 0
    }
});

const emit = defineEmits(['count-change']);

const { state } = useSession();
const loginModal = inject('loginModal', { requireLogin: () => false });

const composerDraft = ref('');
const composerFeedback = ref('');
const composerSubmitting = ref(false);

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

async function submitComment() {
    const canContinue = loginModal.requireLogin(() => submitComment(), {
        title: '登录后发表评论',
        message: '登录后可以参与讨论，和其他读者一起把评论区聊热起来。',
        actionText: '登录并评论'
    });
    if (!canContinue) {
        composerFeedback.value = '登录后可以发表评论';
        return;
    }
    const content = composerDraft.value.trim();
    if (!content) {
        composerFeedback.value = '评论内容不能为空';
        return;
    }
    composerSubmitting.value = true;
    try {
        await createCommentApi(props.articleId, {
            content,
            parentId: 0,
            rootCommentId: 0
        });
        composerDraft.value = '';
        composerFeedback.value = '评论已发布';
        handleCountChange(1);
        await fetchComments(1);
    } catch (error) {
        composerFeedback.value = error.message || '发表评论失败';
    } finally {
        composerSubmitting.value = false;
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
            v-model="composerDraft"
            class="comment-panel-composer"
            :avatar-url="avatarUrl"
            :feedback="composerFeedback"
            :submitting="composerSubmitting"
            placeholder="发一条友善的评论，和更多读者一起聊聊这篇文章。"
            submit-text="发表评论"
            @submit="submitComment"
        />

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
                @refresh="fetchComments(currentPage)"
                @count-change="handleCountChange"
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
    </section>
</template>

<style scoped>
.comment-panel {
    display: grid;
    gap: 18px;
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

.comment-panel-list {
    display: grid;
    gap: 20px;
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
}
</style>
