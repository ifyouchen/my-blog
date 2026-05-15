<script setup>
import {computed, inject, ref, watch} from 'vue';
import CommentComposer from '@/components/CommentComposer.vue';
import ConfirmDialog from '@/components/ConfirmDialog.vue';
import ReportDialog from '@/components/ReportDialog.vue';
import UserHoverCard from '@/components/UserHoverCard.vue';
import {
  createCommentApi,
  deleteCommentApi,
  editCommentApi,
  likeCommentApi,
  pageRepliesApi,
  pinCommentApi,
  unlikeCommentApi,
  unpinCommentApi
} from '@/api/comments';
import {useConfirmDialog} from '@/composables/useConfirmDialog';
import {findWarnSensitiveWords, formatWarnSensitiveWords} from '@/utils/sensitiveWords';

const props = defineProps({
    articleId: {
        type: [Number, String],
        required: true
    },
    comment: {
        type: Object,
        required: true
    },
    currentUser: {
        type: Object,
        default: null
    }
});

const emit = defineEmits(['count-change', 'root-delete']);

const loginModal = inject('loginModal', { requireLogin: () => false });
const {
    confirmDialog,
    openConfirmDialog,
    closeConfirmDialog,
    executeConfirmDialog
} = useConfirmDialog();

const expandedReplies = ref(false);
const repliesLoading = ref(false);
const replies = ref([]);
const replyPage = ref(1);
const replyPageSize = 10;
const replyTotal = ref(0);
const replyDraft = ref('');
const replyFeedback = ref('');
const replySubmitting = ref(false);
const activeReplyTarget = ref(null);
const reportTarget = ref(null);
const editingComment = ref(false);
const editDraft = ref('');
const editFeedback = ref('');
const editSubmitting = ref(false);
const localComment = ref(createCommentSnapshot(props.comment));

watch(() => props.comment, (value) => {
    localComment.value = createCommentSnapshot(value);
}, { deep: true });

const avatarUrl = computed(() => {
    return props.currentUser?.avatar || props.currentUser?.avatarUrl;
});

const visibleReplies = computed(() => {
    return expandedReplies.value ? replies.value : localComment.value.replyPreview;
});

const displayedReplyCount = computed(() => {
    return expandedReplies.value ? replyTotal.value : localComment.value.replyCount;
});

function createCommentSnapshot(comment) {
    return {
        ...comment,
        user: comment.user ? { ...comment.user } : null,
        replyToUser: comment.replyToUser ? { ...comment.replyToUser } : null,
        replyPreview: (comment.replyPreview || []).map((item) => ({
            ...item,
            user: item.user ? { ...item.user } : null,
            replyToUser: item.replyToUser ? { ...item.replyToUser } : null
        }))
    };
}

async function fetchReplies(page = 1) {
    repliesLoading.value = true;
    try {
        const result = await pageRepliesApi(localComment.value.id, {
            page,
            pageSize: replyPageSize
        });
        replies.value = result.items || [];
        replyPage.value = result.page || page;
        replyTotal.value = result.total || 0;
        return true;
    } catch (error) {
        replyFeedback.value = error.message || '加载回复失败';
        return false;
    } finally {
        repliesLoading.value = false;
    }
}

async function expandReplies(page = 1) {
    const loaded = await fetchReplies(page);
    if (loaded) {
        expandedReplies.value = true;
    }
}

function startReply(target) {
    const canContinue = loginModal.requireLogin(() => startReply(target), {
        title: '登录后回复评论',
        message: '登录后可以继续这段讨论，回复会展示在当前楼中楼中。',
        actionText: '登录并回复'
    });
    if (!canContinue) {
        replyFeedback.value = '登录后可以回复评论';
        return;
    }
    activeReplyTarget.value = target;
    replyFeedback.value = '';
}

function cancelReply() {
    activeReplyTarget.value = null;
    replyDraft.value = '';
    replyFeedback.value = '';
}

