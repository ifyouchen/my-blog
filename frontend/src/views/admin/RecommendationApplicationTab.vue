<script setup>
import {computed, onMounted, ref, watch} from 'vue';
import {
    approveAdminRecommendationApplicationApi,
    getAdminRecommendationApplicationsApi,
    rejectAdminRecommendationApplicationApi
} from '@/api/growth';
import AdminSelect from '@/components/admin/AdminSelect.vue';

const STATUS_OPTIONS = [
    { value: '', label: '全部状态' },
    { value: 'PENDING', label: '待审核' },
    { value: 'APPROVED', label: '已通过' },
    { value: 'REJECTED', label: '已拒绝' }
];

const status = ref('');
const page = ref(1);
const size = 10;
const total = ref(0);
const items = ref([]);
const loading = ref(false);
const feedback = ref('');
const feedbackType = ref('success');
const actionId = ref(null);

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size)));

const loadApplications = async () => {
    loading.value = true;
    try {
        const result = await getAdminRecommendationApplicationsApi({
            status: status.value || undefined,
            page: page.value,
            size
        });
        items.value = result.items || [];
        total.value = result.total || 0;
    } catch (error) {
        items.value = [];
        total.value = 0;
        feedback.value = error.message || '加载推荐申请失败';
        feedbackType.value = 'error';
    } finally {
        loading.value = false;
    }
};

const submitReview = async (item, action) => {
    if (!item?.id || actionId.value === item.id) {
        return;
    }
    actionId.value = item.id;
    feedback.value = '';
    try {
        if (action === 'approve') {
            await approveAdminRecommendationApplicationApi(item.id);
            feedback.value = `已通过《${item.articleTitle || item.articleId}》的首页推荐申请`;
        } else {
            await rejectAdminRecommendationApplicationApi(item.id);
            feedback.value = `已拒绝《${item.articleTitle || item.articleId}》的首页推荐申请`;
        }
        feedbackType.value = 'success';
        await loadApplications();
    } catch (error) {
        feedback.value = error.message || '审核失败，请稍后重试';
        feedbackType.value = 'error';
    } finally {
        actionId.value = null;
    }
};

const changePage = (nextPage) => {
    if (nextPage < 1 || nextPage > totalPages.value || nextPage === page.value || loading.value) {
        return;
    }
    page.value = nextPage;
    loadApplications();
};

watch(status, () => {
    page.value = 1;
    loadApplications();
});

onMounted(loadApplications);
</script>

<template>
    <section class="ag-section">
        <div class="ag-section-head">
            <h2 class="ag-section-title">首页推荐申请</h2>
            <span class="ag-section-subtitle">处理 Lv.7 创作者提交的首页推荐申请</span>
        </div>
        <p class="ag-hint">审核通过后会直接沿用现有首页精选逻辑，不会新增第二套推荐渲染链路。</p>

        <div class="ag-query-row">
            <AdminSelect v-model="status" :options="STATUS_OPTIONS" />
            <button type="button" class="ag-btn secondary" :disabled="loading" @click="loadApplications">
                {{ loading ? '刷新中...' : '刷新列表' }}
            </button>
        </div>

        <p v-if="feedback" :class="feedbackType === 'error' ? 'ag-error' : 'ag-success'">{{ feedback }}</p>

        <div class="ag-table-wrap">
            <table class="ag-table">
                <thead>
                    <tr>
                        <th>文章</th>
                        <th>作者</th>
                        <th>申请时间</th>
                        <th>状态</th>
                        <th>审核时间</th>
                        <th>备注</th>
                        <th>操作</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-if="loading">
                        <td colspan="7" class="ag-table-empty">加载中...</td>
                    </tr>
                    <tr v-else-if="!items.length">
                        <td colspan="7" class="ag-table-empty">暂无推荐申请</td>
                    </tr>
                    <tr v-for="item in items" v-else :key="item.id">
                        <td>
                            <strong>{{ item.articleTitle || `文章 ${item.articleId}` }}</strong>
                            <small>ID: {{ item.articleId }}</small>
                        </td>
                        <td>{{ item.authorName || `用户 ${item.authorId}` }}</td>
                        <td class="time-cell"><span>{{ item.appliedAt || '-' }}</span></td>
                        <td>{{ STATUS_OPTIONS.find((option) => option.value === item.status)?.label || item.status }}</td>
                        <td class="time-cell"><span>{{ item.reviewedAt || '-' }}</span></td>
                        <td>{{ item.reviewNote || '-' }}</td>
                        <td class="action-cell">
                            <a
                                class="ag-btn secondary small"
                                :href="`/articles/${item.articleId}`"
                                target="_blank"
                                rel="noreferrer"
                            >
                                查看文章
                            </a>
                            <button
                                v-if="item.status === 'PENDING'"
                                type="button"
                                class="ag-btn primary small"
                                :disabled="actionId === item.id"
                                @click="submitReview(item, 'approve')"
                            >
                                {{ actionId === item.id ? '处理中...' : '通过' }}
                            </button>
                            <button
                                v-if="item.status === 'PENDING'"
                                type="button"
                                class="ag-btn danger small"
                                :disabled="actionId === item.id"
                                @click="submitReview(item, 'reject')"
                            >
                                {{ actionId === item.id ? '处理中...' : '拒绝' }}
                            </button>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

        <div v-if="totalPages > 1" class="ag-pagination">
            <button type="button" class="ag-btn secondary small" :disabled="page <= 1 || loading" @click="changePage(page - 1)">
                上一页
            </button>
            <span>第 {{ page }} / {{ totalPages }} 页，共 {{ total }} 条</span>
            <button
                type="button"
                class="ag-btn secondary small"
                :disabled="page >= totalPages || loading"
                @click="changePage(page + 1)"
            >
                下一页
            </button>
        </div>
    </section>
</template>
