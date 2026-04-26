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
            <p class="eyebrow">技术创作者平台</p>
            <h1 id="quick-title">发现今天值得读的工程内容</h1>
            <p class="quick-copy">
                覆盖后端架构、前端体验、数据库调优与团队实践，把可以直接落地到项目里的文章放到最前面。
            </p>
        </div>
        <div class="quick-stats" aria-label="平台数据">
            <span><strong>{{ formatNumber(totalArticles) }}</strong> 篇文章</span>
            <span><strong>{{ formatNumber(totalAuthors) }}</strong> 位作者</span>
            <span><strong>{{ formatNumber(totalColumns) }}</strong> 个专栏</span>
        </div>
    </section>
</template>
