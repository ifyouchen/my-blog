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
    }
});

const activeId = ref('');
const tocNavRef = ref(null);
const observer = ref(null);

const toc = computed(() => extractToc(props.content));

const scrollActiveItemIntoView = () => {
    if (!tocNavRef.value || !activeId.value) {
        return;
    }
    const activeLink = tocNavRef.value.querySelector(`[data-toc-id="${activeId.value}"]`);
    if (activeLink) {
        activeLink.scrollIntoView({ block: 'nearest' });
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

const scrollToHeading = (id) => {
    const element = document.getElementById(id);
    if (element) {
        activeId.value = id;
        element.scrollIntoView({ behavior: 'smooth', block: 'start' });
        scrollActiveItemIntoView();
    }
};

watch(() => props.content, () => {
    activeId.value = '';
    nextTick(() => {
        setTimeout(initObserver, 100);
    });
});

onMounted(() => {
    setTimeout(initObserver, 100);
});

onUnmounted(() => {
    if (observer.value) {
        observer.value.disconnect();
    }
});
</script>

<template>
    <aside v-if="toc.length > 0" class="article-toc">
        <div class="toc-header">
            <span>目录</span>
            <strong>{{ toc.length }}</strong>
        </div>
        <nav ref="tocNavRef" class="toc-nav" aria-label="文章目录">
            <a
                v-for="item in toc"
                :key="item.id"
                :href="`#${item.id}`"
                :class="['toc-item', `level-${item.level}`, { active: activeId === item.id }]"
                :data-toc-id="item.id"
                :title="item.text"
                @click.prevent="scrollToHeading(item.id)"
            >
                {{ item.text }}
            </a>
        </nav>
    </aside>
</template>

<style scoped>
.article-toc {
    margin: 16px 0 0;
    padding: 14px 0 14px 14px;
    background: var(--surface);
    border: 1px solid rgba(219, 227, 223, 0.82);
    border-radius: 8px;
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

.toc-header strong {
    min-width: 24px;
    height: 24px;
    color: var(--brand-strong);
    font-size: 12px;
    line-height: 24px;
    text-align: center;
    background: var(--surface-soft);
    border-radius: 8px;
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
    border-radius: 6px;
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
    border-radius: 8px;
}

.toc-item:hover {
    color: var(--brand-strong);
    background: rgba(15, 143, 117, 0.05);
}

.toc-item.active {
    color: var(--brand-strong);
    font-weight: 800;
    background: rgba(15, 143, 117, 0.08);
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
    .article-toc {
        display: none;
    }
}
</style>
