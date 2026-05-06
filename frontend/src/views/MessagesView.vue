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
import {uploadImageApi} from '@/api/uploads';
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
const messageInputAreaRef = ref(null);
const messageInputRef = ref(null);
const imageFileInputRef = ref(null);
const messageInput = ref('');
const pendingImageFile = ref(null);
const pendingImageUrl = ref('');
const previewImageSrc = ref('');
const previewScale = ref(1);
const emojiPickerOpen = ref(false);
const emojiPickerRef = ref(null);
const inputAreaHeight = ref(null);

const MIN_INPUT_AREA_HEIGHT = 96;
const MAX_INPUT_AREA_HEIGHT = 400;
const MIN_MESSAGE_LIST_HEIGHT = 160;
const IMAGE_MAX_BYTES = 10 * 1024 * 1024;
const PREVIEW_MIN_SCALE = 0.5;
const PREVIEW_MAX_SCALE = 4;
const PREVIEW_SCALE_STEP = 0.25;

const messagesCache = new Map();

const isMine = (msg) => String(msg.senderId) === String(sessionState.user?.id);
const isUploadedImageUrl = (content) => (
    /^\/api\/uploads\/files\/.+\.(png|jpe?g|gif|webp)$/i.test(String(content || '').trim())
);
const isImageMessage = (msg) => (
    String(msg.type || 'TEXT').toUpperCase() === 'IMAGE' || isUploadedImageUrl(msg.content)
);
const inputBusy = computed(() => state.sending);
const canSendMessage = computed(() => Boolean(messageInput.value.trim() || pendingImageFile.value));

const messagePreview = (msg) => isImageMessage(msg) ? '[图片]' : (msg.content || '');
const formatConversationPreview = (content) => isUploadedImageUrl(content) ? '[图片]' : (content || '');

const updateConversationPreview = (conversationId, preview, lastMessageAt) => {
    const conv = state.conversations.find(c => c.id === conversationId);
    if (!conv) return;
    conv.lastMessage = preview;
    conv.lastMessageAt = lastMessageAt || conv.lastMessageAt;
};

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

