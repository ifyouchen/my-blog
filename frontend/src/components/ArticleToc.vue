<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted, watch } from 'vue';
import { extractToc } from '@/api/transformers';

const props = defineProps({
    content: {
        type: [String, Array],
        default: ''
    },
    targetSelector: {
        type: String,
        default: '.markdown-preview'
    },
    useCustomNavigation: {
        type: Boolean,
        default: false
    },
    refreshOnContentChange: {
        type: Boolean,
        default: false
    },
    mobileVisible: {
        type: Boolean,
        default: false
    }
});

const emit = defineEmits(['navigate']);

const activeId = ref('');
const tocNavRef = ref(null);
const observer = ref(null);
let observerInitToken = 0;

const TOC_COLLAPSE_THRESHOLD = 10;
const TOC_COLLAPSE_LEVEL = 3; // 三级以下（level >= 3）默认折叠

const toc = computed(() => extractToc(props.content));

// 当标题总数超过阈值时，自动初始化为折叠状态
const deepCollapsed = ref(false);

watch(toc, (newToc) => {
    if (newToc.length > TOC_COLLAPSE_THRESHOLD) {
        deepCollapsed.value = true;
    } else {
        deepCollapsed.value = false;
    }
}, { immediate: true });

const visibleToc = computed(() => {
    if (!deepCollapsed.value) return toc.value;
    return toc.value.filter(item => item.level < TOC_COLLAPSE_LEVEL);
});

const hasDeepItems = computed(() => toc.value.some(item => item.level >= TOC_COLLAPSE_LEVEL));

const scrollActiveItemIntoView = () => {
    if (!tocNavRef.value || !activeId.value) {
        return;
    }
    const activeLink = tocNavRef.value.querySelector(`[data-toc-id="${activeId.value}"]`);
    if (activeLink) {
        const container = tocNavRef.value;
        const linkTop = activeLink.offsetTop;
        const linkBottom = linkTop + activeLink.offsetHeight;
        const visibleTop = container.scrollTop;
        const visibleBottom = visibleTop + container.clientHeight;
        const padding = 8;
        if (linkTop - padding < visibleTop) {
            container.scrollTop = Math.max(0, linkTop - padding);
        } else if (linkBottom + padding > visibleBottom) {
            container.scrollTop = Math.max(0, linkBottom - container.clientHeight + padding);
        }
    }
};

const initObserver = () => {
    if (observer.value) {
        observer.value.disconnect();
    }

    const headings = document.querySelectorAll(
        `${props.targetSelector} h1, ${props.targetSelector} h2, ${props.targetSelector} h3, `
        + `${props.targetSelector} h4, ${props.targetSelector} h5`
    );
    if (headings.length === 0) {
        return;
    }

    observer.value = new IntersectionObserver(
        (entries) => {
            const visibleEntries = entries
                .filter((entry) => entry.isIntersecting)
                .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top);
            if (visibleEntries.length > 0) {
                activeId.value = visibleEntries[0].target.id;
                scrollActiveItemIntoView();
            }
        },
        {
            rootMargin: '-80px 0px -70% 0px',
            threshold: 0
        }
    );

    headings.forEach((heading, index) => {
        const tocItem = toc.value[index];
        if (tocItem && heading) {
            heading.id = tocItem.id;
            observer.value.observe(heading);
        }
    });

    if (!activeId.value && toc.value.length > 0) {
        activeId.value = toc.value[0].id;
    }
};

const scheduleObserverRefresh = async () => {
    const currentToken = ++observerInitToken;
    await nextTick();
    if (currentToken !== observerInitToken) {
        return;
    }
    initObserver();
};

const scrollToHeading = (item, index) => {
    const id = item.id;
    activeId.value = id;

    if (props.useCustomNavigation) {
        emit('navigate', { item, index });
        scrollActiveItemIntoView();
        return;
    }

    const element = document.getElementById(id);
    if (element) {
        element.scrollIntoView({ behavior: 'smooth', block: 'start' });
        scrollActiveItemIntoView();
    }
    emit('navigate', { item, index });
};

watch(toc, async (nextToc, previousToc) => {
    const nextSignature = nextToc.map((item) => `${item.id}:${item.level}:${item.text}`).join('|');
    const previousSignature = (previousToc || []).map((item) => `${item.id}:${item.level}:${item.text}`).join('|');
    if (nextSignature === previousSignature && observer.value && !props.refreshOnContentChange) {
        return;
    }

    if (activeId.value && !nextToc.some((item) => item.id === activeId.value)) {
        activeId.value = nextToc[0]?.id || '';
    } else if (!activeId.value && nextToc.length > 0) {
        activeId.value = nextToc[0].id;
    }

    await scheduleObserverRefresh();
});

watch(() => props.targetSelector, () => {
    scheduleObserverRefresh();
});

onMounted(() => {
    scheduleObserverRefresh();
});

onUnmounted(() => {
    if (observer.value) {
        observer.value.disconnect();
    }
});
</script>

