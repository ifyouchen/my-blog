import {request} from '@/api/http';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

/**
 * 获取或创建会话。
 */
export const createConversationApi = async (participantId) => {
    return await request('/messages/conversations', {
        method: 'POST',
        body: { participantId }
    });
};

/**
 * 获取会话列表。
 */
export const getConversationsApi = async ({ page = 1, pageSize = 20 } = {}) => {
    return await request(`/messages/conversations?page=${page}&pageSize=${pageSize}`);
};

/**
 * 删除会话。
 */
export const deleteConversationApi = async (conversationId) => {
    return await request(`/messages/conversations/${conversationId}`, {
        method: 'DELETE'
    });
};

/**
 * 获取消息历史。
 */
export const getMessagesApi = async ({ conversationId, page = 1, pageSize = 50 }) => {
    return await request(`/messages/conversations/${conversationId}/messages?page=${page}&pageSize=${pageSize}`);
};

/**
 * 发送消息。
 */
export const sendMessageApi = async ({ conversationId, content, type = 'TEXT' }) => {
    return await request(`/messages/conversations/${conversationId}/messages`, {
        method: 'POST',
        body: { content, type }
    });
};

/**
 * 标记会话内所有消息为已读。
 */
export const markMessagesReadApi = async (conversationId) => {
    return await request(`/messages/conversations/${conversationId}/read`, {
        method: 'POST'
    });
};

/**
 * 获取未读消息总数。
 */
export const getMessageUnreadCountApi = async () => {
    return await request('/messages/unread-count');
};

/**
 * SSE 订阅私信事件（新消息、未读计数）。
 * 返回取消订阅函数。
 */
export const subscribeMessageStream = (onMessage, onUnread) => {
    const getToken = () => {
        try {
            const raw = localStorage.getItem('my-blog-session');
            const session = raw ? JSON.parse(raw) : null;
            if (!session || !session.token || session.token === 'local-dev-token') return '';
            return session.token;
        } catch {
            return '';
        }
    };

    const token = getToken();
    if (!token) {
        return () => {};
    }

    const url = `${API_BASE_URL}/messages/stream?_t=${Date.now()}&token=${encodeURIComponent(token)}`;
    const es = new EventSource(url);

    es.addEventListener('new-message', (event) => {
        try {
            const data = JSON.parse(event.data);
            if (onMessage) {
                onMessage(data);
            }
        } catch {
            // ignore parse errors
        }
    });

    es.addEventListener('unread', (event) => {
        try {
            const data = JSON.parse(event.data);
            if (typeof data.count === 'number' && onUnread) {
                onUnread(data.count);
            }
        } catch {
            // ignore parse errors
        }
    });

    es.onerror = () => {
        // EventSource auto-reconnects
    };

    return () => es.close();
};