const messageInputAreaStyle = computed(() => (
    inputAreaHeight.value ? {height: `${inputAreaHeight.value}px`} : null
));
const previewImageStyle = computed(() => ({
    transform: `scale(${previewScale.value})`
}));
const previewScalePercent = computed(() => `${Math.round(previewScale.value * 100)}%`);
const previewImages = computed(() => {
    const images = state.messages
        .filter(isImageMessage)
        .map(getMessageImageSrc)
        .filter(Boolean);
    if (pendingImageUrl.value) {
        images.push(pendingImageUrl.value);
    }
    return images;
});
const previewImageIndex = computed(() => previewImages.value.findIndex(src => src === previewImageSrc.value));
const canSwitchPreviewImage = computed(() => previewImages.value.length > 1);
const previewImagePosition = computed(() => {
    if (!previewImageSrc.value || previewImageIndex.value < 0) {
        return '';
    }
    return `${previewImageIndex.value + 1} / ${previewImages.value.length}`;
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
    const imageFile = pendingImageFile.value;
    if ((!content && !imageFile) || !state.activeConversationId || inputBusy.value) return;
    const conversationId = state.activeConversationId;
    state.sending = true;
    emojiPickerOpen.value = false;
    try {
        let imageUrl = '';
        if (imageFile) {
            const uploaded = await uploadImageApi(imageFile, 'message');
            if (!uploaded?.url) {
                throw new Error('图片上传失败');
            }
            imageUrl = uploaded.url;
        }

        const sentMessages = [];
        if (content) {
            sentMessages.push(await sendMessageApi({
                conversationId,
                content,
                type: 'TEXT'
            }));
        }
        if (imageUrl) {
            sentMessages.push(await sendMessageApi({
                conversationId,
                content: imageUrl,
                type: 'IMAGE'
            }));
        }

        if (state.activeConversationId === conversationId) {
            state.messages.push(...sentMessages);
            scrollToBottom();
        }
        const lastMessage = sentMessages[sentMessages.length - 1];
        if (lastMessage) {
            updateConversationPreview(conversationId, messagePreview(lastMessage), lastMessage.createdAt);
        }
        messageInput.value = '';
        clearPendingImage();
        messagesCache.delete(conversationId);
    } catch (e) {
        toast.error(e.message || '发送失败');
    } finally {
        state.sending = false;
        // 等 sending 变为 false（textarea 解除禁用）后再聚焦
        await nextTick();
        messageInputRef.value?.focus();
    }
};

const setPendingImage = (file) => {
    clearPendingImage();
    pendingImageFile.value = file;
    pendingImageUrl.value = URL.createObjectURL(file);
};

const clearPendingImage = () => {
    if (previewImageSrc.value && previewImageSrc.value === pendingImageUrl.value) {
        closeImagePreview();
    }
    if (pendingImageUrl.value) {
        URL.revokeObjectURL(pendingImageUrl.value);
    }
    pendingImageFile.value = null;
    pendingImageUrl.value = '';
};

const validatePendingImage = (file) => {
    if (!file.type || !file.type.startsWith('image/')) {
        toast.error('请选择图片文件');
        return false;
    }
    if (file.size > IMAGE_MAX_BYTES) {
        toast.error('私信图片不能超过 10MB');
        return false;
    }
    return true;
};

const getPastedImageFile = (clipboardData) => {
    const items = Array.from(clipboardData?.items || []);
    for (const item of items) {
        if (item.kind === 'file' && item.type.startsWith('image/')) {
            return item.getAsFile();
        }
    }
    const files = Array.from(clipboardData?.files || []);
    return files.find(file => file.type?.startsWith('image/')) || null;
};

const handlePaste = (event) => {
    if (inputBusy.value) return;
    const file = getPastedImageFile(event.clipboardData);
    if (!file) return;
    event.preventDefault();
    if (validatePendingImage(file)) {
        setPendingImage(file);
        emojiPickerOpen.value = false;
        nextTick(() => messageInputRef.value?.focus());
    }
};

const handleDropImage = (event) => {
    if (inputBusy.value) return;
    const file = Array.from(event.dataTransfer?.files || [])
        .find(item => item.type?.startsWith('image/'));
    if (!file) return;
    event.preventDefault();
    if (validatePendingImage(file)) {
        setPendingImage(file);
        nextTick(() => messageInputRef.value?.focus());
    }
};

const triggerImagePicker = () => {
    if (inputBusy.value) return;
    emojiPickerOpen.value = false;
    if (imageFileInputRef.value) {
        imageFileInputRef.value.value = '';
        imageFileInputRef.value.click();
    }
};

const handleImageFileChange = (event) => {
    if (inputBusy.value) return;
    const file = Array.from(event.target?.files || [])
        .find(item => item.type?.startsWith('image/'));
    if (file && validatePendingImage(file)) {
        setPendingImage(file);
        nextTick(() => messageInputRef.value?.focus());
    }
    if (event.target) {
        event.target.value = '';
    }
};

const handleDragOverImage = (event) => {
    const hasImage = Array.from(event.dataTransfer?.items || [])
        .some(item => item.kind === 'file' && item.type.startsWith('image/'));
    if (hasImage) {
        event.preventDefault();
    }
};

const handleBackspacePendingImage = (event) => {
    if (event.key === 'Backspace' && !messageInput.value && pendingImageFile.value) {
        event.preventDefault();
        clearPendingImage();
        return;
    }

    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        sendMessage();
    }
};

const getMessageImageSrc = (msg) => {
    const content = String(msg.content || '').trim();
    if (content) {
        return content;
    }
    return '';
};

const openImagePreview = (src) => {
    if (!src) return;
    previewImageSrc.value = src;
    resetPreviewZoom();
};

const closeImagePreview = () => {
    previewImageSrc.value = '';
    resetPreviewZoom();
};

const switchPreviewImage = (direction) => {
    const images = previewImages.value;
    if (!previewImageSrc.value || images.length < 2) return;
    const currentIndex = previewImageIndex.value >= 0 ? previewImageIndex.value : 0;
    const nextIndex = (currentIndex + direction + images.length) % images.length;
    previewImageSrc.value = images[nextIndex];
    resetPreviewZoom();
};

const showPreviousPreviewImage = () => {
    switchPreviewImage(-1);
};

const showNextPreviewImage = () => {
    switchPreviewImage(1);
};