async function confirmSensitiveComment(content, actionText, feedbackRef, onConfirm) {
    const sensitiveHits = await findWarnSensitiveWords(content);
    if (!sensitiveHits.length) {
        return false;
    }
    const words = formatWarnSensitiveWords(sensitiveHits);
    feedbackRef.value = `存在敏感词 ${words}，请修改或确认继续${actionText}`;
    openConfirmDialog({
        eyebrow: '敏感词提醒',
        title: '评论包含警告词',
        message: `存在敏感词 ${words}，提交后会自动替换为 ***。是否确认${actionText}？`,
        confirmText: `确认${actionText}`,
        cancelText: '返回修改',
        tone: 'warning',
        onConfirm
    });
    return true;
}

async function submitReply(options = {}) {
    const canContinue = loginModal.requireLogin(() => submitReply(), {
        title: '登录后回复评论',
        message: '登录后可以继续这段讨论，回复会展示在当前楼中楼中。',
        actionText: '登录并回复'
    });
    if (!canContinue) {
        replyFeedback.value = '登录后可以回复评论';
        return;
    }
    const content = replyDraft.value.trim();
    if (!content || !activeReplyTarget.value) {
        replyFeedback.value = '回复内容不能为空';
        return;
    }
    if (options.skipSensitiveWarning !== true) {
        replySubmitting.value = true;
        const waitingConfirm = await confirmSensitiveComment(content, '发送回复', replyFeedback, async () => {
            await submitReply({ skipSensitiveWarning: true });
        });
        replySubmitting.value = false;
        if (waitingConfirm) {
            return;
        }
    }
    replySubmitting.value = true;
    try {
        const createdReply = await createCommentApi(props.articleId, {
            content,
            rootCommentId: localComment.value.id,
            parentId: activeReplyTarget.value.id
        });
        const nextReplyCount = (localComment.value.replyCount || 0) + 1;
        localComment.value.replyCount = nextReplyCount;
        replyTotal.value = nextReplyCount;
        emit('count-change', 1);
        replyDraft.value = '';
        activeReplyTarget.value = null;
        replyFeedback.value = '回复已发布';
        if (expandedReplies.value) {
            const lastPage = Math.max(1, Math.ceil(nextReplyCount / replyPageSize));
            await expandReplies(lastPage);
        } else if (localComment.value.replyPreview.length < 3) {
            localComment.value.replyPreview = [...localComment.value.replyPreview, createdReply];
        }
    } catch (error) {
        replyFeedback.value = error.message || '回复失败';
    } finally {
        replySubmitting.value = false;
    }
}

async function toggleLike(target) {
    const canContinue = loginModal.requireLogin(() => toggleLike(target), {
        title: '登录后点赞评论',
        message: '登录后可以点赞你认同的评论，并同步到账号状态。',
        actionText: '登录并点赞'
    });
    if (!canContinue) {
        replyFeedback.value = '登录后可以点赞评论';
        return;
    }
    const previousLiked = target.liked;
    const previousLikeCount = target.likeCount || 0;
    target.liked = !previousLiked;
    target.likeCount = Math.max(0, previousLikeCount + (previousLiked ? -1 : 1));
    try {
        if (previousLiked) {
            await unlikeCommentApi(target.id);
        } else {
            await likeCommentApi(target.id);
        }
    } catch (error) {
        target.liked = previousLiked;
        target.likeCount = previousLikeCount;
        replyFeedback.value = error.message || '操作失败';
    }
}

async function togglePin() {
    const canContinue = loginModal.requireLogin(() => togglePin(), {
        title: '登录后管理评论',
        message: '登录后会校验作者或管理员身份，再执行置顶操作。',
        actionText: '登录并继续'
    });
    if (!canContinue) {
        replyFeedback.value = '登录后可以管理评论';
        return;
    }
    try {
        const nextPinned = !localComment.value.pinned;
        if (localComment.value.pinned) {
            await unpinCommentApi(localComment.value.id);
        } else {
            await pinCommentApi(localComment.value.id);
        }
        localComment.value.pinned = nextPinned;
    } catch (error) {
        replyFeedback.value = error.message || '置顶操作失败';
    }
}

