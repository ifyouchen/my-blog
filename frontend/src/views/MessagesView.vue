<script setup>
import {computed, nextTick, onMounted, onUnmounted, reactive, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {useSession} from '@/stores/session';
import {useToast} from '@/composables/useToast';
import SiteHeader from '@/components/SiteHeader.vue';
import ConfirmDialog from '@/components/ConfirmDialog.vue';
import { useConfirmDialog } from '@/composables/useConfirmDialog';
import {
    createConversationApi,
    deleteConversationApi,
    getConversationsApi,
    getMessagesApi,
    sendMessageApi,
    markMessagesReadApi,
    subscribeMessageStream
} from '@/api/messages';
import EmojiPicker from '@/components/EmojiPicker.vue';
import UserHoverCard from '@/components/UserHoverCard.vue';
import { formatRelativeTime, formatMessageTime } from '@/utils/time';

const route = useRoute();
const router = useRouter();
const { state: sessionState } = useSession();
const toast = useToast();

const {
    confirmDialog,
    openConfirmDialog,
    closeConfirmDialog,
    executeConfirmDialog
} = useConfirmDialog();

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
const messageInputRef = ref(null);
const messageInput = ref('');
const emojiPickerOpen = ref(false);
const emojiPickerRef = ref(null);

const messagesCache = new Map();

const isMine = (msg) => String(msg.senderId) === String(sessionState.user?.id);

const buildUser = (msg) => ({
  id: msg.senderId,
  avatarUrl: msg.senderAvatar || otherParticipant.value?.avatarUrl,
  nickname: msg.senderName || otherParticipant.value?.nickname || otherParticipant.value?.username,
});

const groupedMessages = computed(() => {
  const list = [];
  let prevSenderId = null;
  const TIME_GAP = 5 * 60 * 1000;
  let prevTime = 0;

  for (let i = 0; i < state.messages.length; i++) {
    const msg = state.messages[i];
    const msgTime = msg.createdAt ? new Date(msg.createdAt).getTime() : 0;
    const showTimeDivider = i === 0 || (msgTime - prevTime > TIME_GAP);
    const showAvatar = i === 0
      || msg.senderId !== prevSenderId
      || (msgTime - prevTime > TIME_GAP);

    list.push({ ...msg, showAvatar, showTimeDivider });
    prevSenderId = msg.senderId;
    prevTime = msgTime;
  }
  return list;
});

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
        if (state.activeConversationId) {
            clearConversationUnread(state.activeConversationId);
        }
    } catch (e) {
        state.conversationsError = e.message || '加载会话列表失败';
    } finally {
        state.conversationsLoading = false;
    }
};

const clearConversationUnread = (conversationId) => {
    const conv = state.conversations.find(c => c.id === conversationId);
    if (conv) {
        conv.unreadCount = 0;
    }
};

const refreshHeaderMessageUnread = () => {
    window.dispatchEvent(new CustomEvent('messages:refresh'));
};

const markActiveConversationRead = async (conversationId) => {
    clearConversationUnread(conversationId);
    try {
        await markMessagesReadApi(conversationId);
        clearConversationUnread(conversationId);
        refreshHeaderMessageUnread();
    } catch {
        // 已读状态是即时体验优化，失败时不打断当前聊天。
    }
};

const loadMessages = async (conversationId, append = false) => {
    if (!append) {
        const cached = messagesCache.get(conversationId);
        if (cached) {
            state.messages = cached.messages;
            state.totalMessages = cached.total;
            state.hasMore = cached.hasMore;
            state.page = cached.page;
            state.messagesLoading = false;
            state.messagesError = '';
            nextTick(() => scrollToBottom());
            return;
        }
        state.messagesLoading = true;
        state.messagesError = '';
        state.messages = [];
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
        if (!append) {
            messagesCache.set(conversationId, {
                messages: state.messages,
                total: state.totalMessages,
                hasMore: state.hasMore,
                page: state.page,
            });
        }
        await markActiveConversationRead(conversationId);
    } catch (e) {
        state.messagesError = e.message || '加载消息失败';
    } finally {
        state.messagesLoading = false;
        state.loadingMore = false;
        if (!append) {
            nextTick(() => scrollToBottom());
        }
    }
};

