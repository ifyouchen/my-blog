<script setup>
const props = defineProps({
    comment: {
        type: Object,
        required: true
    },
    currentUserId: {
        type: [Number, String],
        default: null
    },
    currentUserRole: {
        type: String,
        default: ''
    },
    depth: {
        type: Number,
        default: 0
    },
    formatTime: {
        type: Function,
        required: true
    }
});

const emit = defineEmits(['reply', 'delete']);

const normalizeId = (value) => (value === null || value === undefined ? '' : String(value));

const canDelete = () => {
    return normalizeId(props.currentUserId) === normalizeId(props.comment.userId)
        || props.currentUserRole === 'ADMIN';
};

const authorName = (comment) => {
    return comment.user?.nickname || comment.user?.username || '匿名用户';
};

const avatarUrl = (comment) => {
    return comment.user?.avatar || comment.user?.avatarUrl;
};
</script>

<template>
    <article class="comment-thread-item" :class="{ nested: depth > 0 }">
        <img class="comment-thread-avatar" :src="avatarUrl(comment)" :alt="authorName(comment)">
        <div class="comment-thread-body">
            <div class="comment-thread-card">
                <div class="comment-thread-meta">
                    <strong>{{ authorName(comment) }}</strong>
                    <span v-if="comment.parentUser" class="reply-target">
                        回复 {{ comment.parentUser.nickname || comment.parentUser.username }}
                    </span>
                </div>
                <p>{{ comment.content }}</p>
                <div class="comment-thread-actions">
                    <span>{{ formatTime(comment.createdAt) }}</span>
                    <button type="button" @click="emit('reply', comment.id)">回复</button>
                    <button
                        v-if="canDelete()"
                        type="button"
                        class="danger"
                        @click="emit('delete', comment.id)"
                    >
                        删除
                    </button>
                </div>
            </div>

            <div v-if="comment.replies && comment.replies.length" class="comment-thread-replies">
                <template v-if="depth < 1">
                    <CommentItem
                        v-for="reply in comment.replies"
                        :key="reply.id"
                        :comment="reply"
                        :current-user-id="currentUserId"
                        :current-user-role="currentUserRole"
                        :depth="depth + 1"
                        :format-time="formatTime"
                        @reply="emit('reply', $event)"
                        @delete="emit('delete', $event)"
                    />
                </template>
                <template v-else>
                    <div
                        v-for="reply in comment.replies"
                        :key="reply.id"
                        class="comment-flatten-item"
                    >
                        <div class="comment-thread-item nested">
                            <img class="comment-thread-avatar" :src="avatarUrl(reply)" :alt="authorName(reply)">
                            <div class="comment-thread-body">
                                <div class="comment-thread-card">
                                    <div class="comment-thread-meta">
                                        <strong>{{ authorName(reply) }}</strong>
                                        <span v-if="reply.parentUser" class="reply-target">
                                            回复 {{ reply.parentUser.nickname || reply.parentUser.username }}
                                        </span>
                                    </div>
                                    <p>{{ reply.content }}</p>
                                    <div class="comment-thread-actions">
                                        <span>{{ formatTime(reply.createdAt) }}</span>
                                        <button type="button" @click="emit('reply', reply.id)">回复</button>
                                        <button
                                            v-if="normalizeId(currentUserId) === normalizeId(reply.userId) || currentUserRole === 'ADMIN'"
                                            type="button"
                                            class="danger"
                                            @click="emit('delete', reply.id)"
                                        >删除</button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </template>
            </div>
        </div>
    </article>
</template>

<style scoped>
.comment-thread-item {
    display: grid;
    grid-template-columns: 40px minmax(0, 1fr);
    gap: 12px;
    align-items: start;
}

.comment-thread-item.nested {
    grid-template-columns: 32px minmax(0, 1fr);
}

.comment-thread-avatar {
    width: 40px;
    height: 40px;
    object-fit: cover;
    background: var(--surface-soft);
    border-radius: 50%;
}

.comment-thread-item.nested .comment-thread-avatar {
    width: 32px;
    height: 32px;
}

.comment-thread-body {
    min-width: 0;
}

.comment-thread-card {
    padding: 12px;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
}

.comment-thread-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;
}

.comment-thread-meta strong {
    font-size: 14px;
}

.reply-target {
    color: var(--muted);
    font-size: 12px;
}

.comment-thread-card p {
    margin: 8px 0;
    color: var(--text);
    line-height: 1.7;
    overflow-wrap: anywhere;
}

.comment-thread-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    align-items: center;
    color: var(--muted);
    font-size: 12px;
}

.comment-thread-actions button {
    padding: 0;
    color: var(--muted);
    cursor: pointer;
    background: transparent;
    border: 0;
}

.comment-thread-actions button:hover {
    color: var(--brand-strong);
}

.comment-thread-actions .danger:hover {
    color: var(--accent);
}

.comment-thread-replies {
    display: grid;
    gap: 12px;
    margin-top: 12px;
    padding-left: 14px;
    border-left: 2px solid var(--line);
}

@media (max-width: 520px) {
    .comment-thread-item,
    .comment-thread-item.nested {
        grid-template-columns: 32px minmax(0, 1fr);
    }

    .comment-thread-avatar {
        width: 32px;
        height: 32px;
    }
}
</style>
