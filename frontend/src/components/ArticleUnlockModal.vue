<template>
    <Teleport to="body">
        <div v-if="visible" class="unlock-overlay" @click.self="close">
            <div class="unlock-card" role="dialog" aria-modal="true" aria-labelledby="unlock-title">
                <!-- 关闭按钮 -->
                <button type="button" class="unlock-close" aria-label="关闭解锁弹窗"
                        :disabled="unlocking" @click="close">×</button>

                <!-- 头部 -->
                <div class="unlock-header">
                    <div class="unlock-icon-wrap">
                        <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <rect x="3" y="11" width="18" height="11" rx="2"
                                  stroke="currentColor" stroke-width="2"/>
                            <path d="M7 11V7a5 5 0 0 1 10 0v4"
                                  stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                        </svg>
                    </div>
                    <h3 id="unlock-title">解锁付费内容</h3>
                    <p class="unlock-subtitle">该文章为付费内容，解锁后可永久阅读</p>
                </div>

                <!-- 文章信息 -->
                <div class="unlock-article-info">
                    <span class="unlock-label">文章标题</span>
                    <span class="unlock-value">{{ articleTitle || `文章 #${articleId}` }}</span>
                </div>

                <!-- 费用信息 -->
                <div class="unlock-fee-row">
                    <div class="unlock-fee-item">
                        <span class="unlock-label">解锁费用</span>
                        <span class="unlock-price">{{ pointPrice }} 积分</span>
                    </div>
                    <div class="unlock-fee-item">
                        <span class="unlock-label">当前余额</span>
                        <span class="unlock-balance"
                              :class="{ 'is-insufficient': currentBalance < pointPrice }">
                            {{ currentBalance }} 积分
                        </span>
                    </div>
                </div>

                <!-- 余额不足提示 -->
                <div v-if="currentBalance < pointPrice" class="unlock-insufficient">
                    ⚠ 积分余额不足，还差 {{ pointPrice - currentBalance }} 积分
                </div>

                <!-- 操作按钮 -->
                <div class="unlock-actions">
                    <button type="button" class="unlock-btn secondary"
                            :disabled="unlocking" @click="close">取消</button>
                    <button type="button" class="unlock-btn primary"
                            :disabled="unlocking || currentBalance < pointPrice"
                            @click="handleUnlock">
                        {{ unlocking ? '解锁中...' : `花费 ${pointPrice} 积分解锁` }}
                    </button>
                </div>
            </div>
        </div>
    </Teleport>
</template>

<script setup>

const props = defineProps({
    /** 弹窗是否可见 */
    visible: {
        type: Boolean,
        default: false
    },
    /** 文章 ID */
    articleId: {
        type: [Number, String],
        required: true
    },
    /** 文章标题 */
    articleTitle: {
        type: String,
        default: ''
    },
    /** 解锁所需积分数 */
    pointPrice: {
        type: Number,
        default: 0
    },
    /** 当前用户积分余额 */
    currentBalance: {
        type: Number,
        default: 0
    },
    /** 解锁操作是否进行中 */
    unlocking: {
        type: Boolean,
        default: false
    }
});

const emit = defineEmits(['close', 'confirm']);

const close = () => {
    if (props.unlocking) return;
    emit('close');
};

const handleUnlock = () => {
    if (props.unlocking || props.currentBalance < props.pointPrice) return;
    emit('confirm');
};
</script>

<style scoped>
.unlock-overlay {
    position: fixed;
    inset: 0;
    z-index: 1200;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 18px;
    background: rgba(15, 23, 42, 0.5);
    backdrop-filter: blur(2px);
}

.unlock-card {
    position: relative;
    width: min(420px, 100%);
    padding: 32px 28px 24px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    box-shadow: var(--shadow-md);
}

.unlock-close {
    position: absolute;
    top: 12px;
    right: 12px;
    width: 28px;
    height: 28px;
    font-size: 20px;
    line-height: 1;
    color: var(--muted);
    background: none;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
}

.unlock-close:hover:not(:disabled) {
    color: var(--text);
    background: var(--surface-soft);
}

.unlock-close:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

.unlock-header {
    text-align: center;
    margin-bottom: 24px;
}

.unlock-icon-wrap {
    width: 52px;
    height: 52px;
    margin: 0 auto 12px;
    border-radius: 50%;
    background: rgba(251, 146, 60, 0.12);
    color: #ea580c;
    display: flex;
    align-items: center;
    justify-content: center;
}

.unlock-icon-wrap svg {
    width: 26px;
    height: 26px;
}

.unlock-header h3 {
    margin: 0 0 6px;
    font-size: 20px;
    font-weight: 700;
    color: var(--text);
}

.unlock-subtitle {
    margin: 0;
    font-size: 13px;
    color: var(--muted);
}

.unlock-article-info {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    padding: 12px;
    background: var(--surface-soft);
    border-radius: var(--radius-sm);
    margin-bottom: 16px;
}

.unlock-fee-row {
    display: flex;
    gap: 12px;
    margin-bottom: 12px;
}

.unlock-fee-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 4px;
    padding: 12px;
    background: var(--surface-soft);
    border-radius: var(--radius-sm);
}

.unlock-label {
    font-size: 12px;
    color: var(--muted);
}

.unlock-value {
    font-size: 14px;
    color: var(--text);
    font-weight: 500;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.unlock-price {
    font-size: 18px;
    font-weight: 700;
    color: #ea580c;
}

.unlock-balance {
    font-size: 18px;
    font-weight: 700;
    color: var(--text);
}

.unlock-balance.is-insufficient {
    color: #dc2626;
}

.unlock-insufficient {
    padding: 8px 12px;
    background: rgba(220, 38, 38, 0.08);
    border: 1px solid rgba(220, 38, 38, 0.2);
    border-radius: var(--radius-sm);
    font-size: 13px;
    color: #dc2626;
    margin-bottom: 16px;
}

.unlock-actions {
    display: flex;
    gap: 12px;
    margin-top: 20px;
}

.unlock-btn {
    flex: 1;
    height: 40px;
    font-size: 14px;
    font-weight: 600;
    border-radius: var(--radius-sm);
    cursor: pointer;
    transition: background 0.12s, border-color 0.12s, color 0.12s;
    border: 1px solid var(--brand);
}

.unlock-btn.primary {
    background: var(--brand);
    color: #fff;
}

.unlock-btn.primary:hover:not(:disabled) {
    background: var(--brand-strong);
    border-color: var(--brand-strong);
}

.unlock-btn.secondary {
    background: var(--surface);
    color: var(--text);
    border-color: var(--line);
}

.unlock-btn.secondary:hover:not(:disabled) {
    background: var(--surface-soft);
    border-color: var(--line-strong);
}

.unlock-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

@media (max-width: 480px) {
    .unlock-card {
        padding: 28px 18px 20px;
    }

    .unlock-actions {
        flex-direction: column-reverse;
    }
}
</style>

