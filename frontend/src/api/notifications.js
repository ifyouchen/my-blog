import { request } from './http';

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