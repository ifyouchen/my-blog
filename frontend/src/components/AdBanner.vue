<script setup>
import {computed, onBeforeUnmount, onMounted, ref, watch} from 'vue';
import {dismissAdApi, getAdsApi, getDismissedAdIdsApi, recordAdClickApi, recordAdImpressionApi} from '@/api/ads';
import {resolveMediaUrl} from '@/utils/media';
import {useSession} from '@/stores/session';

const props = defineProps({
    slotCode: { type: String, required: true }
});

const { state, isLoggedIn, initializeSession } = useSession();

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
    if (isLoggedIn.value) {
        dismissAdApi(id).catch(() => {});
    }
};

const dismissToday = (id) => {
    try { localStorage.setItem(getDismissKey(id), '1'); } catch (e) {}
    dismissedIds.value.add(id);
    activePopoverAdId.value = null;
    if (isLoggedIn.value) {
        dismissAdApi(id).catch(() => {});
    }
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
    if (!isLoggedIn.value) {
        serverDismissedIds.value = new Set();
        return;
    }
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
    await initializeSession();
    await Promise.all([fetchAds(), fetchDismissedIds()]);
    setTimeout(observeAds, 100);
});

watch(isLoggedIn, (loggedIn) => {
    if (loggedIn) {
        fetchDismissedIds();
    } else {
        serverDismissedIds.value = new Set();
    }
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
                        :src="resolveMediaUrl(ad.imageUrl)"
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

<style scoped src="@/styles/components/AdBanner.css"></style>
