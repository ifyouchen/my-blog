<script setup>
import {computed, onMounted, onUnmounted, reactive, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {useSession} from '@/stores/session';
import {useToast} from '@/composables/useToast';
import SiteHeader from '@/components/SiteHeader.vue';
import {
    createConversationApi,
    deleteConversationApi,
    getConversationsApi,
    getMessagesApi,
    sendMessageApi,
    markMessagesReadApi,
    getMessageUnreadCountApi,
    subscribeMessageStream
} from '@/api/messages';

const route = useRoute();
const router = useRouter();
const { state: sessionState } = useSession();
const toast = useToast();

const state = reactive({
    conversations: [],
    conversationsLoading: false,
    conversationsError: '',
    activeConversationId: null,
    messages: [],
    messagesLoading: false,
    messagesError: '',
    sending: false,
    loadingMore: false,
    hasMore: true,
    page: 1,
    pageSize: 50,
    totalMessages: 0
});

const messageContainer = ref(null);
const messageInput = ref('');

const otherParticipant = computed(() => {
    if (!state.activeConversationId) return null;
    return state.conversations.find(c => c.id === state.activeConversationId)?.participant || null;
});

const loadConversations = async () => {
    state.conversationsLoading = true;
    state.conversationsError = '';
    try {
        const result = await getConversationsApi();
        state.conversations = result.items || [];
    } catch (e) {
        state.conversationsError = e.message || '加载会话列表失败';
    } finally {
        state.conversationsLoading = false;
    }
};

const loadMessages = async (conversationId, append = false) => {
    if (!append) {
        state.messagesLoading = true;
        state.messagesError = '';
        state.page = 1;
        state.hasMore = true;
    }
    try {
        const result = await getMessagesApi({
            conversationId,
            page: state.page,
            pageSize: state.pageSize
        });
        const items = result.items || [];
        state.totalMessages = result.total || 0;
        if (append) {
            state.messages = [...items.reverse(), ...state.messages];
        } else {
            state.messages = items.reverse();
        }
        state.hasMore = state.messages.length < state.totalMessages;
        // 标记已读
        await markMessagesReadApi(conversationId).catch(() => {});
    } catch (e) {
        state.messagesError = e.message || '加载消息失败';
    } finally {
        state.messagesLoading = false;
        state.loadingMore = false;
    }
};

const selectConversation = async (conv) => {
    state.activeConversationId = conv.id;
    await loadMessages(conv.id);
    // 更新未读计数
    conv.unreadCount = 0;
    // 更新 URL
    await router.replace({ query: { ...route.query }, params: { conversationId: String(conv.id) } });
};

const sendMessage = async () => {
    const content = messageInput.value.trim();
    if (!content || !state.activeConversationId) return;
    state.sending = true;
    try {
        const msg = await sendMessageApi({
            conversationId: state.activeConversationId,
            content
        });
        state.messages.push(msg);
        messageInput.value = '';
        // 滚动到底部
        scrollToBottom();
    } catch (e) {
        toast.error(e.message || '发送失败');
    } finally {
        state.sending = false;
    }
};

const handleKeydown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
    }
};

const loadMore = async () => {
    if (!state.activeConversationId || state.loadingMore || !state.hasMore) return;
    state.loadingMore = true;
    state.page++;
    await loadMessages(state.activeConversationId, true);
};

const scrollToBottom = () => {
    setTimeout(() => {
        if (messageContainer.value) {
            messageContainer.value.scrollTop = messageContainer.value.scrollHeight;
        }
    }, 50);
};

// SSE
let unsubscribeMessageStream = null;

const setupSSE = () => {
    if (unsubscribeMessageStream) {
        unsubscribeMessageStream();
    }
    unsubscribeMessageStream = subscribeMessageStream(
        // onMessage
        (data) => {
            // 如果当前在对应的会话中，追加消息
            if (state.activeConversationId && data.conversationId === state.activeConversationId) {
                state.messages.push({
                    id: Date.now(),
                    conversationId: data.conversationId,
                    senderId: data.senderId,
                    senderName: data.senderName,
                    senderAvatar: data.senderAvatar,
                    content: data.content,
                    createdAt: data.createdAt,
                    read: false
                });
                scrollToBottom();
            }
            // 刷新会话列表
            loadConversations();
        },
        // onUnread
        () => {}
    );
};