async function removeComment(target) {
    const canContinue = loginModal.requireLogin(() => removeComment(target), {
        title: '登录后管理评论',
        message: '登录后会校验你的身份，只允许删除自己或有权限管理的评论。',
        actionText: '登录并继续'
    });
    if (!canContinue) {
        replyFeedback.value = '登录后可以管理评论';
        return;
    }
    openConfirmDialog({
        eyebrow: '评论删除确认',
        title: '删除评论',
        message: '确定要删除这条评论吗？删除后该评论及其关联展示将从当前讨论区移除。',
        confirmText: '确认删除',
        tone: 'danger',
        onConfirm: async () => {
            try {
                await deleteCommentApi(target.id);
                if (target.id === localComment.value.id) {
                    emit('count-change', -(1 + (localComment.value.replyCount || 0)));
                    emit('root-delete', { id: localComment.value.id });
                    return;
                }
                localComment.value.replyCount = Math.max(0, (localComment.value.replyCount || 0) - 1);
                replyTotal.value = Math.max(0, replyTotal.value - 1);
                replies.value = replies.value.filter((item) => item.id !== target.id);
                localComment.value.replyPreview = localComment.value.replyPreview.filter((item) => item.id !== target.id);
                emit('count-change', -1);
                if (expandedReplies.value) {
                    const nextPage = Math.max(1, Math.min(
                        replyPage.value,
                        Math.ceil(Math.max(replyTotal.value, 1) / replyPageSize)
                    ));
                    if (!replies.value.length && replyTotal.value > 0) {
                        await fetchReplies(nextPage);
                    } else {
                        replyPage.value = nextPage;
                    }
                }
            } catch (error) {
                replyFeedback.value = error.message || '删除失败';
            }
        }
    });
}

function openReport(target) {
    const canContinue = loginModal.requireLogin(() => openReport(target), {
        title: '登录后举报评论',
        message: '登录后可以提交评论举报，管理员会在后台处理。',
        actionText: '登录并举报'
    });
    if (!canContinue) {
        replyFeedback.value = '登录后可以举报评论';
        return;
    }
    reportTarget.value = {
        id: target.id,
        title: `评论 #${target.id}`
    };
}

function closeReportDialog() {
    reportTarget.value = null;
}

function handleReportSuccess() {
    closeReportDialog();
}

function startEditComment() {
    const canContinue = loginModal.requireLogin(() => startEditComment(), {
        title: '登录后编辑评论',
        message: '登录后才能编辑自己的评论。',
        actionText: '登录'
    });
    if (!canContinue) return;
    editDraft.value = localComment.value.content;
    editFeedback.value = '';
    editingComment.value = true;
}

function cancelEditComment() {
    editingComment.value = false;
    editDraft.value = '';
    editFeedback.value = '';
}

async function submitEditComment(options = {}) {
    const content = editDraft.value.trim();
    if (!content) {
        editFeedback.value = '评论内容不能为空';
        return;
    }
    if (options.skipSensitiveWarning !== true) {
        editSubmitting.value = true;
        const waitingConfirm = await confirmSensitiveComment(content, '保存修改', editFeedback, async () => {
            await submitEditComment({ skipSensitiveWarning: true });
        });
        editSubmitting.value = false;
        if (waitingConfirm) {
            return;
        }
    }
    editSubmitting.value = true;
    try {
        const updated = await editCommentApi(localComment.value.id, content);
        localComment.value.content = updated.content;
        localComment.value.editedAt = updated.editedAt;
        localComment.value.editCount = updated.editCount;
        localComment.value.canEdit = updated.canEdit;
        editingComment.value = false;
        editDraft.value = '';
        editFeedback.value = '';
    } catch (error) {
        editFeedback.value = error.message || '编辑失败';
    } finally {
        editSubmitting.value = false;
    }
}

function goReplyPage(step) {
    const nextPage = replyPage.value + step;
    if (nextPage < 1) {
        return;
    }
    const totalPages = Math.max(1, Math.ceil(replyTotal.value / replyPageSize));
    if (nextPage > totalPages) {
        return;
    }
    fetchReplies(nextPage);
}
</script>

