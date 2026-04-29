<script setup>
import {computed, ref, watch} from 'vue';
import {createReportApi} from '@/api/reports';

const props = defineProps({
    visible: {
        type: Boolean,
        default: false
    },
    targetType: {
        type: String,
        required: true
    },
    targetId: {
        type: [Number, String],
        required: true
    },
    targetTitle: {
        type: String,
        default: ''
    }
});

const emit = defineEmits(['close', 'success']);

const reasonType = ref('SPAM');
const reasonDetail = ref('');
const submitting = ref(false);
const errorMessage = ref('');

const reasonOptions = [
    { value: 'SPAM', label: '垃圾内容' },
    { value: 'INFRINGEMENT', label: '侵权' },
    { value: 'ABUSE', label: '辱骂骚扰' },
    { value: 'ILLEGAL', label: '违法违规' },
    { value: 'OTHER', label: '其他' }
];

const targetLabel = computed(() => {
    if (props.targetType === 'ARTICLE') {
        return '文章';
    }
    if (props.targetType === 'COMMENT') {
        return '评论';
    }
    return '用户';
});

const close = () => {
    if (submitting.value) {
        return;
    }
    emit('close');
};

const submit = async () => {
    if (submitting.value) {
        return;
    }
    submitting.value = true;
    errorMessage.value = '';
    try {
        const result = await createReportApi({
            targetType: props.targetType,
            targetId: Number(props.targetId),
            reasonType: reasonType.value,
            reasonDetail: reasonDetail.value.trim()
        });
        emit('success', result);
        emit('close');
    } catch (error) {
        errorMessage.value = error.message || '举报提交失败';
    } finally {
        submitting.value = false;
    }
};

watch(() => props.visible, (visible) => {
    if (!visible) {
        reasonType.value = 'SPAM';
        reasonDetail.value = '';
        errorMessage.value = '';
    }
});
</script>

<template>
    <Teleport to="body">
        <div v-if="visible" class="report-dialog-overlay" @click.self="close">
            <form class="report-dialog-card" @submit.prevent="submit">
                <button
                    type="button"
                    class="report-dialog-close"
                    aria-label="关闭举报弹窗"
                    :disabled="submitting"
                    @click="close"
                >
                    ×
                </button>
                <p class="report-dialog-eyebrow">内容举报</p>
                <h3>举报{{ targetLabel }}</h3>
                <p v-if="targetTitle" class="report-dialog-target">{{ targetTitle }}</p>

                <label class="report-field">
                    <span>举报原因</span>
                    <select v-model="reasonType" :disabled="submitting">
                        <option v-for="option in reasonOptions" :key="option.value" :value="option.value">
                            {{ option.label }}
                        </option>
                    </select>
                </label>

                <label class="report-field">
                    <span>补充说明</span>
                    <textarea
                        v-model="reasonDetail"
                        rows="4"
                        maxlength="500"
                        :disabled="submitting"
                        placeholder="可以补充具体情况"
                    ></textarea>
                </label>

                <p v-if="errorMessage" class="report-dialog-error">{{ errorMessage }}</p>

                <div class="report-dialog-actions">
                    <button type="button" :disabled="submitting" @click="close">取消</button>
                    <button type="submit" class="primary" :disabled="submitting">
                        {{ submitting ? '提交中...' : '提交举报' }}
                    </button>
                </div>
            </form>
        </div>
    </Teleport>
</template>

<style scoped>
.report-dialog-overlay {
    position: fixed;
    inset: 0;
    z-index: 1200;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 18px;
    background: rgba(15, 23, 42, 0.42);
}

.report-dialog-card {
    position: relative;
    display: grid;
    gap: 16px;
    width: min(460px, 100%);
    padding: 30px 28px 24px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    box-shadow: var(--shadow-md);
}

.report-dialog-close {
    position: absolute;
    top: 12px;
    right: 12px;
    width: 28px;
    height: 28px;
    color: var(--muted);
    font-size: 20px;
    line-height: 1;
    cursor: pointer;
    background: none;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.report-dialog-eyebrow {
    margin: 0;
    color: var(--brand-strong);
    font-size: 13px;
    font-weight: 800;
}

.report-dialog-card h3 {
    margin: -8px 0 0;
    color: var(--text);
    font-size: 22px;
}

.report-dialog-target {
    margin: 0;
    color: var(--muted);
    font-size: 14px;
    line-height: 1.6;
    overflow-wrap: anywhere;
}

.report-field {
    display: grid;
    gap: 8px;
}

.report-field span {
    color: var(--muted);
    font-size: 13px;
    font-weight: 700;
}

.report-field select,
.report-field textarea {
    width: 100%;
    padding: 10px 12px;
    color: var(--text);
    font: inherit;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.report-field textarea {
    resize: vertical;
}

.report-dialog-error {
    margin: 0;
    color: #b42318;
    font-size: 13px;
}

.report-dialog-actions {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
}

.report-dialog-actions button {
    min-width: 96px;
    min-height: 38px;
    padding: 0 16px;
    color: var(--text);
    font-weight: 600;
    cursor: pointer;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.report-dialog-actions button.primary {
    color: #ffffff;
    background: var(--brand);
    border-color: var(--brand);
}

.report-dialog-actions button:disabled {
    opacity: 0.65;
    cursor: not-allowed;
}

@media (max-width: 560px) {
    .report-dialog-card {
        padding: 28px 20px 20px;
    }

    .report-dialog-actions {
        flex-direction: column-reverse;
    }

    .report-dialog-actions button {
        width: 100%;
    }
}
</style>
