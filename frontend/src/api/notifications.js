import {request} from './http';

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
    const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';
    const SESSION_KEY = 'my-blog-session';

    const getToken = () => {
        try {
            const raw = localStorage.getItem(SESSION_KEY);
            const session = raw ? JSON.parse(raw) : null;
            if (!session || !session.token || session.token === 'local-dev-token') return '';
            return session.token;
        } catch {
            return '';
        }
    };

    const token = getToken();
    if (!token) return () => {};

    const url = `${API_BASE_URL}/notifications/stream?_t=${Date.now()}`;
    const es = new EventSource(url + `&token=${encodeURIComponent(token)}`);

    es.addEventListener('unread', (event) => {
        try {
            const data = JSON.parse(event.data);
            if (typeof data.count === 'number') {
                onUnread(data.count);
            }
        } catch {}
    });

    es.onerror = () => {
        // SSE 断开后自动重连（浏览器原生行为），此处不需要额外处理
    };

    return () => es.close();
};

export const getActiveAnnouncementsApi = async () => {
    return await request('/announcements/active');
};
