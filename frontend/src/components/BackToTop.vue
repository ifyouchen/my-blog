<script setup>
import { ref, onMounted, onUnmounted } from 'vue';

const visible = ref(false);
const SCROLL_THRESHOLD = 300;

const handleScroll = () => {
    visible.value = window.scrollY > SCROLL_THRESHOLD;
};

const scrollToTop = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
};

onMounted(() => {
    window.addEventListener('scroll', handleScroll, { passive: true });
});

onUnmounted(() => {
    window.removeEventListener('scroll', handleScroll);
});
</script>

<template>
    <Transition name="fade">
        <button
            v-if="visible"
            class="back-to-top"
            title="回到顶部"
            @click="scrollToTop"
        >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="18 15 12 9 6 15" />
            </svg>
        </button>
    </Transition>
</template>

<style scoped>
.back-to-top {
    position: fixed;
    right: 24px;
    bottom: 40px;
    width: 44px;
    height: 44px;
    border-radius: 50%;
    border: none;
    background: #fff;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 999;
    transition: background 0.2s, box-shadow 0.2s;
}

.back-to-top:hover {
    background: #f0f0f0;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}

.back-to-top svg {
    width: 22px;
    height: 22px;
    color: #555;
}

.fade-enter-active,
.fade-leave-active {
    transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
    opacity: 0;
}
</style>
