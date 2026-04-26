<script setup>
import { computed, ref, watch } from 'vue';
import {
    getPageEnd,
    getPageStart,
    getPaginationItems,
    getTotalPages,
    readPositiveInt
} from '@/views/admin/adminShared';

const props = defineProps({
    state: {
        type: Object,
        required: true
    },
    label: {
        type: String,
        default: '分页'
    }
});

const emit = defineEmits(['page-change']);

const totalPages = computed(() => getTotalPages(props.state));
const pageStart = computed(() => getPageStart(props.state));
const pageEnd = computed(() => getPageEnd(props.state));
const items = computed(() => getPaginationItems(props.state));
const jumpPage = ref(String(props.state.page || 1));

watch(
    () => props.state.page,
    (value) => {
        jumpPage.value = String(value || 1);
    },
    { immediate: true }
);

const changePage = (targetPage) => {
    if (props.state.loading || targetPage < 1 || targetPage > totalPages.value || targetPage === props.state.page) {
        return;
    }
    emit('page-change', targetPage);
};

const submitJump = () => {
    const targetPage = Math.min(Math.max(1, readPositiveInt(jumpPage.value, props.state.page)), totalPages.value);
    changePage(targetPage);
};
</script>

<template>
    <nav v-if="totalPages > 1" class="admin-pagination" :aria-label="label">
        <p class="admin-pagination-summary">
            第 {{ state.page }} / {{ totalPages }} 页，共 {{ state.total }} 条，
            当前 {{ pageStart }}-{{ pageEnd }} 条
        </p>
        <div class="admin-pagination-actions">
            <button type="button" :disabled="state.loading || state.page <= 1" @click="changePage(1)">首页</button>
            <button type="button" :disabled="state.loading || state.page <= 1" @click="changePage(state.page - 1)">上一页</button>
            <template v-for="item in items" :key="`${label}-${item.type}-${item.value}`">
                <span v-if="item.type === 'ellipsis'" class="pagination-ellipsis">...</span>
                <button
                    v-else
                    type="button"
                    :class="{ active: item.value === state.page }"
                    :disabled="state.loading || item.value === state.page"
                    @click="changePage(item.value)"
                >
                    {{ item.value }}
                </button>
            </template>
            <button
                type="button"
                :disabled="state.loading || state.page >= totalPages"
                @click="changePage(state.page + 1)"
            >
                下一页
            </button>
            <button
                type="button"
                :disabled="state.loading || state.page >= totalPages"
                @click="changePage(totalPages)"
            >
                末页
            </button>
        </div>
        <form class="admin-pagination-jump" @submit.prevent="submitJump">
            <label :for="`${label}-page-jump`">跳至</label>
            <input
                :id="`${label}-page-jump`"
                v-model="jumpPage"
                type="number"
                min="1"
                :max="totalPages"
                :disabled="state.loading"
                inputmode="numeric"
            >
            <span>页</span>
            <button type="submit" :disabled="state.loading">跳转</button>
        </form>
    </nav>
</template>