const selectConversation = async (conv) => {
    state.activeConversationId = conv.id;
    await loadMessages(conv.id);
    // 更新未读计数
    clearConversationUnread(conv.id);
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
        messagesCache.delete(state.activeConversationId);
        // 滚动到底部
        scrollToBottom();
    } catch (e) {
        toast.error(e.message || '发送失败');
    } finally {
        state.sending = false;
        // 等 sending 变为 false（textarea 解除禁用）后再聚焦
        await nextTick();
        messageInputRef.value?.focus();
    }
};

const deleteConversation = (conv) => {
    const name = conv.participant?.nickname || conv.participant?.username || '未知用户';
    openConfirmDialog({
        title: '删除会话',
        message: `确定删除与「${name}」的会话吗？删除后聊天记录将不再显示。`,
        confirmText: '确认删除',
        tone: 'danger',
        onConfirm: async () => {
            try {
                await deleteConversationApi(conv.id);
                state.conversations = state.conversations.filter(c => c.id !== conv.id);
                if (state.activeConversationId === conv.id) {
                    state.activeConversationId = null;
                    state.messages = [];
                    await router.replace('/messages');
                }
            } catch (error) {
                toast.error(error.message || '删除会话失败');
            }
        }
    });
};

const handleKeydown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
    }
};

const insertEmoji = (emojiChar) => {
  const textarea = messageInputRef.value;
  if (!textarea) {
    messageInput.value += emojiChar;
    emojiPickerOpen.value = false;
    return;
  }
  const start = textarea.selectionStart;
  const end = textarea.selectionEnd;
  const text = messageInput.value;
  messageInput.value = text.substring(0, start) + emojiChar + text.substring(end);
  emojiPickerOpen.value = false;
  nextTick(() => {
    textarea.selectionStart = textarea.selectionEnd = start + emojiChar.length;
    textarea.focus();
  });
};

