<script setup>
import { reactive, watch } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import ConfirmDialog from '@/components/ConfirmDialog.vue';
import AdminPagination from '@/components/admin/AdminPagination.vue';
import { deleteAdminCommentApi, getAdminCommentsApi } from '@/api/admin';
import {
    createPagedState,
    formatAdminDateTime,
    readPositiveInt,
    readQueryText,
    resolveAdminOverflowPage,
    syncAdminQuery,
    useAdminRefresh
} from '@/views/admin/adminShared';
import { useConfirmDialog } from '@/composables/useConfirmDialog';
import { useToast } from '@/composables/useToast';

const route = useRoute();
const router = useRouter();
const toast = useToast();
const {
    confirmDialog,
    openConfirmDialog,
    closeConfirmDialog,
    executeConfirmDialog
} = useConfirmDialog();

const state = reactive({
    ...createPagedState(),
    articleId: '',
    keyword: '',
    actionLoadingId: null
});

const applyRouteState = () => {
    state.page = readPositiveInt(route.query.page, 1);
    state.articleId = readQueryText(route, 'articleId');
    state.keyword = readQueryText(route, 'keyword');
    state.jumpPage = String(state.page);
};

const loadComments = async () => {
    state.loading = true;
    state.error = '';
    try {
        const articleIdValue = state.articleId ? Number.parseInt(state.articleId, 10) : null;
        const result = await getAdminCommentsApi(
            state.page,
            state.pageSize,
            Number.isNaN(articleIdValue) ? null : articleIdValue,
            state.keyword || null
        );
        const overflowPage = resolveAdminOverflowPage(state, result);
        if (overflowPage) {
            state.page = overflowPage;
            state.jumpPage = String(overflowPage);
            await syncQuery({
                page: overflowPage > 1 ? String(overflowPage) : undefined
            });
            return;
        }
        state.items = result.items || [];
        state.total = result.total || 0;
        state.page = result.page || state.page;
        state.jumpPage = String(state.page);
    } catch (error) {
        state.error = error.message || '评论列表加载失败';
    } finally {
        state.loading = false;
    }
};

const syncQuery = async (patch = {}) => {
    await syncAdminQuery(router, route, {
        page: patch.page ?? (state.page > 1 ? String(state.page) : undefined),
        articleId: patch.articleId ?? (state.articleId || undefined),
        keyword: patch.keyword ?? (state.keyword || undefined)
    });
};

const submitFilters = async () => {
    state.page = 1;
    await syncQuery({
        page: undefined,
        articleId: state.articleId || undefined,
        keyword: state.keyword || undefined
    });
};

const resetFilters = async () => {
    state.articleId = '';
    state.keyword = '';
    state.page = 1;
    await syncQuery({ page: undefined, articleId: undefined, keyword: undefined });
};

const changePage = async (targetPage) => {
    state.page = targetPage;
    await syncQuery({
        page: targetPage > 1 ? String(targetPage) : undefined
    });
};

const removeComment = async (comment) => {
    openConfirmDialog({
        eyebrow: '评论治理确认',
        title: '删除评论',
        message: `确定删除评论 #${comment.id} 吗？删除后这条评论将从文章互动区移除。`,
        confirmText: '确认删除',
        tone: 'danger',
        onConfirm: async () => {
            state.actionLoadingId = comment.id;
            try {
                await deleteAdminCommentApi(comment.id);
                await loadComments();
            } catch (error) {
                toast.error(error.message || '评论删除失败');
            } finally {
                state.actionLoadingId = null;
            }
        }
    });
};

useAdminRefresh(loadComments);

watch(
    () => [route.query.page, route.query.articleId, route.query.keyword],
    () => {
        applyRouteState();
        loadComments();
    },
    { immediate: true }
);
</script>

<template>
    <section class="dashboard-content-panel">
        <div class="admin-toolbar">
            <form class="admin-filter-toolbar" @submit.prevent="submitFilters">
                <label>
                    <span>文章 ID</span>
                    <input v-model.trim="state.articleId" type="number" min="1" placeholder="按文章筛选">
                </label>
                <label class="admin-filter-grow">
                    <span>关键词</span>
                    <input v-model.trim="state.keyword" type="text" placeholder="搜索评论内容或文章标题">
                </label>
                <div class="admin-filter-actions">
                    <button type="submit">查询</button>
                    <button type="button" @click="resetFilters">重置</button>
                </div>
            </form>
        </div>

        <div class="admin-table-shell">
            <p v-if="state.error && state.items.length" class="backend-state-text error-text subtle">
                {{ state.error }}
            </p>
            <p v-else-if="state.error && !state.items.length" class="backend-state-text error-text">
                {{ state.error }}
            </p>
            <template v-else>
                <div class="admin-table-wrap" data-testid="admin-comments-table">
                    <table class="admin-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>类型</th>
                                <th>所属文章</th>
                                <th>评论人</th>
                                <th>内容</th>
                                <th>发布时间</th>
                                <th>操作</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="comment in state.items" :key="comment.id">
                                <td>#{{ comment.id }}</td>
                                <td>
                                    <span class="status-pill" :class="{ warning: comment.type === 'REPLY' }">
                                        {{ comment.type === 'ROOT' ? '主评论' : '回复' }}
                                    </span>
                                </td>
                                <td>
                                    <RouterLink :to="`/articles/${comment.articleId}`">
                                        {{ comment.articleTitle || `文章 #${comment.articleId}` }}
                                    </RouterLink>
                                    <p class="admin-subtext">文章 ID：{{ comment.articleId }}</p>
                                </td>
                                <td>
                                    <strong>{{ comment.nickname || comment.username || `#${comment.userId}` }}</strong>
                                    <p class="admin-subtext">@{{ comment.username || comment.userId }}</p>
                                </td>
                                <td>{{ comment.content }}</td>
                                <td>{{ formatAdminDateTime(comment.createdAt) }}</td>
                                <td class="table-actions">
                                    <button
                                        type="button"
                                        class="danger-link"
                                        :disabled="state.actionLoadingId === comment.id"
                                        @click="removeComment(comment)"
                                    >
                                        删除
                                    </button>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                <p v-if="!state.items.length" class="backend-state-text">当前筛选条件下没有评论</p>
            </template>
        </div>

        <AdminPagination :state="state" label="评论分页" @page-change="changePage" />
        <ConfirmDialog
            :visible="confirmDialog.visible"
            :eyebrow="confirmDialog.eyebrow"
            :title="confirmDialog.title"
            :message="confirmDialog.message"
            :confirm-text="confirmDialog.confirmText"
            :tone="confirmDialog.tone"
            :loading="confirmDialog.loading"
            @close="closeConfirmDialog"
            @confirm="executeConfirmDialog"
        />
    </section>
</template>