<template>
    <aside
        v-if="toc.length > 0"
        :class="['article-toc', { 'article-toc--mobile-visible': mobileVisible }]"
    >
        <div class="toc-header">
            <span>目录</span>
            <div class="toc-header-right">
                <strong>{{ toc.length }}</strong>
                <button
                    v-if="hasDeepItems && toc.length > TOC_COLLAPSE_THRESHOLD"
                    type="button"
                    class="toc-collapse-btn"
                    :title="deepCollapsed ? '展开全部目录' : '折叠三级以下目录'"
                    @click="deepCollapsed = !deepCollapsed"
                >
                    {{ deepCollapsed ? '展开' : '折叠' }}
                </button>
            </div>
        </div>
        <nav ref="tocNavRef" class="toc-nav" aria-label="文章目录">
            <a
                v-for="(item, index) in visibleToc"
                :key="item.id"
                :href="`#${item.id}`"
                :class="['toc-item', `level-${item.level}`, { active: activeId === item.id }]"
                :data-toc-id="item.id"
                :title="item.text"
                @click.prevent="scrollToHeading(item, toc.indexOf(item))"
            >
                {{ item.text }}
            </a>
            <button
                v-if="deepCollapsed && hasDeepItems"
                type="button"
                class="toc-show-more"
                @click="deepCollapsed = false"
            >
                显示更多目录...
            </button>
        </nav>
    </aside>
</template>

<style scoped>
.article-toc {
    margin: 16px 0 0;
    padding: 14px 0 14px 14px;
    background: var(--surface);
    border: 1px solid rgba(219, 227, 223, 0.82);
    border-radius: var(--radius-md);
    box-shadow: 0 12px 24px rgba(29, 45, 41, 0.05);
}

.toc-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
    padding: 0 14px 10px 0;
    margin-bottom: 8px;
    font-size: 14px;
    font-weight: 800;
    color: var(--text);
    border-bottom: 1px solid rgba(219, 227, 223, 0.72);
}

.toc-header-right {
    display: flex;
    align-items: center;
    gap: 6px;
}

.toc-header strong {
    min-width: 24px;
    height: 24px;
    color: var(--brand-strong);
    font-size: 12px;
    line-height: 24px;
    text-align: center;
    background: var(--surface-soft);
    border-radius: var(--radius-md);
}

.toc-collapse-btn {
    height: 22px;
    padding: 0 7px;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    background: var(--surface);
    color: var(--muted);
    font-size: 11px;
    font-weight: 600;
    cursor: pointer;
    transition: color 0.12s, border-color 0.12s;
}

.toc-collapse-btn:hover {
    color: var(--brand);
    border-color: var(--brand);
}

.toc-show-more {
    display: block;
    width: 100%;
    padding: 6px 13px;
    border: none;
    background: transparent;
    color: var(--brand);
    font-size: 12px;
    cursor: pointer;
    text-align: left;
    border-radius: var(--radius-sm);
    transition: background 0.12s;
}

.toc-show-more:hover {
    background: var(--brand-soft);
}

.toc-nav {
    position: relative;
    display: grid;
    gap: 2px;
    max-height: calc(100vh - 260px);
    padding: 2px 10px 2px 0;
    overflow-y: auto;
}

.toc-nav::before {
    position: absolute;
    top: 2px;
    bottom: 2px;
    left: 0;
    width: 1px;
    content: "";
    background: var(--line);
}

.toc-item {
    position: relative;
    display: flex;
    align-items: center;
    min-height: 30px;
    padding: 5px 8px 5px 13px;
    overflow: hidden;
    color: var(--muted);
    font-size: 13px;
    line-height: 1.4;
    text-overflow: ellipsis;
    white-space: nowrap;
    border-radius: var(--radius-sm);
    transition: color 0.2s, background 0.2s;
}

.toc-item::before {
    position: absolute;
    top: 7px;
    bottom: 7px;
    left: 0;
    width: 3px;
    content: "";
    background: transparent;
    border-radius: var(--radius-md);
}

.toc-item:hover {
    color: var(--brand-strong);
    background: var(--brand-soft);
}

.toc-item.active {
    color: var(--brand-strong);
    font-weight: 800;
    background: var(--brand-soft);
}

.toc-item.active::before {
    background: var(--brand);
}

.toc-item.level-2 {
    padding-left: 25px;
}

.toc-item.level-3 {
    padding-left: 37px;
    font-size: 12px;
}

.toc-item.level-4 {
    padding-left: 49px;
    font-size: 12px;
}

.toc-item.level-5 {
    padding-left: 61px;
    color: #7a8783;
    font-size: 12px;
}

@media (max-width: 980px) {
    .article-toc:not(.article-toc--mobile-visible) {
        display: none;
    }

    .article-toc--mobile-visible {
        display: block;
        margin: 0;
        border: 0;
        box-shadow: none;
    }

    .article-toc--mobile-visible .toc-nav {
        max-height: calc(72vh - 88px);
    }
}
</style>