const handlePreviewKeydown = (event) => {
    if (event.key === 'Escape' && previewImageSrc.value) {
        closeImagePreview();
    } else if ((event.key === 'ArrowLeft' || event.key === 'ArrowUp') && previewImageSrc.value) {
        event.preventDefault();
        showPreviousPreviewImage();
    } else if ((event.key === 'ArrowRight' || event.key === 'ArrowDown') && previewImageSrc.value) {
        event.preventDefault();
        showNextPreviewImage();
    } else if ((event.key === '+' || event.key === '=') && previewImageSrc.value) {
        event.preventDefault();
        zoomPreview(PREVIEW_SCALE_STEP);
    } else if ((event.key === '-' || event.key === '_') && previewImageSrc.value) {
        event.preventDefault();
        zoomPreview(-PREVIEW_SCALE_STEP);
    } else if (event.key === '0' && previewImageSrc.value) {
        event.preventDefault();
        resetPreviewZoom();
    }
};

const clampPreviewScale = (value) => Math.min(PREVIEW_MAX_SCALE, Math.max(PREVIEW_MIN_SCALE, value));

const zoomPreview = (delta) => {
    previewScale.value = clampPreviewScale(Number((previewScale.value + delta).toFixed(2)));
};

const resetPreviewZoom = () => {
    previewScale.value = 1;
};

const handlePreviewWheel = (event) => {
    zoomPreview(event.deltaY > 0 ? -PREVIEW_SCALE_STEP : PREVIEW_SCALE_STEP);
};

const handleImageLoad = () => {
    scrollToBottom();
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

const handleKeydown = handleBackspacePendingImage;

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
        scrollToBottomNow();
    }, 50);
};

const scrollToBottomNow = () => {
    if (messageContainer.value) {
        messageContainer.value.scrollTop = messageContainer.value.scrollHeight;
    }
};

const isMessageListNearBottom = () => {
    const el = messageContainer.value;
    if (!el) return false;
    return el.scrollHeight - el.scrollTop - el.clientHeight < 32;
};

const getMaxInputAreaHeight = () => {
    const panel = messageInputAreaRef.value?.closest('.conversation-detail-panel');
    const header = panel?.querySelector('.message-header');
    const handle = panel?.querySelector('.input-resize-handle');
    if (!panel || !header || !handle) {
        return MAX_INPUT_AREA_HEIGHT;
    }

    const availableHeight = panel.clientHeight
        - header.offsetHeight
        - handle.offsetHeight
        - MIN_MESSAGE_LIST_HEIGHT;
    return Math.max(MIN_INPUT_AREA_HEIGHT, Math.min(MAX_INPUT_AREA_HEIGHT, availableHeight));
};

