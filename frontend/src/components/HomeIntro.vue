<script setup>
defineProps({
    totalArticles: {
        type: Number,
        default: 0
    },
    totalAuthors: {
        type: Number,
        default: 0
    },
    totalColumns: {
        type: Number,
        default: 0
    }
});

const formatNumber = (num) => {
    if (num >= 10000) {
        return (num / 10000).toFixed(1) + '万';
    }
    return num.toLocaleString();
};
</script>

<template>
    <section class="quick-panel" aria-labelledby="quick-title">
        <div>
            <p class="eyebrow">技术创作者社区</p>
            <h1 id="quick-title">今天值得读的工程实践</h1>
            <p class="quick-copy">从后端架构、前端体验到数据库调优，把能直接用在项目里的经验放到最前面。</p>
        </div>
        <div class="quick-stats" aria-label="平台数据">
            <span><strong>{{ formatNumber(totalArticles) }}</strong> 篇文章</span>
            <span><strong>{{ formatNumber(totalAuthors) }}</strong> 位作者</span>
            <span><strong>{{ formatNumber(totalColumns) }}</strong> 个专栏</span>
        </div>
    </section>
</template>