<template>
    <!-- 仅当文章需要解锁时显示 -->
    <span v-if="needUnlock" class="lock-badge" :class="{ 'is-unlocked': unlocked }">
        <svg v-if="!unlocked" class="lock-icon" viewBox="0 0 24 24" fill="none"
             xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
            <rect x="3" y="11" width="18" height="11" rx="2" stroke="currentColor" stroke-width="2"/>
            <path d="M7 11V7a5 5 0 0 1 10 0v4" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        </svg>
        <svg v-else class="lock-icon" viewBox="0 0 24 24" fill="none"
             xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
            <rect x="3" y="11" width="18" height="11" rx="2" stroke="currentColor" stroke-width="2"/>
            <path d="M7 11V7a5 5 0 0 1 9.9-1" stroke="currentColor" stroke-width="2"
                  stroke-linecap="round"/>
        </svg>
        <span v-if="!unlocked && pointPrice > 0" class="lock-price">{{ pointPrice }}积分</span>
        <span v-if="unlocked" class="lock-unlocked-text">已解锁</span>
    </span>
</template>

<script setup>
defineProps({
    /** 是否需要解锁 */
    needUnlock: {
        type: Boolean,
        default: false
    },
    /** 当前用户是否已解锁 */
    unlocked: {
        type: Boolean,
        default: false
    },
    /** 解锁所需积分数 */
    pointPrice: {
        type: Number,
        default: 0
    }
});
</script>

<style scoped src="@/styles/components/ArticleLockBadge.css"></style>

