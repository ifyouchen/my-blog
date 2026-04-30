<script setup>
import {onMounted, ref} from 'vue';
import {useRouter} from 'vue-router';
import SiteHeader from '@/components/SiteHeader.vue';
import EmptyState from '@/components/EmptyState.vue';
import {getCategoriesApi} from '@/api/categories';
import {getHotTagsApi} from '@/api/tags';

const router = useRouter();
const categories = ref([]);
const hotTags = ref([]);
const loading = ref(true);
const errorMessage = ref('');

const loadData = async () => {
    loading.value = true;
    errorMessage.value = '';
    try {
        const [cats, tags] = await Promise.all([
            getCategoriesApi(),
            getHotTagsApi(50)
        ]);
        categories.value = Array.isArray(cats) ? cats : [];
        hotTags.value = Array.isArray(tags) ? tags : [];
    } catch (err) {
        errorMessage.value = '内容加载失败，请稍后重试';
    } finally {
        loading.value = false;
    }
};

const goToCategory = (category) => {
    router.push({ name: 'categoryDetail', params: { id: category.id } });
};

const goToTag = (tag) => {
    router.push({ name: 'tagDetail', params: { id: tag.id } });
};

onMounted(loadData);
</script>

<template>
    <SiteHeader />
    <main class="page-shell explore-page">
        <section class="explore-hero">
            <div class="explore-hero-inner">
                <p class="eyebrow">探索</p>
                <h1>发现精彩内容</h1>
                <p class="explore-hero-desc">浏览分类和标签，找到你感兴趣的技术主题。</p>
            </div>
        </section>

        <EmptyState
            v-if="errorMessage"
            eyebrow="探索"
            title="加载失败"
            :description="errorMessage"
            tone="error"
        />

        <template v-else>
            <!-- 分类区域 -->
            <section class="explore-section">
                <div class="explore-section-header">
                    <h2>全部分类</h2>
                </div>
                <div v-if="loading" class="explore-grid">
                    <div v-for="i in 8" :key="i" class="explore-category-card explore-skeleton"></div>
                </div>
                <div v-else-if="categories.length" class="explore-grid">
                    <button
                        v-for="cat in categories"
                        :key="cat.id"
                        class="explore-category-card"
                        @click="goToCategory(cat)"
                    >
                        <span class="explore-category-name">{{ cat.name }}</span>
                        <span v-if="cat.description" class="explore-category-desc">{{ cat.description }}</span>
                    </button>
                </div>
                <p v-else class="explore-empty">暂无分类</p>
            </section>

            <!-- 热门标签区域 -->
            <section class="explore-section">
                <div class="explore-section-header">
                    <h2>热门标签</h2>
                    <p class="explore-section-sub">按使用量排序</p>
                </div>
                <div v-if="loading" class="explore-tags">
                    <div v-for="i in 20" :key="i" class="explore-tag explore-tag-skeleton"></div>
                </div>
                <div v-else-if="hotTags.length" class="explore-tags">
                    <button
                        v-for="tag in hotTags"
                        :key="tag.id"
                        class="explore-tag"
                        @click="goToTag(tag)"
                    >
                        <span class="explore-tag-hash">#</span>{{ tag.name }}
                        <span v-if="tag.useCount" class="explore-tag-count">{{ tag.useCount }}</span>
                    </button>
                </div>
                <p v-else class="explore-empty">暂无热门标签</p>
            </section>
        </template>
    </main>
</template>

<style scoped>
.explore-page {
    display: grid;
    gap: 24px;
}

.explore-hero {
    padding: 36px 28px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
}

.explore-hero-inner {
    display: grid;
    gap: 10px;
    max-width: 640px;
}

.explore-hero-inner h1 {
    margin: 0;
    font-size: clamp(26px, 3vw, 36px);
    line-height: 1.2;
}

.explore-hero-desc {
    margin: 0;
    color: var(--muted);
    font-size: 15px;
    line-height: 1.8;
}

/* 通用区块 */
.explore-section {
    display: grid;
    gap: 16px;
}

.explore-section-header {
    display: flex;
    align-items: baseline;
    gap: 10px;
}

.explore-section-header h2 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
}

.explore-section-sub {
    margin: 0;
    color: var(--muted);
    font-size: 13px;
}

.explore-empty {
    color: var(--muted);
    font-size: 14px;
}

/* 分类网格 */
.explore-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
    gap: 12px;
}

.explore-category-card {
    display: grid;
    gap: 6px;
    padding: 16px 18px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
    cursor: pointer;
    text-align: left;
    transition: border-color 0.15s, box-shadow 0.15s;
}

.explore-category-card:hover {
    border-color: var(--accent);
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
}

.explore-category-name {
    font-size: 15px;
    font-weight: 600;
    color: var(--text);
}

.explore-category-desc {
    font-size: 12px;
    color: var(--muted);
    line-height: 1.5;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

/* 标签云 */
.explore-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.explore-tag {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 6px 14px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    font-size: 13px;
    color: var(--text);
    cursor: pointer;
    transition: border-color 0.15s, background 0.15s;
}

.explore-tag:hover {
    border-color: var(--accent);
    background: var(--surface-soft);
}

.explore-tag-hash {
    color: var(--accent);
    font-weight: 600;
}

.explore-tag-count {
    margin-left: 4px;
    padding: 1px 6px;
    background: var(--surface-soft);
    border-radius: 999px;
    font-size: 11px;
    color: var(--muted);
}

/* 骨架屏 */
.explore-skeleton {
    min-height: 72px;
    background: linear-gradient(90deg, var(--surface-soft) 25%, var(--line) 37%, var(--surface-soft) 63%);
    background-size: 400% 100%;
    animation: explore-skeleton 1.2s ease-in-out infinite;
}

.explore-tag-skeleton {
    width: 80px;
    height: 32px;
    border-radius: var(--radius-sm);
    background: linear-gradient(90deg, var(--surface-soft) 25%, var(--line) 37%, var(--surface-soft) 63%);
    background-size: 400% 100%;
    animation: explore-skeleton 1.2s ease-in-out infinite;
    border: none;
}

@keyframes explore-skeleton {
    0% { background-position: 100% 0; }
    100% { background-position: 0 0; }
}

@media (max-width: 640px) {
    .explore-grid {
        grid-template-columns: repeat(2, 1fr);
    }
}
</style>

