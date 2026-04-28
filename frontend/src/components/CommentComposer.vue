<script setup>
import { computed } from 'vue';

const props = defineProps({
    modelValue: {
        type: String,
        default: ''
    },
    avatarUrl: {
        type: String,
        default: ''
    },
    placeholder: {
        type: String,
        default: '说点什么吧'
    },
    submitText: {
        type: String,
        default: '发表评论'
    },
    feedback: {
        type: String,
        default: ''
    },
    compact: {
        type: Boolean,
        default: false
    },
    submitting: {
        type: Boolean,
        default: false
    },
    showCancel: {
        type: Boolean,
        default: false
    }
});

const emit = defineEmits(['update:modelValue', 'submit', 'cancel']);

const avatarSrc = computed(() => {
    return props.avatarUrl || 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=96&q=80';
});
</script>

<template>
    <div :class="['comment-composer', { compact }]" data-testid="comment-composer">
        <img class="comment-composer-avatar" :src="avatarSrc" alt="用户头像">
        <div class="comment-composer-main">
            <textarea
                :value="modelValue"
                class="comment-composer-input"
                :rows="compact ? 3 : 4"
                :placeholder="placeholder"
                data-testid="comment-composer-input"
                @input="emit('update:modelValue', $event.target.value)"
            />
            <div class="comment-composer-footer">
                <span v-if="feedback" class="comment-composer-feedback">{{ feedback }}</span>
                <div class="comment-composer-actions">
                    <button
                        v-if="showCancel"
                        type="button"
                        class="comment-composer-cancel"
                        data-testid="comment-composer-cancel"
                        @click="emit('cancel')"
                    >
                        取消
                    </button>
                    <button
                        type="button"
                        class="comment-composer-submit"
                        :disabled="submitting"
                        data-testid="comment-composer-submit"
                        @click="emit('submit')"
                    >
                        {{ submitting ? '提交中...' : submitText }}
                    </button>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
.comment-composer {
    display: grid;
    grid-template-columns: 40px minmax(0, 1fr);
    gap: 12px;
    align-items: start;
}

.comment-composer.compact {
    grid-template-columns: 32px minmax(0, 1fr);
}

.comment-composer-avatar {
    width: 40px;
    height: 40px;
    object-fit: cover;
    border-radius: 50%;
    background: var(--surface-soft);
}

.comment-composer.compact .comment-composer-avatar {
    width: 32px;
    height: 32px;
}

.comment-composer-main {
    min-width: 0;
    padding: 12px;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
}

.comment-composer-input {
    width: 100%;
    padding: 0;
    font-size: 14px;
    line-height: 1.7;
    color: var(--text);
    background: transparent;
    border: 0;
    outline: none;
    resize: vertical;
    box-sizing: border-box;
}

.comment-composer-input::placeholder {
    color: var(--muted);
}

.comment-composer-footer {
    display: flex;
    gap: 12px;
    align-items: center;
    justify-content: space-between;
    margin-top: 10px;
    padding-top: 10px;
    border-top: 1px solid var(--line);
}

.comment-composer-feedback {
    min-width: 0;
    color: var(--muted);
    font-size: 13px;
}

.comment-composer-actions {
    display: flex;
    gap: 8px;
    align-items: center;
}

.comment-composer-cancel,
.comment-composer-submit {
    min-height: 34px;
    padding: 0 14px;
    font-size: 13px;
    cursor: pointer;
    border-radius: var(--radius-md);
}

.comment-composer-cancel {
    color: var(--muted);
    background: var(--surface);
    border: 1px solid var(--line);
}

.comment-composer-submit {
    color: #ffffff;
    background: var(--brand);
    border: 1px solid var(--brand);
}

.comment-composer-submit:hover:not(:disabled) {
    background: var(--brand-strong);
    border-color: var(--brand-strong);
}

.comment-composer-submit:disabled {
    opacity: 0.65;
    cursor: not-allowed;
}

@media (max-width: 640px) {
    .comment-composer,
    .comment-composer.compact {
        grid-template-columns: 32px minmax(0, 1fr);
    }

    .comment-composer-avatar {
        width: 32px;
        height: 32px;
    }

    .comment-composer-footer {
        align-items: stretch;
        flex-direction: column;
    }

    .comment-composer-actions {
        justify-content: flex-end;
    }
}
</style>
