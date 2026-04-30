<script setup>
/**
 * AdBanner — 广告位展示组件。
 *
 * 功能：
 * 1. 挂载时调用 /api/ads?slot=xxx 获取当前生效广告列表。
 * 2. 广告进入可视区后触发曝光上报（每个广告只上报一次）。
 * 3. 点击广告先上报点击，再用 window.open 打开目标链接。
 * 4. 广告必须显示"广告"或自定义标签。
 * 5. 目标链接始终在新窗口打开并携带 rel="noopener noreferrer"。
 */
import {onBeforeUnmount, onMounted, ref} from 'vue';
import {getAdsApi, recordAdClickApi, recordAdImpressionApi} from '@/api/ads';

const props = defineProps({
    /** 广告位编码，如 home_sidebar / article_sidebar */
    slot: {
        type: String,
        required: true
    }
});

const ads = ref([]);
const impressionReported = ref(new Set());
let intersectionObserver = null;

const fetchAds = async () => {
    try {
        const result = await getAdsApi(props.slot);
        ads.value = Array.isArray(result) ? result : [];
    } catch (e) {
        ads.value = [];
    }
};

const handleClick = async (ad) => {
    try {
        await recordAdClickApi(ad.id);
    } catch (e) {
        // 点击上报失败不阻断跳转
    }
    window.open(ad.targetUrl, '_blank', 'noopener,noreferrer');
};

const setupObserver = () => {
    if (!('IntersectionObserver' in window)) {
        return;
    }
    intersectionObserver = new IntersectionObserver(
        (entries) => {
            entries.forEach((entry) => {
                if (entry.isIntersecting) {
                    const id = entry.target.dataset.adId;
                    if (id && !impressionReported.value.has(id)) {
                        impressionReported.value.add(id);
                        recordAdImpressionApi(Number(id)).catch(() => {});
                        intersectionObserver.unobserve(entry.target);
                    }
                }
            });
        },
        { threshold: 0.5 }
    );
};

const observeAds = () => {
    if (!intersectionObserver) {
        return;
    }
    document.querySelectorAll(`[data-ad-slot="${props.slot}"] [data-ad-id]`).forEach((el) => {
        intersectionObserver.observe(el);
    });
};

onMounted(async () => {
    setupObserver();
    await fetchAds();
    setTimeout(observeAds, 100);
});

onBeforeUnmount(() => {
    if (intersectionObserver) {
        intersectionObserver.disconnect();
    }
});
</script>

<template>
    <div v-if="ads.length" class="ad-banner-list" :data-ad-slot="slot">
        <div
            v-for="ad in ads"
            :key="ad.id"
            class="ad-banner-item"
            :data-ad-id="String(ad.id)"
        >
            <span class="ad-label">{{ ad.label || '广告' }}</span>
            <button
                type="button"
                class="ad-banner-body"
                :title="`广告：${ad.title}`"
                @click="handleClick(ad)"
            >
                <img
                    v-if="ad.imageUrl"
                    class="ad-banner-image"
                    :src="ad.imageUrl"
                    :alt="ad.title"
                    loading="lazy"
                >
                <span class="ad-banner-title">{{ ad.title }}</span>
            </button>
        </div>
    </div>
</template>

<style scoped>
.ad-banner-list {
    display: grid;
    gap: 10px;
}

.ad-banner-item {
    position: relative;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    background: var(--surface);
    overflow: hidden;
}

.ad-label {
    position: absolute;
    top: 6px;
    right: 8px;
    z-index: 1;
    padding: 2px 6px;
    color: var(--muted);
    font-size: 11px;
    font-weight: 700;
    background: rgba(0, 0, 0, 0.06);
    border-radius: 4px;
    pointer-events: none;
    user-select: none;
}

.ad-banner-body {
    display: grid;
    gap: 8px;
    width: 100%;
    padding: 12px;
    color: var(--text);
    text-align: left;
    cursor: pointer;
    background: transparent;
    border: none;
    transition: background 0.12s;
}

.ad-banner-body:hover {
    background: var(--surface-soft);
}

.ad-banner-body:focus-visible {
    outline: 2px solid var(--brand);
    outline-offset: -2px;
}

.ad-banner-image {
    width: 100%;
    height: 80px;
    object-fit: cover;
    border-radius: 4px;
    display: block;
}

.ad-banner-title {
    font-size: 13px;
    font-weight: 600;
    line-height: 1.5;
    color: var(--text);
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
}
</style>

