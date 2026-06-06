<script setup>
import {
    formatAdminDateTime,
    formatAdminOperationLabel,
    formatAdminStatusLabel
} from '@/views/admin/adminShared';

const props = defineProps({
    open: {
        type: Boolean,
        default: false
    },
    log: {
        type: Object,
        default: null
    }
});

const emit = defineEmits(['close']);

const closeDrawer = () => {
    emit('close');
};

const hasLog = () => Boolean(props.log);
</script>

<template>
    <div v-if="open" class="admin-drawer-mask" @click.self="closeDrawer">
        <aside class="admin-drawer" aria-label="日志详情抽屉">
            <div class="admin-drawer-head">
                <div>
                    <p class="eyebrow">日志详情</p>
                    <h2>{{ hasLog() ? formatAdminOperationLabel(log.operation) : '操作日志' }}</h2>
                </div>
                <button type="button" @click="closeDrawer">关闭</button>
            </div>

            <template v-if="hasLog()">
                <dl class="admin-detail-grid">
                    <div>
                        <dt>ID</dt>
                        <dd>{{ log.id }}</dd>
                    </div>
                    <div>
                        <dt>管理员</dt>
                        <dd>{{ log.adminUsername || `#${log.adminUserId}` }}</dd>
                    </div>
                    <div>
                        <dt>目标</dt>
                        <dd>{{ log.targetType }} #{{ log.targetId ?? '-' }}</dd>
                    </div>
                    <div>
                        <dt>结果</dt>
                        <dd>{{ formatAdminStatusLabel(log.resultStatus) }}</dd>
                    </div>
                    <div>
                        <dt>请求</dt>
                        <dd>{{ log.requestMethod || '-' }} {{ log.requestUri || '-' }}</dd>
                    </div>
                    <div>
                        <dt>时间</dt>
                        <dd>{{ formatAdminDateTime(log.createdAt) }}</dd>
                    </div>
                    <div>
                        <dt>IP</dt>
                        <dd>{{ log.ipAddress || '-' }}</dd>
                    </div>
                    <div>
                        <dt>User-Agent</dt>
                        <dd>{{ log.userAgent || '-' }}</dd>
                    </div>
                </dl>

                <section class="admin-drawer-section">
                    <h3>操作详情</h3>
                    <p>{{ log.detail || '-' }}</p>
                </section>

                <section class="admin-drawer-section">
                    <h3>变更前快照</h3>
                    <pre>{{ log.beforeSnapshot || '-' }}</pre>
                </section>

                <section class="admin-drawer-section">
                    <h3>变更后快照</h3>
                    <pre>{{ log.afterSnapshot || '-' }}</pre>
                </section>
            </template>
        </aside>
    </div>
</template>