const startResize = (e) => {
    e.preventDefault();
    const inputArea = messageInputAreaRef.value;
    if (!inputArea) return;

    const startY = e.clientY;
    const startHeight = inputArea.offsetHeight;
    const keepPinnedToBottom = isMessageListNearBottom();

    const onMove = (ev) => {
        const diff = startY - ev.clientY;
        const maxHeight = getMaxInputAreaHeight();
        inputAreaHeight.value = Math.max(
            MIN_INPUT_AREA_HEIGHT,
            Math.min(maxHeight, startHeight + diff)
        );

        if (keepPinnedToBottom) {
            nextTick(() => scrollToBottomNow());
        }
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
                    type: data.type || 'TEXT',
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
    document.addEventListener('keydown', handlePreviewKeydown);

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
    document.removeEventListener('keydown', handlePreviewKeydown);
    clearPendingImage();
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
                        <div class="conv-preview">{{ formatConversationPreview(conv.lastMessage) }}</div>
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
                                    <div class="message-bubble" :class="{ 'message-image-bubble': isImageMessage(msg) }">
                                        <button
                                            v-if="isImageMessage(msg)"
                                            class="message-image-button"
                                            type="button"
                                            title="查看大图"
                                            @click="openImagePreview(getMessageImageSrc(msg))"
                                        >
                                            <img
                                                class="message-image"
                                                :src="getMessageImageSrc(msg)"
                                                alt="图片消息"
                                                loading="lazy"
                                                @load="handleImageLoad"
                                            >
                                        </button>
                                        <div v-else class="message-content">{{ msg.content }}</div>
                                    </div>
                                </div>
                            </div>
                        </template>
                    </template>
                </div>

                <div class="input-resize-handle" @mousedown="startResize">
                    <div class="input-resize-handle-grip"></div>
                </div>
                <div
                    ref="messageInputAreaRef"
                    class="message-input-area"
                    :class="{ 'has-pending-image': pendingImageUrl }"
                    :style="messageInputAreaStyle"
                    @dragover="handleDragOverImage"
                    @drop="handleDropImage"
                >
                    <div class="input-toolbar">
                        <div class="input-toolbar-actions">
                            <button
                                class="emoji-trigger-btn"
                                type="button"
                                :disabled="inputBusy"
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
                            <button
                                class="image-upload-btn"
                                :class="{ active: pendingImageUrl }"
                                type="button"
                                :disabled="inputBusy"
                                :title="pendingImageUrl ? '更换图片' : '上传图片'"
                                @click="triggerImagePicker"
                            >
                                <svg viewBox="0 0 20 20" width="18" height="18" fill="none">
                                    <rect
                                        x="3"
                                        y="4"
                                        width="14"
                                        height="12"
                                        rx="2"
                                        stroke="currentColor"
                                        stroke-width="1.4"
                                    />
                                    <circle cx="7.2" cy="8" r="1.3" fill="currentColor"/>
                                    <path
                                        d="M5 14l3.5-3.5 2.4 2.4 2-2L17 14"
                                        stroke="currentColor"
                                        stroke-width="1.4"
                                        stroke-linecap="round"
                                        stroke-linejoin="round"
                                    />
                                </svg>
                            </button>
                            <input
                                ref="imageFileInputRef"
                                class="image-file-input"
                                type="file"
                                accept="image/*"
                                :disabled="inputBusy"
                                @change="handleImageFileChange"
                            >
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
                        <div class="message-input-shell">
                            <div v-if="pendingImageUrl" class="pending-image-preview">
                                <button
                                    class="pending-image-preview-button"
                                    type="button"
                                    title="查看大图"
                                    @click="openImagePreview(pendingImageUrl)"
                                >
                                    <img :src="pendingImageUrl" alt="待发送图片">
                                </button>
                                <button
                                    class="pending-image-remove"
                                    type="button"
                                    title="移除图片"
                                    :disabled="inputBusy"
                                    @click="clearPendingImage"
                                >
                                    <svg viewBox="0 0 16 16" width="14" height="14" fill="none">
                                        <path d="M4 4l8 8M12 4l-8 8" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>
                                    </svg>
                                </button>
                            </div>
                            <textarea
                                ref="messageInputRef"
                                v-model="messageInput"
                                class="message-input"
                                placeholder="输入消息..."
                                rows="3"
                                :disabled="inputBusy"
                                @paste="handlePaste"
                                @keydown="handleKeydown"
                            ></textarea>
                        </div>
                        <button
                            class="send-btn"
                            :class="{ sending: state.sending }"
                            :disabled="inputBusy || !canSendMessage"
                            @click="sendMessage"
                        >{{ state.sending ? '发送中' : '发送' }}</button>
                    </div>
                </div>
            </template>

            <div v-else class="no-conversation-selected">
                <p>选择一个会话开始聊天</p>
            </div>
        </main>
    </div>
    <ConfirmDialog v-bind="confirmDialog" @close="closeConfirmDialog" @confirm="executeConfirmDialog" />
    <div
        v-if="previewImageSrc"
        class="image-preview-overlay"
        @click.self="closeImagePreview"
        @wheel.prevent="handlePreviewWheel"
    >
        <div class="image-preview-toolbar">
            <span v-if="previewImagePosition" class="image-preview-count">{{ previewImagePosition }}</span>
            <button
                class="image-preview-tool"
                type="button"
                title="缩小"
                :disabled="previewScale <= PREVIEW_MIN_SCALE"
                @click="zoomPreview(-PREVIEW_SCALE_STEP)"
            >
                <svg viewBox="0 0 20 20" width="18" height="18" fill="none">
                    <path d="M5 10h10" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
                </svg>
            </button>
            <button
                class="image-preview-scale"
                type="button"
                title="恢复原始大小"
                @click="resetPreviewZoom"
            >{{ previewScalePercent }}</button>
            <button
                class="image-preview-tool"
                type="button"
                title="放大"
                :disabled="previewScale >= PREVIEW_MAX_SCALE"
                @click="zoomPreview(PREVIEW_SCALE_STEP)"
            >
                <svg viewBox="0 0 20 20" width="18" height="18" fill="none">
                    <path d="M10 5v10M5 10h10" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
                </svg>
            </button>
        </div>
        <button
            v-if="canSwitchPreviewImage"
            class="image-preview-nav image-preview-prev"
            type="button"
            title="上一张"
            @click="showPreviousPreviewImage"
        >
            <svg viewBox="0 0 24 24" width="28" height="28" fill="none">
                <path d="M15 18l-6-6 6-6" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
        </button>
        <button
            v-if="canSwitchPreviewImage"
            class="image-preview-nav image-preview-next"
            type="button"
            title="下一张"
            @click="showNextPreviewImage"
        >
            <svg viewBox="0 0 24 24" width="28" height="28" fill="none">
                <path d="M9 18l6-6-6-6" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
        </button>
        <button
            class="image-preview-close"
            type="button"
            title="关闭预览"
            @click="closeImagePreview"
        >
            <svg viewBox="0 0 20 20" width="20" height="20" fill="none">
                <path d="M5 5l10 10M15 5L5 15" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            </svg>
        </button>
        <div class="image-preview-stage">
            <img
                class="image-preview-full"
                :src="previewImageSrc"
                :style="previewImageStyle"
                alt="图片预览"
            >
        </div>
    </div>
</template>

<style scoped>
.messages-page {
    display: flex;
    height: calc(100vh - 64px);
    max-width: 1080px;
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
    overflow: hidden;
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
    flex: 1 1 auto;
    min-height: 0;
    overflow-y: auto;
    scrollbar-gutter: stable;
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
    background: #eaf3ff;
    color: var(--text-strong);
    box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.12);
    border-bottom-right-radius: 6px;
}

.message-item:not(.message-self) .message-bubble {
    border-bottom-left-radius: 6px;
}

.message-image-bubble {
    padding: 4px;
    overflow: hidden;
    background: transparent;
    box-shadow: none;
}

.message-self .message-image-bubble {
    background: transparent;
    box-shadow: none;
}

.message-image-button {
    display: block;
    padding: 0;
    border: 0;
    background: transparent;
    cursor: zoom-in;
}

.message-image-button:focus-visible {
    outline: 2px solid var(--brand);
    outline-offset: 3px;
    border-radius: 12px;
}

.message-image {
    display: block;
    max-width: min(280px, 58vw);
    max-height: 320px;
    border-radius: 12px;
    object-fit: contain;
    background: var(--surface-soft);
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
    position: relative;
    z-index: 2;
    border-top: 1px solid var(--line);
    display: flex;
    flex-direction: column;
    flex: 0 0 auto;
    min-height: 96px;
    max-height: min(400px, calc(100% - 220px));
    overflow: visible;
}

.message-input-area.has-pending-image {
    min-height: 168px;
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

.message-input-shell {
    flex: 1;
    align-self: stretch;
    display: flex;
    flex-direction: column;
    min-width: 0;
    min-height: 36px;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    background: var(--surface-soft);
    overflow: hidden;
}

.message-input-shell:focus-within {
    border-color: var(--brand);
}

.pending-image-preview {
    position: relative;
    width: 96px;
    height: 72px;
    margin: 8px 8px 0;
    border: 1px solid var(--line);
    border-radius: 8px;
    overflow: hidden;
    background: var(--surface);
    flex-shrink: 0;
}

.pending-image-preview img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
}

.pending-image-preview-button {
    display: block;
    width: 100%;
    height: 100%;
    padding: 0;
    border: 0;
    background: transparent;
    cursor: zoom-in;
}

.pending-image-remove {
    position: absolute;
    top: 4px;
    right: 4px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 22px;
    height: 22px;
    padding: 0;
    color: #fff;
    background: rgba(15, 23, 42, 0.72);
    border: none;
    border-radius: 50%;
    cursor: pointer;
}

.pending-image-remove:hover:not(:disabled) {
    background: rgba(15, 23, 42, 0.88);
}

.message-input {
    flex: 1;
    padding: 8px 12px;
    border: 0;
    border-radius: 0;
    background: transparent;
    color: var(--text);
    font: inherit;
    font-size: 14px;
    line-height: 1.4;
    resize: none;
    outline: 0;
    min-height: 36px;
}

.image-file-input {
    position: absolute;
    width: 1px;
    height: 1px;
    overflow: hidden;
    clip: rect(0 0 0 0);
    clip-path: inset(50%);
    white-space: nowrap;
}

.emoji-trigger-btn,
.image-upload-btn {
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

.emoji-trigger-btn:hover:not(:disabled),
.image-upload-btn:hover:not(:disabled),
.image-upload-btn.active {
  color: var(--brand);
  background: var(--surface-soft);
}

.emoji-trigger-btn:disabled,
.image-upload-btn:disabled {
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
    width: 76px;
    padding: 8px 20px;
    min-height: 36px;
    background: var(--brand);
    color: #fff;
    border: none;
    border-radius: var(--radius-sm);
    font-size: 14px;
    font-weight: 600;
    white-space: nowrap;
    cursor: pointer;
    transition: background 0.12s;
}

.send-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

.send-btn.sending {
    opacity: 1;
    cursor: wait;
}

.send-btn:hover:not(:disabled) {
    background: var(--brand-strong);
}

.image-preview-overlay {
    position: fixed;
    inset: 0;
    z-index: 1000;
    padding: 48px;
    background: rgba(15, 23, 42, 0.78);
}

.image-preview-stage {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
}

.image-preview-full {
    max-width: min(92vw, 1120px);
    max-height: 88vh;
    object-fit: contain;
    border-radius: 10px;
    background: var(--surface);
    box-shadow: 0 24px 80px rgba(15, 23, 42, 0.36);
    transition: transform 0.12s ease-out;
    transform-origin: center center;
}

.image-preview-close {
    position: fixed;
    top: 20px;
    right: 20px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    padding: 0;
    color: #fff;
    background: rgba(15, 23, 42, 0.68);
    border: 1px solid rgba(255, 255, 255, 0.24);
    border-radius: 50%;
    cursor: pointer;
}

.image-preview-close:hover {
    background: rgba(15, 23, 42, 0.86);
}

.image-preview-toolbar {
    position: fixed;
    top: 20px;
    left: 50%;
    z-index: 1001;
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 6px;
    background: rgba(15, 23, 42, 0.68);
    border: 1px solid rgba(255, 255, 255, 0.24);
    border-radius: 999px;
    transform: translateX(-50%);
}

.image-preview-count {
    min-width: 48px;
    padding: 0 8px;
    color: rgba(255, 255, 255, 0.9);
    font-size: 13px;
    font-weight: 600;
    text-align: center;
}

.image-preview-tool,
.image-preview-scale {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    height: 32px;
    color: #fff;
    background: transparent;
    border: none;
    border-radius: 999px;
    cursor: pointer;
}

.image-preview-tool {
    width: 32px;
    padding: 0;
}

.image-preview-scale {
    min-width: 58px;
    padding: 0 10px;
    font-size: 13px;
    font-weight: 600;
}

.image-preview-tool:hover:not(:disabled),
.image-preview-scale:hover {
    background: rgba(255, 255, 255, 0.12);
}

.image-preview-tool:disabled {
    opacity: 0.36;
    cursor: not-allowed;
}

.image-preview-nav {
    position: fixed;
    top: 50%;
    z-index: 1001;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 48px;
    height: 64px;
    padding: 0;
    color: #fff;
    background: rgba(15, 23, 42, 0.58);
    border: 1px solid rgba(255, 255, 255, 0.22);
    border-radius: 999px;
    cursor: pointer;
    transform: translateY(-50%);
}

.image-preview-nav:hover {
    background: rgba(15, 23, 42, 0.82);
}

.image-preview-prev {
    left: 24px;
}

.image-preview-next {
    right: 24px;
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

    .image-preview-overlay {
        padding: 18px;
    }

    .image-preview-full {
        max-width: 96vw;
        max-height: 86vh;
        border-radius: 8px;
    }

    .image-preview-close {
        top: 12px;
        right: 12px;
    }

    .image-preview-toolbar {
        top: 12px;
    }

    .image-preview-count {
        min-width: 40px;
        padding: 0 4px;
    }

    .image-preview-nav {
        width: 40px;
        height: 54px;
    }

    .image-preview-prev {
        left: 10px;
    }

    .image-preview-next {
        right: 10px;
    }
}
</style>
