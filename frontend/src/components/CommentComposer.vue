<script setup>
import {computed, nextTick, ref} from 'vue';

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
    readonly: {
        type: Boolean,
        default: false
    },
    showCancel: {
        type: Boolean,
        default: false
    },
    maxLength: {
        type: Number,
        default: 1000
    }
});

const emit = defineEmits(['update:modelValue', 'submit', 'cancel', 'activate']);

const textareaRef = ref(null);
const avatarSrc = computed(() => {
    return props.avatarUrl || 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=96&q=80';
});
const inputLength = computed(() => props.modelValue?.length || 0);
const counterTone = computed(() => {
    if (inputLength.value >= props.maxLength) {
        return 'danger';
    }
    if (inputLength.value >= Math.floor(props.maxLength * 0.9)) {
        return 'warning';
    }
    return 'normal';
});

const updateValue = (value) => {
    if (!props.readonly) {
        emit('update:modelValue', value);
    }
};

const focus = () => {
    textareaRef.value?.focus();
};

const insertCodeBlock = async () => {
    if (props.readonly) {
        emit('activate');
        return;
    }
    const textarea = textareaRef.value;
    if (!textarea) {
        return;
    }
    const value = props.modelValue || '';
    const start = textarea.selectionStart ?? value.length;
    const end = textarea.selectionEnd ?? start;
    const selected = value.slice(start, end);
    const prefix = start > 0 && value[start - 1] !== '\n' ? '\n\n' : '';
    const suffix = end < value.length && value[end] !== '\n' ? '\n\n' : '';
    const block = selected
        ? `${prefix}\`\`\`text\n${selected}\n\`\`\`${suffix}`
        : `${prefix}\`\`\`text\n\n\`\`\`${suffix}`;
    const nextValue = `${value.slice(0, start)}${block}${value.slice(end)}`;
    emit('update:modelValue', nextValue);
    await nextTick();
    textarea.focus();
    if (!selected) {
        const cursor = start + prefix.length + '```text\n'.length;
        textarea.setSelectionRange(cursor, cursor);
    } else {
        const cursor = start + block.length;
        textarea.setSelectionRange(cursor, cursor);
    }
};

defineExpose({ focus });
</script>

<template>
    <div :class="['comment-composer', { compact }]" data-testid="comment-composer">
        <img class="comment-composer-avatar" :src="avatarSrc" alt="用户头像" loading="lazy" decoding="async">
        <div class="comment-composer-main">
            <textarea
                ref="textareaRef"
                :value="modelValue"
                class="comment-composer-input"
                :rows="compact ? 3 : 4"
                :placeholder="placeholder"
                :readonly="readonly"
                :aria-readonly="readonly"
                :maxlength="maxLength"
                data-testid="comment-composer-input"
                @focus="readonly && emit('activate')"
                @click="readonly && emit('activate')"
                @input="updateValue($event.target.value)"
            />
            <div class="comment-composer-footer">
                <div class="comment-composer-meta">
                    <span v-if="feedback" class="comment-composer-feedback">{{ feedback }}</span>
                    <span
                        :class="['comment-composer-counter', counterTone]"
                        data-testid="comment-composer-counter"
                    >
                        {{ inputLength }} / {{ maxLength }}
                    </span>
                </div>
                <div class="comment-composer-actions">
                    <button
                        type="button"
                        class="comment-composer-tool"
                        title="插入代码块"
                        aria-label="插入代码块"
                        :disabled="submitting"
                        @click="insertCodeBlock"
                    >
                        &lt;/&gt;
                    </button>
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

<style scoped src="@/styles/components/CommentComposer.css"></style>
