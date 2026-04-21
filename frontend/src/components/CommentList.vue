<script setup>
import { ref, computed, inject, onMounted, watch } from 'vue';
import { getCommentsApi, createCommentApi, deleteCommentApi } from '@/api/comments';
import { normalizeComment } from '@/api/transformers';
import { useSession } from '@/stores/session';
import CommentItem from '@/components/CommentItem.vue';

const props = defineProps({
    articleId: {
        type: Number,
        required: true
    }
});

const { state } = useSession();
const loginModal = inject('loginModal', { requireLogin: () => false });

const comments = ref([]);
const commentText = ref('');
const replyText = ref('');
const replyToId = ref(null);
const feedback = ref('');
const sortType = ref('time');
const loading = ref(false);

const sortedComments = computed(() => {
    const list = [...comments.value];
    if (sortType.value === 'hot') {
        return list.sort((a, b) => (b.likeCount || 0) - (a.likeCount || 0));
    }
    return list.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
});

const countCommentTree = (items) => {
    return items.reduce((total, item) => {
        return total + 1 + countCommentTree(item.replies || []);
    }, 0);
};

const totalCommentCount = computed(() => countCommentTree(comments.value));

const fetchComments = async () => {
    loading.value = true;
    try {
        const data = await getCommentsApi(props.articleId);
        comments.value = (data || []).map(c => normalizeComment(c));
    } catch (error) {
        console.error('获取评论失败:', error);
    } finally {
        loading.value = false;
    }
};

const submitComment = async () => {
    const canContinue = loginModal.requireLogin(() => submitComment(), {
        title: '登录后发表评论',
        message: '登录后可以参与讨论，并收到后续回复提醒。',
        actionText: '登录并评论'
    });
    if (!canContinue) {
        feedback.value = '登录后可以发表评论';
        return;
    }
    const content = commentText.value.trim();
    if (!content) {
        feedback.value = '评论内容不能为空';
        return;
    }
    try {
        await createCommentApi(props.articleId, { content, parentId: 0 });
        feedback.value = '评论已发布';
        commentText.value = '';
        await fetchComments();
    } catch (error) {
        feedback.value = error.message || '评论失败';
    }
};

const submitReply = async (parentId) => {
    const canContinue = loginModal.requireLogin(() => submitReply(parentId), {
        title: '登录后回复评论',
        message: '登录后可以继续这段讨论，回复会展示在当前评论下。',
        actionText: '登录并回复'
    });
    if (!canContinue) {
        feedback.value = '登录后可以回复评论';
        return;
    }
    const content = replyText.value.trim();
    if (!content) {
        feedback.value = '回复内容不能为空';
        return;
    }
    try {
        await createCommentApi(props.articleId, { content, parentId });
        feedback.value = '回复已发布';
        replyText.value = '';
        replyToId.value = null;
        await fetchComments();
    } catch (error) {
        feedback.value = error.message || '回复失败';
    }
};

const deleteComment = async (commentId) => {
    const canContinue = loginModal.requireLogin(() => deleteComment(commentId), {
        title: '登录后管理评论',
        message: '登录后会校验你的身份，只允许删除自己或有权限管理的评论。',
        actionText: '登录并继续'
    });
    if (!canContinue) {
        feedback.value = '登录后可以管理评论';
        return;
    }
    if (!confirm('确定要删除这条评论吗？')) {
        return;
    }
    try {
        await deleteCommentApi(commentId);
        feedback.value = '评论已删除';
        await fetchComments();
    } catch (error) {
        feedback.value = error.message || '删除失败';
    }
};

const startReply = (commentId) => {
    const canContinue = loginModal.requireLogin(() => startReply(commentId), {
        title: '登录后回复评论',
        message: '登录后可以继续这段讨论，回复会展示在当前评论下。',
        actionText: '登录并回复'
    });
    if (!canContinue) {
        feedback.value = '登录后可以回复评论';
        return;
    }
    replyToId.value = commentId;
    replyText.value = '';
};

const cancelReply = () => {
    replyToId.value = null;
    replyText.value = '';
};

const currentUserId = computed(() => state.user?.id);
const currentUserRole = computed(() => state.user?.role || '');

const formatTime = (timeStr) => {
    if (!timeStr) return '';
    const date = new Date(timeStr);
    const now = new Date();
    const diff = now - date;
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);
    if (minutes < 1) return '刚刚';
    if (minutes < 60) return `${minutes}分钟前`;
    if (hours < 24) return `${hours}小时前`;
    if (days < 30) return `${days}天前`;
    return timeStr.substring(0, 10);
};

defineExpose({ fetchComments });