<template>
    <article class="comment-root-item" data-testid="comment-root-item">
        <UserHoverCard
            :user="localComment.user"
            variant="avatar"
            avatar-class="comment-root-avatar"
        />
        <div class="comment-root-main">
            <header class="comment-root-header">
                <div class="comment-root-author-line">
                    <UserHoverCard
                        :user="localComment.user"
                        name-class="comment-author-name"
                    />
                    <span v-if="localComment.author" class="comment-badge author">作者</span>
                    <span v-if="localComment.pinned" class="comment-badge pinned">置顶</span>
                    <span class="comment-root-time">{{ localComment.time }}</span>
                </div>
            </header>

            <template v-if="editingComment">
                <div class="comment-edit-area">
                    <textarea
                        v-model="editDraft"
                        class="comment-edit-textarea"
                        :disabled="editSubmitting"
                        rows="3"
                        maxlength="2000"
                        placeholder="编辑你的评论..."
                    ></textarea>
                    <div v-if="editFeedback" class="comment-edit-feedback">{{ editFeedback }}</div>
                    <div class="comment-edit-actions">
                        <button type="button" class="btn-cancel" :disabled="editSubmitting" @click="cancelEditComment">取消</button>
                        <button type="button" class="btn-submit" :disabled="editSubmitting || !editDraft.trim()" @click="submitEditComment">
                            {{ editSubmitting ? '保存中...' : '保存修改' }}
                        </button>
                    </div>
                </div>
            </template>
            <template v-else>
                <div class="comment-root-content">
                    {{ localComment.content }}
                    <span v-if="localComment.editCount > 0" class="comment-edited-mark">（已编辑）</span>
                </div>
            </template>

            <div class="comment-root-actions">
                <button type="button" :class="{ active: localComment.liked }" data-testid="comment-like-button" @click="toggleLike(localComment)">
                    {{ localComment.liked ? '已赞' : '点赞' }} {{ localComment.likeCount }}
                </button>
                <button type="button" data-testid="comment-reply-button" @click="startReply(localComment)">回复</button>
                <button v-if="localComment.canEdit" type="button" data-testid="comment-edit-button" @click="startEditComment">
                    编辑
                </button>
                <button v-if="localComment.canPin" type="button" data-testid="comment-pin-button" @click="togglePin">
                    {{ localComment.pinned ? '取消置顶' : '置顶' }}
                </button>
                <button v-if="localComment.canDelete" type="button" class="danger" data-testid="comment-delete-button" @click="removeComment(localComment)">
                    删除
                </button>
                <button type="button" data-testid="comment-report-button" @click="openReport(localComment)">
                    举报
                </button>
            </div>

            <section v-if="displayedReplyCount > 0 || activeReplyTarget" class="comment-reply-shell">
                <div v-if="visibleReplies.length" class="comment-reply-items">
                    <article v-for="reply in visibleReplies" :key="reply.id" class="comment-reply-item">
                        <UserHoverCard
                            :user="reply.user"
                            variant="avatar"
                            avatar-class="comment-reply-avatar"
                        />
                        <div class="comment-reply-main">
                            <div class="comment-reply-author-line">
                                <UserHoverCard
                                    :user="reply.user"
                                    name-class="comment-author-name"
                                />
                                <span v-if="reply.author" class="comment-badge author">作者</span>
                                <span class="comment-root-time">{{ reply.time }}</span>
                            </div>
                            <p class="comment-reply-content">
                                <template v-if="reply.replyToUser">
                                    <UserHoverCard
                                        :user="reply.replyToUser"
                                        trigger-class="reply-target-trigger"
                                        name-class="reply-target"
                                        name-prefix="@"
                                    />
                                </template>
                                {{ reply.content }}
                            </p>
                            <div class="comment-root-actions reply-actions">
                                <button type="button" :class="{ active: reply.liked }" data-testid="reply-like-button" @click="toggleLike(reply)">
                                    {{ reply.liked ? '已赞' : '点赞' }} {{ reply.likeCount }}
                                </button>
                                <button type="button" data-testid="reply-reply-button" @click="startReply(reply)">回复</button>
                                <button v-if="reply.canDelete" type="button" class="danger" data-testid="reply-delete-button" @click="removeComment(reply)">
                                    删除
                                </button>
                                <button type="button" data-testid="reply-report-button" @click="openReport(reply)">
                                    举报
                                </button>
                            </div>
                        </div>
                    </article>
                </div>

                <div class="comment-reply-summary">
                    <button
                        v-if="!expandedReplies && localComment.replyCount > (localComment.replyPreview?.length || 0)"
                        type="button"
                        class="comment-reply-toggle"
                        @click="expandReplies(1)"
                    >
                        展开 {{ localComment.replyCount }} 条回复
                    </button>
                    <button
                        v-else-if="!expandedReplies && localComment.replyCount > 0"
                        type="button"
                        class="comment-reply-toggle"
                        @click="expandReplies(1)"
                    >
                        查看全部回复
                    </button>
                    <button
                        v-if="expandedReplies"
                        type="button"
                        class="comment-reply-toggle"
                        @click="expandedReplies = false"
                    >
                        收起回复
                    </button>
                    <span v-if="repliesLoading" class="comment-reply-loading">楼中楼加载中...</span>
                </div>

                <div v-if="expandedReplies && replyTotal > replyPageSize" class="comment-reply-pagination">
                    <button type="button" :disabled="replyPage <= 1 || repliesLoading" @click="goReplyPage(-1)">上一页</button>
                    <span>{{ replyPage }} / {{ Math.max(1, Math.ceil(replyTotal / replyPageSize)) }}</span>
                    <button
                        type="button"
                        :disabled="replyPage >= Math.ceil(replyTotal / replyPageSize) || repliesLoading"
                        @click="goReplyPage(1)"
                    >
                        下一页
                    </button>
                </div>

                <CommentComposer
                    v-if="activeReplyTarget"
                    v-model="replyDraft"
                    class="comment-inline-composer"
                    :avatar-url="avatarUrl"
                    :compact="true"
                    :submitting="replySubmitting"
                    :show-cancel="true"
                    :feedback="replyFeedback"
                    :placeholder="`回复 ${activeReplyTarget.user.name}`"
                    submit-text="发送回复"
                    @submit="submitReply"
                    @cancel="cancelReply"
                />
            </section>
        </div>
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
        <ReportDialog
            v-if="reportTarget"
            :visible="Boolean(reportTarget)"
            target-type="COMMENT"
            :target-id="reportTarget.id"
            :target-title="reportTarget.title"
            @close="closeReportDialog"
            @success="handleReportSuccess"
        />
    </article>
