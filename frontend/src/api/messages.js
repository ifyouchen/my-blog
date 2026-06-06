import {request, subscribeAuthorizedEventStream} from '@/api/http';

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
 * 更新会话置顶状态。
 */
export const updateConversationPinApi = async (conversationId, pinned) => {
    return await request(`/messages/conversations/${conversationId}/pin`, {
        method: 'PUT',
        body: { pinned }
    });
};

/**
 * 更新会话消息免打扰状态。
 */
export const updateConversationMuteApi = async (conversationId, muted) => {
    return await request(`/messages/conversations/${conversationId}/mute`, {
        method: 'PUT',
        body: { muted }
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
export const sendMessageApi = async ({ conversationId, content, type = 'TEXT', parentId }) => {
    return await request(`/messages/conversations/${conversationId}/messages`, {
        method: 'POST',
        body: { content, type, parentId }
    });
};

/**
 * 撤回消息（仅允许发送者在10分钟内撤回）。
 */
export const recallMessageApi = async (messageId) => {
    return await request(`/messages/${messageId}/recall`, {
        method: 'POST'
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
 * SSE 订阅私信事件（新消息、未读计数、消息撤回、在线状态）。
 * 返回取消订阅函数。
 */
export const subscribeMessageStream = (onMessage, onUnread, onRecall, options = {}) => {
    return subscribeAuthorizedEventStream('/messages/stream', {
        onOpen: options.onOpen,
        onError: options.onError,
        'new-message': (event) => {
            try {
                const data = JSON.parse(event.data);
                if (onMessage) {
                    onMessage(data);
                }
            } catch {
                // ignore parse errors
            }
        },
        unread: (event) => {
            try {
                const data = JSON.parse(event.data);
                if (typeof data.count === 'number' && onUnread) {
                    onUnread(data.count);
                }
            } catch {
                // ignore parse errors
            }
        },
        'message-recalled': (event) => {
            try {
                const data = JSON.parse(event.data);
                if (onRecall) {
                    onRecall(data);
                }
            } catch {
                // ignore parse errors
            }
        },
        presence: (event) => {
            try {
                const data = JSON.parse(event.data);
                if (options.onPresence) {
                    options.onPresence(data);
                }
            } catch {
                // ignore parse errors
            }
        }
    }, options);
};
