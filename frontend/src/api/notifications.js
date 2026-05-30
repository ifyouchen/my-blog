import {request, subscribeAuthorizedEventStream} from './http';

export const getNotificationsApi = async (page = 1, pageSize = 10, filter = 'all') => {
    return await request(`/notifications?page=${page}&pageSize=${pageSize}&filter=${filter}`);
};

export const getNotificationUnreadCountApi = async () => {
    return await request('/notifications/unread-count');
};

export const getRecentNotificationsApi = async (limit = 5, filter = 'unread') => {
    return await request(`/notifications/recent?limit=${limit}&filter=${filter}`);
};

export const markNotificationReadApi = async (id) => {
    return await request(`/notifications/${id}/read`, {
        method: 'POST'
    });
};

export const markAllNotificationsReadApi = async () => {
    return await request('/notifications/read-all', {
        method: 'POST'
    });
};

/**
 * 订阅 SSE 通知流。
 * @param {function} onUnread - 收到未读数更新时的回调 (count: number)
 * @returns {function} 取消订阅的函数
 */
export const subscribeNotificationStream = (onUnread) => {
    return subscribeAuthorizedEventStream('/notifications/stream', {
        unread: (event) => {
            try {
                const data = JSON.parse(event.data);
                if (typeof data.count === 'number') {
                    onUnread(data.count);
                }
            } catch {
                // ignore parse errors
            }
        }
    });
};

export const getActiveAnnouncementsApi = async () => {
    return await request('/announcements/active', {
        suppressAuthPrompt: true
    });
};