</template>

<style scoped>
.comment-root-item {
    display: grid;
    grid-template-columns: 40px minmax(0, 1fr);
    gap: 12px;
    align-items: start;
    overflow: visible;
}

:deep(.comment-root-avatar),
:deep(.comment-reply-avatar) {
    border-radius: 50%;
    object-fit: cover;
    background: var(--surface-soft);
}

:deep(.comment-root-avatar) {
    width: 40px;
    height: 40px;
}

.comment-root-main {
    min-width: 0;
    padding-bottom: 18px;
    border-bottom: 1px solid rgba(219, 227, 223, 0.84);
    overflow: visible;
}

.comment-root-header,
.comment-reply-author-line {
    margin-bottom: 6px;
}

.comment-root-author-line,
.comment-reply-author-line {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;
}

.comment-root-author-line strong,
.comment-reply-author-line strong,
:deep(.comment-author-name) {
    font-size: 15px;
    color: var(--text);
}

:deep(.comment-author-name) {
    font-weight: 700;
}

.comment-root-time {
    color: var(--muted);
    font-size: 12px;
}

.comment-badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-height: 22px;
    padding: 0 8px;
    font-size: 12px;
    font-weight: 700;
border-radius: var(--radius-sm);
}

.comment-badge.author {
color: var(--brand-strong);
    background: var(--brand-soft);
}