const loadMore = async () => {
    if (!state.activeConversationId || state.loadingMore || !state.hasMore) return;
    const el = messageContainer.value;
    if (!el || el.scrollTop > 80) return;
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

const startResize = (e) => {
  const handle = e.currentTarget;
  const inputArea = handle.nextElementSibling;
  const startY = e.clientY;
  const startHeight = inputArea.offsetHeight;

  const onMove = (ev) => {
    const diff = startY - ev.clientY;
    const newHeight = Math.max(60, Math.min(400, startHeight + diff));
    inputArea.style.height = newHeight + 'px';
  };

  const onUp = () => {
    document.removeEventListener('mousemove', onMove);
    document.removeEventListener('mouseup', onUp);
    document.body.style.cursor = '';
    document.body.style.userSelect = '';
  };

  document.body.style.cursor = 'ns-resize';
  document.body.style.userSelect = 'none';
  document.addEventListener('mousemove', onMove);
  document.addEventListener('mouseup', onUp);
};

const handleEmojiPickerClickOutside = (event) => {
  if (emojiPickerOpen.value
      && emojiPickerRef.value
      && !emojiPickerRef.value.contains(event.target)
      && !event.target.closest('.emoji-trigger-btn')) {
    emojiPickerOpen.value = false;
  }
};

// SSE
let unsubscribeMessageStream = null;

const setupSSE = () => {
    if (unsubscribeMessageStream) {
        unsubscribeMessageStream();
    }
    unsubscribeMessageStream = subscribeMessageStream(
        // onMessage
        async (data) => {
            // 如果当前在对应的会话中，追加消息
            if (state.activeConversationId && data.conversationId === state.activeConversationId) {
                state.messages.push({
                    id: data.id || Date.now(),
                    conversationId: data.conversationId,
                    senderId: data.senderId,
                    senderName: data.senderName,
                    senderAvatar: data.senderAvatar,
                    content: data.content,
                    createdAt: data.createdAt,
                    read: true
                });
                await markActiveConversationRead(data.conversationId);
                messagesCache.delete(data.conversationId);
                scrollToBottom();
                await loadConversations();
                clearConversationUnread(data.conversationId);
                return;
            }
            // 刷新会话列表
            await loadConversations();
        },
        // onUnread
        () => {}
    );
};

onMounted(async () => {
    await loadConversations();
    setupSSE();
    document.addEventListener('click', handleEmojiPickerClickOutside);

    // 如果路由中有 conversationId，自动选中
    const convId = route.params.conversationId;
    if (convId) {
        const conv = state.conversations.find(c => String(c.id) === convId);
        if (conv) {
            await selectConversation(conv);
        }
    }
});

watch(() => route.params.conversationId, (newId) => {
    if (!newId) {
        state.activeConversationId = null;
        state.messages = [];
    } else if (!state.activeConversationId) {
        const conv = state.conversations.find(c => String(c.id) === newId);
        if (conv) {
            selectConversation(conv);
        }
    }
});

onUnmounted(() => {
    document.removeEventListener('click', handleEmojiPickerClickOutside);
    if (unsubscribeMessageStream) {
        unsubscribeMessageStream();
    }
});

watch(() => state.messages.length, () => {
    if (!state.loadingMore && !state.messagesLoading) {
        scrollToBottom();
    }
});

watch(() => state.sending, (sending) => {
  if (sending) {
    emojiPickerOpen.value = false;
  }
});

</script>

<template>
    <SiteHeader />
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
                    <!-- 删除按钮 -->
                    <button class="conv-delete-btn" title="删除会话" @click.stop="deleteConversation(conv)">
                        <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                            <path d="M2 4h10M5 4V2.5a.5.5 0 01.5-.5h3a.5.5 0 01.5.5V4M3 4v7.5a1 1 0 001 1h6a1 1 0 001-1V4" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
                        </svg>
                    </button>
                    <div class="conv-time" v-if="conv.lastMessageAt">{{ formatRelativeTime(conv.lastMessageAt) }}</div>
                </div>
            </div>
        </aside>

        <!-- 会话详情 -->
        <main class="conversation-detail-panel" :class="{ 'detail-active': !!state.activeConversationId }">
            <template v-if="state.activeConversationId">
                <div class="message-header">
                    <button class="back-btn" @click="router.push('/messages')">
                        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M15 18l-6-6 6-6"/>
                        </svg>
                    </button>
                    <div class="message-header-info">
                        <UserHoverCard
                            v-if="otherParticipant"
                            :user="otherParticipant"
                            variant="name"
                            name-class="message-header-name"
                            trigger="click"
                        />
                        <span v-else class="message-header-name">消息</span>
                        <span class="message-header-subtitle">在线</span>
                    </div>
                </div>

                <div ref="messageContainer" class="message-list" @scroll="loadMore">
                    <div v-if="state.messagesLoading" class="panel-state-text">加载中...</div>
                    <div v-else-if="state.messagesError" class="panel-state-text error">{{ state.messagesError }}</div>
                    <template v-else>
                        <div v-if="state.loadingMore" class="panel-state-text loading">加载更多...</div>
                        <div v-if="!state.messages.length" class="panel-state-text">暂无消息，发送第一条消息吧</div>
                        <template v-for="msg in groupedMessages" :key="msg.id">
                            <div v-if="msg.showTimeDivider" class="message-time-divider">
                                {{ formatMessageTime(msg.createdAt) }}
                            </div>
                            <div
                                class="message-item"
                                :class="{ 'message-self': isMine(msg) }"
                            >
                                <img
                                    v-if="!isMine(msg) && msg.showAvatar"
                                    class="message-avatar"
                                    :src="msg.senderAvatar || otherParticipant?.avatarUrl"
                                    alt=""
                                >
                                <div v-else-if="!isMine(msg)" class="message-avatar-spacer"></div>
                                <div class="message-body">
                                    <div class="message-bubble">
                                        <div class="message-content">{{ msg.content }}</div>
                                    </div>
                                </div>
                            </div>
                        </template>
                    </template>
                </div>

                <div class="input-resize-handle" @mousedown="startResize">
                    <div class="input-resize-handle-grip"></div>
                </div>
                <div class="message-input-area">
                    <div class="input-toolbar">
                        <div class="input-toolbar-actions">
                            <button
                                class="emoji-trigger-btn"
                                type="button"
                                :disabled="state.sending"
                                title="选择表情"
                                @click.stop="emojiPickerOpen = !emojiPickerOpen"
                            >
                                <svg viewBox="0 0 20 20" width="18" height="18" fill="none">
                                    <circle cx="10" cy="10" r="8" stroke="currentColor" stroke-width="1.4"/>
                                    <circle cx="7" cy="8" r="1" fill="currentColor"/>
                                    <circle cx="13" cy="8" r="1" fill="currentColor"/>
                                    <path d="M6 13c1.2 1.2 2.8 2 4.5 2s3.3-.8 4.5-2" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
                                </svg>
                            </button>
                        </div>
                        <div
                            v-if="emojiPickerOpen"
                            ref="emojiPickerRef"
                            class="emoji-picker-panel"
                            @click.stop
                        >
                            <EmojiPicker @select="insertEmoji" />
                        </div>
                    </div>
                    <div class="input-wrapper">
                        <textarea
                            ref="messageInputRef"
                            v-model="messageInput"
                            class="message-input"
                            placeholder="输入消息..."
                            rows="3"
                            :disabled="state.sending"
                            @keydown="handleKeydown"
                        ></textarea>
                        <button
                            class="send-btn"
                            :disabled="state.sending || !messageInput.trim()"
                            @click="sendMessage"
                        >{{ state.sending ? '发送中...' : '发送' }}</button>
                    </div>
                </div>
            </template>

            <div v-else class="no-conversation-selected">
                <p>选择一个会话开始聊天</p>
            </div>
        </main>
    </div>
    <ConfirmDialog v-bind="confirmDialog" @close="closeConfirmDialog" @confirm="executeConfirmDialog" />
</template>

<style scoped>
.messages-page {
    display: flex;
    height: calc(100vh - 64px);
    max-width: 960px;
    margin: 0 auto;
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
    overflow: hidden;
    background: var(--surface);
}

.conversation-list-panel {
    width: 260px;
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
    position: relative;
}

.conversation-item:hover {
    background: var(--surface);
}

.conversation-item.active {
    background: var(--brand-soft);
}

.conversation-item.active::before {
    content: '';
    position: absolute;
    left: 0;
    top: 4px;
    bottom: 4px;
    width: 3px;
    background: var(--brand);
    border-radius: 0 2px 2px 0;
}

.conv-avatar {
    position: relative;
    width: 44px;
    height: 44px;
    flex-shrink: 0;
}

.conv-avatar img {
    width: 44px;
    height: 44px;
    border-radius: 50%;
    object-fit: cover;
}

.avatar-placeholder {
    display: flex;
    width: 44px;
    height: 44px;
    border-radius: 50%;
    background: var(--brand);
    color: #fff;
    font-size: 16px;
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
    font-size: 15px;
    font-weight: 500;
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
    margin-top: 3px;
}

.conv-time {
    font-size: 11px;
    color: var(--muted);
    flex-shrink: 0;
    align-self: flex-start;
}

.conv-delete-btn {
    display: none;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 24px;
    flex-shrink: 0;
    background: transparent;
    border: none;
    color: var(--muted);
    cursor: pointer;
    border-radius: 4px;
    transition: background 0.1s, color 0.1s;
    opacity: 0.7;
}

.conversation-item:hover .conv-delete-btn {
    display: inline-flex;
}

.conv-delete-btn:hover {
    background: var(--surface);
    color: var(--accent);
    opacity: 1;
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
    gap: 10px;
    padding: 10px 16px;
    border-bottom: 1px solid var(--line);
}

.back-btn {
    display: none;
    align-items: center;
    justify-content: center;
    background: none;
    border: none;
    cursor: pointer;
    color: var(--text);
    padding: 4px;
    border-radius: var(--radius-sm);
    transition: background 0.1s;
}

.back-btn:hover {
    background: var(--surface-soft);
}

.message-header-info {
    display: flex;
    flex-direction: column;
    gap: 1px;
    min-width: 0;
}

.message-header-name {
    font-size: 15px;
    font-weight: 600;
    color: var(--text-strong);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.message-header-subtitle {
    font-size: 11px;
    color: var(--muted);
}

.message-list {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    padding: 12px 16px;
    display: flex;
    flex-direction: column;
    gap: 2px;
}

.message-time-divider {
    text-align: center;
    font-size: 11px;
    color: var(--muted);
    margin: 10px 0 6px;
}

.message-item {
    display: flex;
    gap: 10px;
    max-width: 70%;
    margin-top: 2px;
}

.message-self {
    margin-left: auto;
    flex-direction: row-reverse;
}

.message-avatar {
    width: 44px;
    height: 44px;
    border-radius: 50%;
    object-fit: cover;
    flex-shrink: 0;
    align-self: flex-end;
}

.message-avatar-spacer {
    width: 44px;
    height: 0;
    flex-shrink: 0;
}

.message-body {
    display: flex;
    flex-direction: column;
}

.message-bubble {
    padding: 9px 14px;
    border-radius: 14px;
    background: var(--surface-soft);
    font-size: 14px;
    line-height: 1.5;
    word-break: break-word;
}

.message-self .message-bubble {
    background: var(--brand);
    color: #fff;
    border-bottom-right-radius: 6px;
}

.message-item:not(.message-self) .message-bubble {
    border-bottom-left-radius: 6px;
}

.input-resize-handle {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 6px;
    cursor: ns-resize;
    background: transparent;
    transition: background 0.15s;
    flex-shrink: 0;
}

.input-resize-handle:hover {
    background: var(--surface-soft);
}

.input-resize-handle:active {
    background: var(--surface-soft);
}

.input-resize-handle-grip {
    width: 32px;
    height: 3px;
    background: var(--line);
    border-radius: 2px;
    opacity: 0.5;
    transition: opacity 0.15s;
}

.input-resize-handle:hover .input-resize-handle-grip {
    opacity: 1;
}

.message-input-area {
    border-top: 1px solid var(--line);
    display: flex;
    flex-direction: column;
}

.input-toolbar {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 4px 16px 0;
    min-height: 28px;
    flex-shrink: 0;
}

.input-toolbar-actions {
    display: flex;
    align-items: center;
    gap: 4px;
}

.input-wrapper {
    display: flex;
    gap: 8px;
    padding: 8px 16px 12px;
    align-items: flex-end;
    flex: 1;
    min-height: 0;
}

.message-input {
    flex: 1;
    align-self: stretch;
    padding: 8px 12px;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    background: var(--surface-soft);
    color: var(--text);
    font: inherit;
    font-size: 14px;
    line-height: 1.4;
    resize: none;
    outline: 0;
    min-height: 36px;
}

.message-input:focus {
    border-color: var(--brand);
}

.emoji-trigger-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 26px;
  padding: 0;
  color: var(--muted);
  background: transparent;
  border: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: color 0.15s, background 0.15s;
}

.emoji-trigger-btn:hover:not(:disabled) {
  color: var(--brand);
  background: var(--surface-soft);
}

.emoji-trigger-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.emoji-picker-panel {
  position: absolute;
  bottom: calc(100% + 4px);
  left: 16px;
  z-index: 200;
}

.send-btn {
    padding: 8px 20px;
    min-height: 36px;
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
        max-width: none;
        margin: 0;
        border-radius: 0;
        border: none;
    }

    .conversation-list-panel {
        width: 100%;
    }

    .conversation-detail-panel {
        display: none;
    }
    .conversation-detail-panel.detail-active {
        display: flex;
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
