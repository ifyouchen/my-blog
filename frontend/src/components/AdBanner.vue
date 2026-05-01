<script setup>
import {computed, onBeforeUnmount, onMounted, ref} from 'vue';
import {dismissAdApi, getAdsApi, getDismissedAdIdsApi, recordAdClickApi, recordAdImpressionApi} from '@/api/ads';

const props = defineProps({
    slotCode: { type: String, required: true }
});

const ads = ref([]);
const impressionReported = ref(new Set());
const dismissedIds = ref(new Set());
const serverDismissedIds = ref(new Set());
const activePopoverAdId = ref(null);
let intersectionObserver = null;
let closeHoverTimer = null;
const POPOVER_HOVER_DELAY = 200;

const today = new Date();
const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;

const getDismissKey = (id) => `ad_dismissed_${id}_${todayStr}`;

const isDismissedPermanently = (id) => {
    try { return localStorage.getItem(getDismissKey(id)) === '1'; } catch (e) { return false; }
};

const visibleAds = computed(() => {
    return ads.value.filter(ad => (
        !dismissedIds.value.has(ad.id)
        && !serverDismissedIds.value.has(ad.id)
        && !isDismissedPermanently(ad.id)
    ));
});

const dismissSession = (id) => {
    dismissedIds.value.add(id);
    activePopoverAdId.value = null;
    dismissAdApi(id).catch(() => {});
};

const dismissToday = (id) => {
    try { localStorage.setItem(getDismissKey(id), '1'); } catch (e) {}
    dismissedIds.value.add(id);
    activePopoverAdId.value = null;
    dismissAdApi(id).catch(() => {});
};

const openPopover = (id) => {
    if (closeHoverTimer) clearTimeout(closeHoverTimer);
    activePopoverAdId.value = id;
};

const closePopover = () => {
    if (closeHoverTimer) clearTimeout(closeHoverTimer);
    closeHoverTimer = window.setTimeout(() => {
        activePopoverAdId.value = null;
    }, POPOVER_HOVER_DELAY);
};

const cancelClosePopover = () => {
    if (closeHoverTimer) clearTimeout(closeHoverTimer);
};

const fetchAds = async () => {
    try {
        const result = await getAdsApi(props.slotCode);
        ads.value = Array.isArray(result) ? result : [];
    } catch (e) {
        ads.value = [];
    }
};

const handleClick = async (ad) => {
    try { await recordAdClickApi(ad.id); } catch (e) {}
    window.open(ad.targetUrl, '_blank', 'noopener,noreferrer');
};

const setupObserver = () => {
    if (!('IntersectionObserver' in window)) return;
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
    if (!intersectionObserver) return;
    document.querySelectorAll(`[data-ad-slot="${props.slotCode}"] [data-ad-id]`).forEach((el) => {
        intersectionObserver.observe(el);
    });
};

const fetchDismissedIds = async () => {
    try {
        const result = await getDismissedAdIdsApi();
        if (result?.ids?.length) {
            serverDismissedIds.value = new Set(result.ids);
        }
    } catch (e) {
        // ignore
    }
};

onMounted(async () => {
    setupObserver();
    await Promise.all([fetchAds(), fetchDismissedIds()]);
    setTimeout(observeAds, 100);
});

onBeforeUnmount(() => {
    if (intersectionObserver) intersectionObserver.disconnect();
});
</script>

<template>
    <div v-if="visibleAds.length" class="ad-banner-list" :data-ad-slot="slotCode">
        <div
            v-for="ad in visibleAds"
            :key="ad.id"
            class="ad-banner-item"
            :data-ad-id="String(ad.id)"
        >
            <button
                type="button"
                class="ad-banner-body"
                :title="`广告：${ad.title}`"
                @click="handleClick(ad)"
            >
                <div class="ad-image-wrap">
                    <img
                        v-if="ad.imageUrl"
                        class="ad-banner-image"
                        :src="ad.imageUrl"
                        :alt="ad.title"
                        loading="lazy"
                        decoding="async"
                    >
                    <span class="ad-label-tag">广告</span>
                </div>
                <span class="ad-banner-title">{{ ad.title }}</span>
            </button>

            <button
                type="button"
                class="ad-close-btn"
                aria-label="关闭广告"
                @click="openPopover(ad.id)"
                @mouseenter="cancelClosePopover"
            >
                <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                    <path d="M3 3l8 8M11 3l-8 8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                </svg>
            </button>

            <div
                v-if="activePopoverAdId === ad.id"
                class="ad-dismiss-popover"
                @mouseenter="cancelClosePopover"
                @mouseleave="closePopover"
            >
                <button type="button" class="dismiss-option" @click="dismissSession(ad.id)">
                    关闭
                </button>
                <button type="button" class="dismiss-option" @click="dismissToday(ad.id)">
                    今天内不再提示
                </button>
            </div>
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
    overflow: visible;
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

.ad-image-wrap {
    position: relative;
    overflow: hidden;
    border-radius: 4px;
}

.ad-banner-image {
    width: 100%;
    height: 80px;
    object-fit: cover;
    display: block;
}

.ad-label-tag {
    position: absolute;
    top: 6px;
    left: 6px;
    z-index: 1;
    padding: 2px 7px;
    font-size: 10px;
    font-weight: 700;
    color: #fff;
    background: rgba(0, 0, 0, 0.55);
    border-radius: 4px;
    pointer-events: none;
    user-select: none;
    line-height: 1.4;
    backdrop-filter: blur(2px);
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

.ad-close-btn {
    position: absolute;
    top: 6px;
    right: 8px;
    z-index: 2;
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--muted);
    background: rgba(0, 0, 0, 0.06);
    border: none;
    border-radius: 4px;
    cursor: pointer;
    opacity: 0;
    transition: opacity 0.15s, background 0.15s, color 0.15s;
}

.ad-banner-item:hover .ad-close-btn {
    opacity: 1;
}

.ad-close-btn:hover {
    color: var(--text);
    background: rgba(0, 0, 0, 0.12);
}

.ad-dismiss-popover {
    position: absolute;
    top: 34px;
    right: 4px;
    z-index: 10;
    display: flex;
    flex-direction: column;
    min-width: 130px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
    overflow: hidden;
}

.dismiss-option {
    padding: 8px 14px;
    font-size: 12px;
    font-weight: 500;
    color: var(--text);
    text-align: left;
    background: transparent;
    border: none;
    cursor: pointer;
    transition: background 0.1s;
}

.dismiss-option:hover {
    background: var(--surface-soft);
}

.dismiss-option + .dismiss-option {
    border-top: 1px solid var(--line);
}
</style>