onMounted(async () => {
    await loadConversations();
    setupSSE();

    // 如果路由中有 conversationId，自动选中
    const convId = route.params.conversationId;
    if (convId) {
        const conv = state.conversations.find(c => String(c.id) === convId);
        if (conv) {
            await selectConversation(conv);
        }
    }
});

onUnmounted(() => {
    if (unsubscribeMessageStream) {
        unsubscribeMessageStream();
    }
});

watch(() => state.messages.length, () => {
    if (!state.loadingMore && !state.messagesLoading) {
        scrollToBottom();
    }
});
</script>

<template>
    <div class="messages-page">
        <!-- 会话列表 -->
        <aside class="conversation-list-panel">
            <div class="conversation-list-header">
                <h2>私信</h2>
            </div>
            <div v-if="state.conversationsLoading" class="panel-state-text">加载中...</div>
            <div v-else-if="state.conversationsError" class="panel-state-text error">{{ state.conversationsError }}</div>
            <div v-else-if="!state.conversations.length" class="panel-state-text">暂无会话</div>
            <div v-else class="conversation-list">
                <div
                    v-for="conv in state.conversations"
                    :key="conv.id"
                    class="conversation-item"
                    :class="{ active: state.activeConversationId === conv.id }"
                    @click="selectConversation(conv)"
                >
                    <div class="conv-avatar">
                        <img v-if="conv.participant?.avatarUrl" :src="conv.participant.avatarUrl" alt="">
                        <span v-else class="avatar-placeholder">{{ (conv.participant?.nickname || conv.participant?.username || '?')[0] }}</span>
                        <span v-if="conv.unreadCount > 0" class="unread-dot">{{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}</span>
                    </div>
                    <div class="conv-info">
                        <div class="conv-name">{{ conv.participant?.nickname || conv.participant?.username || '未知用户' }}</div>
                        <div class="conv-preview">{{ conv.lastMessage || '' }}</div>
                    </div>
                    <div class="conv-time" v-if="conv.lastMessageAt">{{ conv.lastMessageAt.slice(5, 16) }}</div>
                </div>
            </div>
        </aside>

        <!-- 会话详情 -->
        <main class="conversation-detail-panel">
            <template v-if="state.activeConversationId">
                <div class="message-header">
                    <button class="back-btn" @click="router.push('/messages')">&larr;</button>
                    <img
                        v-if="otherParticipant?.avatarUrl"
                        class="message-header-avatar"
                        :src="otherParticipant.avatarUrl"
                        alt=""
                    >
                    <span v-else class="message-header-placeholder">{{ (otherParticipant?.nickname || otherParticipant?.username || '?')[0] }}</span>
                    <span class="message-header-name">
                        {{ otherParticipant?.nickname || otherParticipant?.username || '消息' }}
                    </span>
                </div>

                <div ref="messageContainer" class="message-list" @scroll="loadMore">
                    <div v-if="state.messagesLoading" class="panel-state-text">加载中...</div>
                    <div v-else-if="state.messagesError" class="panel-state-text error">{{ state.messagesError }}</div>
                    <template v-else>
                        <div v-if="state.loadingMore" class="panel-state-text loading">加载更多...</div>
                        <div v-if="!state.messages.length" class="panel-state-text">暂无消息，发送第一条消息吧</div>
                        <div
                            v-for="msg in state.messages"
                            :key="msg.id"
                            class="message-item"
                            :class="{ 'message-self': String(msg.senderId) === String(sessionState.user?.id) }"
                        >
                            <img
                                v-if="String(msg.senderId) !== String(sessionState.user?.id)"
                                class="message-avatar"
                                :src="msg.senderAvatar || otherParticipant?.avatarUrl"
                                alt=""
                            >
                            <div class="message-body">
                                <div class="message-bubble">
                                    <div class="message-content">{{ msg.content }}</div>
                                    <div class="message-meta">
                                        <span class="message-time">{{ msg.createdAt ? msg.createdAt.slice(5, 16) : '' }}</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </template>
                </div>

                <div class="message-input-area">
                    <textarea
                        v-model="messageInput"
                        class="message-input"
                        placeholder="输入消息..."
                        rows="2"
                        :disabled="state.sending"
                        @keydown="handleKeydown"
                    ></textarea>
                    <button
                        class="send-btn"
                        :disabled="state.sending || !messageInput.trim()"
                        @click="sendMessage"
                    >发送</button>
                </div>
            </template>

            <div v-else class="no-conversation-selected">
                <p>选择一个会话开始聊天</p>
            </div>
        </main>
    </div>
</template>

<style scoped>
.messages-page {
    display: flex;
    height: calc(100vh - 64px);
    max-width: 1000px;
    margin: 0 auto;
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
    overflow: hidden;
    background: var(--surface);
}

.conversation-list-panel {
    width: 320px;
    flex-shrink: 0;
    border-right: 1px solid var(--line);
    display: flex;
    flex-direction: column;
    background: var(--surface-soft);
}

.conversation-list-header {
    padding: 16px;
    border-bottom: 1px solid var(--line);
}

.conversation-list-header h2 {
    margin: 0;
    font-size: 16px;
    font-weight: 700;
}

.conversation-list {
    flex: 1;
    overflow-y: auto;
}

.conversation-item {
    display: flex;
    gap: 12px;
    padding: 12px 16px;
    cursor: pointer;
    transition: background 0.12s;
    align-items: center;
}

.conversation-item:hover {
    background: var(--surface);
}

.conversation-item.active {
    background: var(--brand-soft);
}

.conv-avatar {
    position: relative;
    width: 40px;
    height: 40px;
    flex-shrink: 0;
}

.conv-avatar img {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    object-fit: cover;
}

.avatar-placeholder {
    display: flex;
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background: var(--brand);
    color: #fff;
    font-size: 14px;
    font-weight: 700;
    align-items: center;
    justify-content: center;
}

.unread-dot {
    position: absolute;
    top: -4px;
    right: -4px;
    min-width: 18px;
    height: 18px;
    padding: 0 4px;
    background: var(--accent);
    color: #fff;
    font-size: 11px;
    font-weight: 700;
    border-radius: 9px;
    display: flex;
    align-items: center;
    justify-content: center;
}

.conv-info {
    flex: 1;
    min-width: 0;
}

.conv-name {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-strong);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.conv-preview {
    font-size: 12px;
    color: var(--muted);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    margin-top: 2px;
}

.conv-time {
    font-size: 11px;
    color: var(--muted);
    flex-shrink: 0;
}

.conversation-detail-panel {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-width: 0;
}

.message-header {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 16px;
    border-bottom: 1px solid var(--line);
    font-weight: 600;
}

.back-btn {
    display: none;
    background: none;
    border: none;
    font-size: 18px;
    cursor: pointer;
    color: var(--text);
    padding: 4px 8px;
}

.message-list {
    flex: 1;
    overflow-y: auto;
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.message-item {
    display: flex;
    max-width: 70%;
}

.message-bubble {
    padding: 10px 14px;
    border-radius: 12px;
    background: var(--surface-soft);
    font-size: 14px;
    line-height: 1.5;
    word-break: break-word;
}

.message-self .message-bubble {
    background: var(--brand);
    color: #fff;
}

.message-meta {
    font-size: 11px;
    margin-top: 4px;
    opacity: 0.7;
}

.message-input-area {
    display: flex;
    gap: 8px;
    padding: 12px 16px;
    border-top: 1px solid var(--line);
    align-items: flex-end;
}

.message-input {
    flex: 1;
    padding: 8px 12px;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    background: var(--surface-soft);
    color: var(--text);
    font: inherit;
    font-size: 14px;
    resize: none;
    outline: 0;
}

.message-input:focus {
    border-color: var(--brand);
}

.send-btn {
    padding: 8px 20px;
    background: var(--brand);
    color: #fff;
    border: none;
    border-radius: var(--radius-sm);
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: opacity 0.12s;
}

.send-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

.send-btn:hover:not(:disabled) {
    opacity: 0.9;
}

.no-conversation-selected {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--muted);
}

.panel-state-text {
    padding: 24px 16px;
    text-align: center;
    color: var(--muted);
    font-size: 14px;
}

.panel-state-text.error {
    color: var(--accent);
}

@media (max-width: 768px) {
    .messages-page {
        height: calc(100vh - 56px);
        border-radius: 0;
        border: none;
    }

    .conversation-list-panel {
        width: 100%;
    }

    .conversation-detail-panel {
        position: fixed;
        inset: 56px 0 0;
        z-index: 10;
        background: var(--surface);
    }

    .back-btn {
        display: inline-flex;
    }
}
</style>
