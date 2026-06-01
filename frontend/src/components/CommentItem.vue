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
        <img
            class="comment-thread-avatar"
            :src="avatarUrl(comment)"
            :alt="authorName(comment)"
            loading="lazy"
            decoding="async"
        >
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
                            <img
                                class="comment-thread-avatar"
                                :src="avatarUrl(reply)"
                                :alt="authorName(reply)"
                                loading="lazy"
                                decoding="async"
                            >
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

<style scoped src="@/styles/components/CommentItem.css"></style>
