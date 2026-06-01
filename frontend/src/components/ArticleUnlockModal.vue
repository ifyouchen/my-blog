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

<style scoped src="@/styles/components/ArticleUnlockModal.css"></style>