.comment-badge.pinned {
    color: #9a6700;
    background: rgba(154, 103, 0, 0.12);
}

.comment-root-content,
.comment-reply-content {
    margin: 0;
    color: var(--text);
    font-size: 14px;
    line-height: 1.8;
    overflow-wrap: anywhere;
}

.comment-root-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 14px;
    align-items: center;
    margin-top: 10px;
}

.comment-root-actions button {
    padding: 0;
    color: var(--muted);
    font-size: 13px;
    cursor: pointer;
    background: transparent;
    border: 0;
}

.comment-root-actions button:hover,
.comment-root-actions button.active {
    color: var(--brand-strong);
}

.comment-root-actions .danger:hover {
    color: #b42318;
}

.comment-reply-shell {
    margin-top: 14px;
    padding: 14px;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
    overflow: visible;
}

.comment-reply-items {
    display: grid;
    gap: 14px;
    overflow: visible;
}

.comment-reply-item {
    display: grid;
    grid-template-columns: 32px minmax(0, 1fr);
    gap: 10px;
    overflow: visible;
}

:deep(.comment-reply-avatar) {
    width: 32px;
    height: 32px;
}

:deep(.reply-target-trigger) {
    margin-right: 6px;
}

:deep(.reply-target) {
    margin-right: 6px;
    color: var(--brand-strong);
}

.reply-actions {
    margin-top: 8px;
}

.comment-reply-summary,
.comment-reply-pagination {
    display: flex;
    gap: 10px;
    align-items: center;
    margin-top: 12px;
}

.comment-reply-toggle,
.comment-reply-pagination button {
    min-height: 30px;
    padding: 0 10px;
    color: var(--brand-strong);
    font-size: 13px;
    cursor: pointer;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
}

.comment-reply-loading,
.comment-reply-pagination span {
    color: var(--muted);
    font-size: 13px;
}

.comment-inline-composer {
    margin-top: 14px;
}

.comment-edit-area {
    display: grid;
    gap: 8px;
    margin: 8px 0 2px;
}

.comment-edit-textarea {
    width: 100%;
    padding: 10px 12px;
    font-size: 14px;
    font-family: inherit;
    line-height: 1.7;
    color: var(--text);
    background: var(--surface);
    border: 1px solid var(--brand-hover);
    border-radius: var(--radius-md);
    resize: vertical;
    box-sizing: border-box;
    outline: none;
    transition: border-color 0.15s;
}

.comment-edit-textarea:focus {
    border-color: var(--brand-strong);
}

.comment-edit-feedback {
    font-size: 12px;
    color: #b42318;
}

.comment-edit-actions {
    display: flex;
    gap: 8px;
    justify-content: flex-end;
}

.comment-edit-actions .btn-cancel,
.comment-edit-actions .btn-submit {
    min-height: 32px;
    padding: 0 14px;
    font-size: 13px;
    font-weight: 600;
    border-radius: var(--radius-sm);
    cursor: pointer;
    transition: background 0.15s, border-color 0.15s;
}

.comment-edit-actions .btn-cancel {
    color: var(--muted);
    background: var(--surface);
    border: 1px solid var(--line);
}

.comment-edit-actions .btn-cancel:hover {
    border-color: var(--muted);
}

.comment-edit-actions .btn-submit {
    color: #fff;
    background: var(--brand-strong);
    border: 1px solid var(--brand-strong);
}

.comment-edit-actions .btn-submit:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

.comment-edited-mark {
    margin-left: 4px;
    color: var(--muted);
    font-size: 12px;
}

@media (max-width: 640px) {
    .comment-root-item {
        grid-template-columns: 32px minmax(0, 1fr);
    }

    :deep(.comment-root-avatar) {
        width: 32px;
        height: 32px;
    }

    .comment-reply-summary,
    .comment-reply-pagination {
        align-items: stretch;
        flex-direction: column;
    }
}
</style>
