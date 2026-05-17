import {computed, onMounted, onUnmounted, ref, watch} from 'vue';
import {
    getNotificationUnreadCountApi,
    getRecentNotificationsApi,
    subscribeNotificationStream
} from '@/api/notifications';
import {getMessageUnreadCountApi, subscribeMessageStream} from '@/api/messages';

const unreadCount = ref(0);
const recentNotifications = ref([]);
const notificationsLoading = ref(false);
const notificationError = ref('');
const markingAllRead = ref(false);
const messageUnreadCount = ref(0);

let activeConsumers = 0;
let currentLoginState = false;
let notificationPollInterval = null;
let stopSyncTimer = null;
let syncStarted = false;
let unsubscribeNotificationStream = null;
let unsubscribeMessageStream = null;

const displayUnreadCount = computed(() => {
    if (unreadCount.value <= 0) return '';
    if (unreadCount.value > 99) return '99+';
    return unreadCount.value;
});

const displayMessageUnreadCount = computed(() => {
    if (messageUnreadCount.value <= 0) return '';
    if (messageUnreadCount.value > 99) return '99+';
    return messageUnreadCount.value;
});

const clearStopSyncTimer = () => {
    if (stopSyncTimer) {
        clearTimeout(stopSyncTimer);
        stopSyncTimer = null;
    }
};

const resetNotificationState = () => {
    unreadCount.value = 0;
    messageUnreadCount.value = 0;
    recentNotifications.value = [];
    notificationError.value = '';
    notificationsLoading.value = false;
    markingAllRead.value = false;
};

const fetchUnreadCount = async () => {
    if (!currentLoginState) {
        return;
    }
    try {
        const result = await getNotificationUnreadCountApi();
        if (currentLoginState) {
            unreadCount.value = result.count || 0;
        }
    } catch (e) {
        // 获取未读通知失败
    }
};

const fetchMessageUnreadCount = async () => {
    if (!currentLoginState) {
        return;
    }
    try {
        const result = await getMessageUnreadCountApi();
        if (currentLoginState) {
            messageUnreadCount.value = result.count || 0;
        }
    } catch (e) {
        // 获取消息未读数失败
    }
};

const fetchRecentNotifications = async () => {
    if (!currentLoginState) {
        recentNotifications.value = [];
        notificationError.value = '';
        return;
    }
    notificationsLoading.value = true;
    notificationError.value = '';
    try {
        const result = await getRecentNotificationsApi(5);
        if (currentLoginState) {
            recentNotifications.value = result || [];
        }
    } catch (e) {
        if (currentLoginState) {
            notificationError.value = e.message || '通知加载失败，请稍后重试';
        }
    } finally {
        notificationsLoading.value = false;
    }
};

const stopNotificationSync = ({clear = false} = {}) => {
    clearStopSyncTimer();
    syncStarted = false;
    if (unsubscribeNotificationStream) {
        unsubscribeNotificationStream();
        unsubscribeNotificationStream = null;
    }
    if (unsubscribeMessageStream) {
        unsubscribeMessageStream();
        unsubscribeMessageStream = null;
    }
    if (notificationPollInterval) {
        clearInterval(notificationPollInterval);
        notificationPollInterval = null;
    }
    if (clear) {
        resetNotificationState();
    }
};

const refreshUnreadCounts = () => {
    fetchUnreadCount();
    fetchMessageUnreadCount();
};

const startNotificationSync = () => {
    if (!currentLoginState) {
        return;
    }
    clearStopSyncTimer();
    refreshUnreadCounts();
    if (syncStarted) {
        return;
    }
    syncStarted = true;

    unsubscribeNotificationStream = subscribeNotificationStream((count) => {
        if (currentLoginState) {
            unreadCount.value = count;
        }
    });
    unsubscribeMessageStream = subscribeMessageStream(
        () => { fetchMessageUnreadCount(); },
        (count) => {
            if (currentLoginState) {
                messageUnreadCount.value = count;
            }
        }
    );
    notificationPollInterval = setInterval(refreshUnreadCounts, 60000);
};

const releaseHeaderNotificationConsumer = () => {
    activeConsumers = Math.max(0, activeConsumers - 1);
    if (activeConsumers > 0) {
        return;
    }
    clearStopSyncTimer();
    stopSyncTimer = setTimeout(() => {
        stopSyncTimer = null;
        if (activeConsumers === 0) {
            stopNotificationSync();
        }
    }, 1200);
};

export const useHeaderNotifications = (isLoggedIn) => {
    let stopLoginWatch = null;

    onMounted(() => {
        activeConsumers += 1;
        clearStopSyncTimer();
        stopLoginWatch = watch(isLoggedIn, (loggedIn) => {
            currentLoginState = Boolean(loggedIn);
            if (currentLoginState) {
                startNotificationSync();
            } else {
                stopNotificationSync({clear: true});
            }
        }, {immediate: true});
    });

    onUnmounted(() => {
        if (stopLoginWatch) {
            stopLoginWatch();
            stopLoginWatch = null;
        }
        releaseHeaderNotificationConsumer();
    });

    return {
        displayMessageUnreadCount,
        displayUnreadCount,
        fetchRecentNotifications,
        markingAllRead,
        messageUnreadCount,
        notificationError,
        notificationsLoading,
        recentNotifications,
        refreshUnreadCounts,
        unreadCount
    };
};
