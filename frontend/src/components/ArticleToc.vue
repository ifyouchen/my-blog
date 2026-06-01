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
    },
    collapsible: {
        type: Boolean,
        default: false
    }
});

const emit = defineEmits(['navigate']);

const activeId = ref('');
const tocNavRef = ref(null);
const observer = ref(null);
const tocCollapsed = ref(false);
const tocNavId = `article-toc-nav-${Math.random().toString(36).slice(2, 10)}`;
const tocTooltip = ref({
    visible: false,
    text: '',
    x: 0,
    y: 0,
    side: 'left'
});
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

const showTocTooltip = (event, text) => {
    if (props.mobileVisible || !text) {
        tocTooltip.value.visible = false;
        return;
    }
    const rect = event.currentTarget.getBoundingClientRect();
    const tooltipMaxWidth = Math.min(480, window.innerWidth - 48);
    const showOnLeft = rect.left > tooltipMaxWidth + 24;
    tocTooltip.value = {
        visible: true,
        text,
        x: showOnLeft ? rect.left - 12 : rect.right + 12,
        y: Math.min(Math.max(rect.top + (rect.height / 2), 56), window.innerHeight - 56),
        side: showOnLeft ? 'left' : 'right'
    };
};

const hideTocTooltip = () => {
    tocTooltip.value.visible = false;
};

const toggleTocCollapsed = () => {
    tocCollapsed.value = !tocCollapsed.value;
    if (tocCollapsed.value) {
        hideTocTooltip();
    }
};

const handleTocShellClick = (event) => {
    if (!props.collapsible || event.target.closest('button, a')) {
        return;
    }
    if (tocCollapsed.value || event.target.closest('.toc-header')) {
        toggleTocCollapsed();
    }
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
        :class="[
            'article-toc',
            {
                'article-toc--mobile-visible': mobileVisible,
                'article-toc--collapsible': collapsible,
                'article-toc--collapsed': tocCollapsed
            }
        ]"
        @click="handleTocShellClick"
    >
        <div class="toc-header">
            <span>目录</span>
            <div class="toc-header-right">
                <strong>{{ toc.length }}</strong>
                <button
                    v-if="collapsible"
                    type="button"
                    class="toc-collapse-btn"
                    :aria-controls="tocNavId"
                    :aria-expanded="!tocCollapsed"
                    :title="tocCollapsed ? '打开文章目录' : '隐藏文章目录'"
                    @click="toggleTocCollapsed"
                >
                    {{ tocCollapsed ? '打开' : '隐藏' }}
                </button>
                <button
                    v-if="!tocCollapsed && hasDeepItems && toc.length > TOC_COLLAPSE_THRESHOLD"
                    type="button"
                    class="toc-collapse-btn"
                    :title="deepCollapsed ? '展开全部目录' : '折叠三级以下目录'"
                    @click="deepCollapsed = !deepCollapsed"
                >
                    {{ deepCollapsed ? '展开' : '折叠' }}
                </button>
            </div>
        </div>
        <nav
            v-if="!tocCollapsed"
            :id="tocNavId"
            ref="tocNavRef"
            class="toc-nav"
            aria-label="文章目录"
        >
            <a
                v-for="(item, index) in visibleToc"
                :key="item.id"
                :href="`#${item.id}`"
                :class="['toc-item', `level-${item.level}`, { active: activeId === item.id }]"
                :data-toc-id="item.id"
                :title="item.text"
                :aria-label="item.text"
                @mouseenter="showTocTooltip($event, item.text)"
                @mouseleave="hideTocTooltip"
                @focus="showTocTooltip($event, item.text)"
                @blur="hideTocTooltip"
                @click.prevent="scrollToHeading(item, toc.indexOf(item))"
            >
                <span class="toc-item-text">{{ item.text }}</span>
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
    <Teleport to="body">
        <div
            v-if="tocTooltip.visible"
            :class="['toc-title-tooltip', `toc-title-tooltip--${tocTooltip.side}`]"
            :style="{ left: `${tocTooltip.x}px`, top: `${tocTooltip.y}px` }"
            role="tooltip"
        >
            {{ tocTooltip.text }}
        </div>
    </Teleport>
</template>

<style scoped src="@/styles/components/ArticleToc.css"></style>