onMounted(fetchComments);

watch(() => props.articleId, fetchComments);
</script>

<template>
    <div class="comment-panel">
        <div class="comment-header">
            <span class="comment-title">评论 ({{ totalCommentCount }})</span>
            <div class="comment-sort">
                <button
                    :class="{ active: sortType === 'time' }"
                    @click="sortType = 'time'"
                >时间</button>
                <button
                    :class="{ active: sortType === 'hot' }"
                    @click="sortType = 'hot'"
                >热度</button>
            </div>
        </div>

        <div class="comment-input-wrap">
            <textarea
                v-model="commentText"
                class="comment-input"
                placeholder="发一条友善的评论..."
                rows="3"
            ></textarea>
            <div class="comment-input-footer">
                <span v-if="feedback" class="feedback">{{ feedback }}</span>
                <button class="submit-btn" type="button" @click="submitComment">发表评论</button>
            </div>
        </div>

        <p v-if="loading" class="comment-empty">评论加载中...</p>
        <p v-else-if="!sortedComments.length" class="comment-empty">还没有评论，来写下第一条想法。</p>

        <div class="comment-items">
            <CommentItem
                v-for="comment in sortedComments"
                :key="comment.id"
                :comment="comment"
                :current-user-id="currentUserId"
                :current-user-role="currentUserRole"
                :format-time="formatTime"
                @reply="startReply"
                @delete="deleteComment"
            />
        </div>

        <div v-if="replyToId" class="reply-dock">
            <textarea
                v-model="replyText"
                class="reply-input"
                placeholder="写下你的回复..."
                rows="2"
            ></textarea>
            <div class="reply-input-footer">
                <span>正在回复 #{{ replyToId }}</span>
                <div>
                    <button class="cancel-btn" type="button" @click="cancelReply">取消</button>
                    <button class="submit-btn" type="button" @click="submitReply(replyToId)">发布</button>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
.comment-panel {
    padding: 16px 0;
}

.comment-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
}

.comment-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--text);
}

.comment-sort {
    display: flex;
    gap: 8px;
}

.comment-sort button {
    padding: 4px 12px;
    font-size: 13px;
    color: var(--muted);
    background: transparent;
    border: 1px solid var(--line);
    border-radius: 4px;
    cursor: pointer;
}

.comment-sort button.active {
    color: var(--brand);
    border-color: var(--brand);
    background: rgba(15, 143, 117, 0.1);
}

.comment-input-wrap {
    margin-bottom: 24px;
    padding: 12px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: 8px;
}

.comment-input {
    width: 100%;
    padding: 8px;
    font-size: 14px;
    line-height: 1.6;
    color: var(--text);
    background: transparent;
    border: none;
    outline: none;
    resize: none;
    box-sizing: border-box;
}

.comment-input::placeholder {
    color: var(--muted);
}

.comment-input-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 8px;
    padding-top: 8px;
    border-top: 1px solid var(--line);
}

.feedback {
    font-size: 13px;
    color: var(--brand);
}

.submit-btn {
    padding: 6px 16px;
    font-size: 13px;
    font-weight: 500;
    color: #ffffff;
    background: var(--brand);
    border: none;
    border-radius: 4px;
    cursor: pointer;
}

.submit-btn:hover {
    background: var(--brand-strong);
}

.comment-items {
    display: grid;
    gap: 20px;
}

.reply-dock {
    position: sticky;
    bottom: 12px;
    z-index: 2;
    margin-top: 18px;
    padding: 12px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: 8px;
    box-shadow: var(--shadow);
}

.reply-input {
    width: 100%;
    padding: 6px;
    font-size: 13px;
    line-height: 1.5;
    color: var(--text);
    background: transparent;
    border: none;
    outline: none;
    resize: none;
    box-sizing: border-box;
}

.reply-input::placeholder {
    color: var(--muted);
}

.reply-input-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 8px;
    margin-top: 6px;
}

.reply-input-footer span,
.comment-empty {
    color: var(--muted);
    font-size: 13px;
}

.reply-input-footer div {
    display: flex;
    gap: 8px;
}

.cancel-btn {
    padding: 4px 12px;
    font-size: 12px;
    color: var(--muted);
    background: transparent;
    border: 1px solid var(--line);
    border-radius: 4px;
    cursor: pointer;
}

.cancel-btn:hover {
    border-color: var(--muted);
}

@media (max-width: 520px) {
    .comment-header,
    .comment-input-footer,
    .reply-input-footer {
        align-items: stretch;
        flex-direction: column;
    }

    .reply-input-footer div {
        justify-content: flex-end;
    }
}
</style>
