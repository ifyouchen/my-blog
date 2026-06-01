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
    recallMessageApi,
    markMessagesReadApi,
    subscribeMessageStream
} from '@/api/messages';
import {uploadImage} from '@/api/uploads';
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
const previewStageRef = ref(null);
const previewImageRef = ref(null);
const messageInput = ref('');
const pendingImageFile = ref(null);
const pendingImageUrl = ref('');
const previewImageSrc = ref('');
const previewScale = ref(1);
const previewOffset = ref({ x: 0, y: 0 });
const previewDragging = ref(false);
let previewDragStart = null;
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
const PREVIEW_MIN_VISIBLE_EDGE = 56;
const RECALL_TIME_LIMIT_MS = 10 * 60 * 1000;

const replyingTo = ref(null);

const contextMenu = reactive({
    visible: false,
    x: 0,
    y: 0,
    message: null
});

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

const canRecall = (msg) => {
    if (!msg || !isMine(msg) || msg.recalled) return false;
    const elapsed = Date.now() - new Date(msg.createdAt).getTime();
    return elapsed < RECALL_TIME_LIMIT_MS;
};

const startReply = (msg) => {
    replyingTo.value = msg;
    messageInputRef.value?.focus();
};

const replyPreview = (msg) => {
    if (!msg) return '';
    if (msg.type === 'IMAGE' || /\.(png|jpe?g|gif|webp)/i.test(msg.content)) return '[图片]';
    return msg.content.substring(0, 60);
};

const handleRecall = async (msg) => {
    try {
        const result = await recallMessageApi(msg.id);
        const idx = state.messages.findIndex(m => m.id === msg.id);
        if (idx >= 0) {
            state.messages[idx] = { ...state.messages[idx], ...result };
        }
        messagesCache.delete(state.activeConversationId);
    } catch (e) {
        toast.error(e.message || '撤回失败');
    }
};

const scrollToMessage = (messageId) => {
    nextTick(() => {
        const el = messageContainer.value?.querySelector(`[data-msg-id="${messageId}"]`);
        if (el) {
            el.scrollIntoView({ behavior: 'smooth', block: 'center' });
            el.classList.add('highlight-flash');
            setTimeout(() => el.classList.remove('highlight-flash'), 1500);
        }
    });
};

const showContextMenu = (event, msg) => {
    event.preventDefault();
    contextMenu.visible = true;
    contextMenu.x = event.clientX;
    contextMenu.y = event.clientY;
    contextMenu.message = msg;
};

const closeContextMenu = () => {
    contextMenu.visible = false;
    contextMenu.message = null;
};

const messagePreview = (msg) => isImageMessage(msg) ? '[图片]' : (msg.content || '');
const formatConversationPreview = (content) => isUploadedImageUrl(content) ? '[图片]' : (content || '');

const updateConversationPreview = (conversationId, preview, lastMessageAt) => {
    const conv = state.conversations.find(c => String(c.id) === String(conversationId));
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
const formatPresenceText = (user) => {
    if (!user) return '';
    if (user.online) return '在线';
    const relative = formatRelativeTime(user.lastSeenAt);
    return relative ? `最后在线 ${relative}` : '离线';
};
const otherParticipantPresenceText = computed(() => formatPresenceText(otherParticipant.value));

const updateParticipantPresence = (presence) => {
    const userId = presence?.userId;
    if (!userId) return;
    state.conversations = state.conversations.map((conv) => {
        if (String(conv.participant?.id) !== String(userId)) {
            return conv;
        }
        return {
            ...conv,
            participant: {
                ...conv.participant,
                online: Boolean(presence.online),
                lastSeenAt: presence.lastSeenAt || conv.participant.lastSeenAt
            }
        };
    });
};

const messageInputAreaStyle = computed(() => (
    inputAreaHeight.value ? {height: `${inputAreaHeight.value}px`} : null
));
const previewImageStyle = computed(() => ({
    transform: `translate(${previewOffset.value.x}px, ${previewOffset.value.y}px) scale(${previewScale.value})`
}));
const canDragPreviewImage = computed(() => Boolean(previewImageSrc.value));
const previewImageClass = computed(() => ({
    'is-draggable': canDragPreviewImage.value,
    'is-dragging': previewDragging.value
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

const loadConversations = async ({silent = false} = {}) => {
    if (!silent) {
        state.conversationsLoading = true;
        state.conversationsError = '';
    }
    try {
        const result = await getConversationsApi();
        state.conversations = result.items || [];
        if (state.activeConversationId) {
            clearConversationUnread(state.activeConversationId);
        }
    } catch (e) {
        if (!silent) {
            state.conversationsError = e.message || '加载会话列表失败';
        }
    } finally {
        if (!silent) {
            state.conversationsLoading = false;
        }
    }
};

const clearConversationUnread = (conversationId) => {
    const conv = state.conversations.find(c => String(c.id) === String(conversationId));
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
            const uploaded = await uploadImage(imageFile, 'message');
            if (!uploaded?.url) {
                throw new Error('图片上传失败');
            }
            imageUrl = uploaded.mediumUrl || uploaded.url;
        }

        const sentMessages = [];
        if (content) {
            sentMessages.push(await sendMessageApi({
                conversationId,
                content,
                type: 'TEXT',
                parentId: replyingTo.value?.id
            }));
        }
        if (imageUrl) {
            sentMessages.push(await sendMessageApi({
                conversationId,
                content: imageUrl,
                type: 'IMAGE',
                parentId: replyingTo.value?.id
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
        replyingTo.value = null;
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
    stopPreviewDrag();
    previewImageSrc.value = '';
    resetPreviewZoom();
    previewOffset.value = { x: 0, y: 0 };
};

const switchPreviewImage = (direction) => {
    const images = previewImages.value;
    if (!previewImageSrc.value || images.length < 2) return;
    stopPreviewDrag();
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

const clampPreviewOffset = (offset) => {
    const stage = previewStageRef.value;
    const image = previewImageRef.value;
    if (!stage || !image) {
        return { x: 0, y: 0 };
    }
    const stageRect = stage.getBoundingClientRect();
    const scaledWidth = image.offsetWidth * previewScale.value;
    const scaledHeight = image.offsetHeight * previewScale.value;
    const visibleWidth = Math.min(PREVIEW_MIN_VISIBLE_EDGE, scaledWidth / 2);
    const visibleHeight = Math.min(PREVIEW_MIN_VISIBLE_EDGE, scaledHeight / 2);
    const maxX = Math.max(0, (scaledWidth + stageRect.width) / 2 - visibleWidth);
    const maxY = Math.max(0, (scaledHeight + stageRect.height) / 2 - visibleHeight);
    return {
        x: Math.min(maxX, Math.max(-maxX, offset.x)),
        y: Math.min(maxY, Math.max(-maxY, offset.y))
    };
};

const zoomPreview = (delta) => {
    previewScale.value = clampPreviewScale(Number((previewScale.value + delta).toFixed(2)));
    previewOffset.value = clampPreviewOffset(previewOffset.value);
};

const resetPreviewZoom = () => {
    previewScale.value = 1;
    previewOffset.value = { x: 0, y: 0 };
};

const handlePreviewWheel = (event) => {
    zoomPreview(event.deltaY > 0 ? -PREVIEW_SCALE_STEP : PREVIEW_SCALE_STEP);
};

const startPreviewDrag = (event) => {
    if (!canDragPreviewImage.value || event.button !== 0) return;
    event.preventDefault();
    event.stopPropagation();
    previewDragging.value = true;
    previewDragStart = {
        clientX: event.clientX,
        clientY: event.clientY,
        offset: { ...previewOffset.value }
    };
    event.currentTarget?.setPointerCapture?.(event.pointerId);
    window.addEventListener('pointermove', handlePreviewDragMove, { passive: false });
    window.addEventListener('pointerup', stopPreviewDrag);
    window.addEventListener('pointercancel', stopPreviewDrag);
};

const handlePreviewDragMove = (event) => {
    if (!previewDragging.value || !previewDragStart) return;
    event.preventDefault();
    previewOffset.value = {
        x: previewDragStart.offset.x + event.clientX - previewDragStart.clientX,
        y: previewDragStart.offset.y + event.clientY - previewDragStart.clientY
    };
    previewOffset.value = clampPreviewOffset(previewOffset.value);
};

const stopPreviewDrag = () => {
    previewDragging.value = false;
    previewDragStart = null;
    window.removeEventListener('pointermove', handlePreviewDragMove);
    window.removeEventListener('pointerup', stopPreviewDrag);
    window.removeEventListener('pointercancel', stopPreviewDrag);
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
let pollInterval = null;
let sseFallbackTimer = null;
let sseConnected = false;

const POLL_INTERVAL_MS = 5000;
const SSE_FALLBACK_DELAY_MS = 3000;

const clearSseFallbackTimer = () => {
    if (sseFallbackTimer) {
        clearTimeout(sseFallbackTimer);
        sseFallbackTimer = null;
    }
};

const hasMessage = (messageId) => state.messages.some(
    message => String(message.id) === String(messageId)
);

const refreshActiveConversationMessages = async () => {
    if (!state.activeConversationId) return;
    const conversationId = state.activeConversationId;
    try {
        const fullResult = await getMessagesApi({
            conversationId,
            page: 1,
            pageSize: state.pageSize
        });
        if (state.activeConversationId !== conversationId) return;
        const fullItems = fullResult.items || [];
        state.page = 1;
        state.messages = fullItems.reverse();
        state.totalMessages = fullResult.total || 0;
        state.hasMore = state.messages.length < state.totalMessages;
        messagesCache.delete(conversationId);
        await markActiveConversationRead(conversationId);
        scrollToBottom();
    } catch {
        // 兜底刷新失败时保持当前页面状态，等待下一次事件或轮询。
    }
};

const schedulePollingFallback = () => {
    if (pollInterval || sseFallbackTimer) return;
    sseFallbackTimer = setTimeout(async () => {
        sseFallbackTimer = null;
        if (sseConnected) return;
        await refreshActiveConversationMessages();
        startPolling();
    }, SSE_FALLBACK_DELAY_MS);
};

const setupSSE = () => {
    if (unsubscribeMessageStream) {
        unsubscribeMessageStream();
    }
    unsubscribeMessageStream = subscribeMessageStream(
        // onMessage
        async (data) => {
            // 如果当前在对应的会话中，追加消息
            if (state.activeConversationId && data.conversationId === state.activeConversationId) {
                if (data.id && hasMessage(data.id)) {
                    await markActiveConversationRead(data.conversationId);
                    await loadConversations({silent: true});
                    clearConversationUnread(data.conversationId);
                    return;
                }
                const msg = {
                    id: data.id || Date.now(),
                    conversationId: data.conversationId,
                    senderId: data.senderId,
                    senderName: data.senderName,
                    senderAvatar: data.senderAvatar,
                    content: data.content,
                    type: data.type || 'TEXT',
                    parentId: data.parentId,
                    repliedMessage: data.repliedMessage,
                    createdAt: data.createdAt,
                    read: true
                };
                state.messages.push(msg);
                await markActiveConversationRead(data.conversationId);
                messagesCache.delete(data.conversationId);
                updateConversationPreview(data.conversationId, messagePreview(msg), msg.createdAt);
                scrollToBottom();
                await loadConversations({silent: true});
                clearConversationUnread(data.conversationId);
                return;
            }
            // 刷新会话列表
            await loadConversations({silent: true});
        },
        // onUnread
        () => {},
        // onRecall
        (data) => {
            if (state.activeConversationId && data.conversationId === state.activeConversationId) {
                const idx = state.messages.findIndex(m => m.id === data.id);
                if (idx >= 0) {
                    state.messages[idx].recalled = true;
                    state.messages[idx].recalledAt = data.recalledAt;
                    state.messages[idx].content = '对方撤回了一条消息';
                }
                messagesCache.delete(data.conversationId);
            }
            loadConversations({silent: true});
        },
        {
            onOpen: async () => {
                sseConnected = true;
                clearSseFallbackTimer();
                clearPolling();
                await refreshActiveConversationMessages();
                await loadConversations({silent: true});
            },
            onPresence: updateParticipantPresence,
            onError: () => {
                sseConnected = false;
                schedulePollingFallback();
            }
        }
    );
};

// Polling fallback — 当 SSE 连接意外断开时定时拉取最新消息
const startPolling = () => {
    clearPolling();
    pollInterval = setInterval(async () => {
        if (!state.activeConversationId) return;
        try {
            const result = await getMessagesApi({
                conversationId: state.activeConversationId,
                page: 1,
                pageSize: 1
            });
            const items = result.items || [];
            if (items.length > 0) {
                const latestRemote = items[0]; // API 返回 DESC，index 0 是最新消息
                const latestLocal = state.messages.length > 0 ? state.messages[state.messages.length - 1] : null;
                if (!latestLocal || String(latestRemote.id) !== String(latestLocal.id)) {
                    await refreshActiveConversationMessages();
                }
            }
        } catch {
            // 静默失败，下次轮询再试
        }
    }, POLL_INTERVAL_MS);
};

const clearPolling = () => {
    if (pollInterval) {
        clearInterval(pollInterval);
        pollInterval = null;
    }
};

onMounted(async () => {
    await loadConversations();
    setupSSE();
    document.addEventListener('click', handleEmojiPickerClickOutside);
    document.addEventListener('click', closeContextMenu);
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
    document.removeEventListener('click', closeContextMenu);
    document.removeEventListener('keydown', handlePreviewKeydown);
    stopPreviewDrag();
    clearPendingImage();
    clearSseFallbackTimer();
    clearPolling();
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
                        <div class="conv-name-row">
                            <span class="conv-name">{{ conv.participant?.nickname || conv.participant?.username || '未知用户' }}</span>
                            <span
                                class="conv-presence-dot"
                                :class="{ online: conv.participant?.online }"
                                :title="formatPresenceText(conv.participant)"
                            ></span>
                        </div>
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
                        <span
                            v-if="otherParticipantPresenceText"
                            class="message-header-subtitle"
                            :class="{ online: otherParticipant?.online }"
                        >
                            {{ otherParticipantPresenceText }}
                        </span>
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
                                :data-msg-id="msg.id"
                                @contextmenu.prevent="showContextMenu($event, msg)"
                            >
                                <img
                                    v-if="!isMine(msg) && msg.showAvatar"
                                    class="message-avatar"
                                    :src="msg.senderAvatar || otherParticipant?.avatarUrl"
                                    alt=""
                                >
                                <div v-else-if="!isMine(msg)" class="message-avatar-spacer"></div>
                                <div class="message-body">
                                    <div v-if="msg.recalled" class="message-recalled-notice">
                                        {{ msg.content }}
                                    </div>
                                    <template v-else>
                                        <div class="message-bubble" :class="{ 'message-image-bubble': isImageMessage(msg) }">
                                            <div
                                                v-if="msg.repliedMessage"
                                                class="reply-quote"
                                                @click="scrollToMessage(msg.parentId)"
                                            >
                                                <div class="reply-quote-sender">{{ msg.repliedMessage.senderName || '' }}</div>
                                                <div class="reply-quote-content">{{
                                                    msg.repliedMessage.content || '该消息已被删除'
                                                }}</div>
                                            </div>
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
                                    </template>
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
                    <div v-if="replyingTo" class="reply-indicator">
                        <span class="reply-indicator-label">回复 @{{ replyingTo.senderName }}</span>
                        <span class="reply-indicator-preview">{{ replyPreview(replyingTo) }}</span>
                        <button class="reply-indicator-cancel" title="取消回复" @click="replyingTo = null">✕</button>
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
    <div v-if="contextMenu.visible" class="message-context-menu" :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }" @click.stop>
        <button v-if="canRecall(contextMenu.message)" @click="handleRecall(contextMenu.message); closeContextMenu()">撤回</button>
        <button @click="startReply(contextMenu.message); closeContextMenu()">回复</button>
    </div>
    <div
        v-if="previewImageSrc"
        class="image-preview-overlay"
        role="dialog"
        aria-modal="true"
        aria-label="图片预览"
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
        <div ref="previewStageRef" class="image-preview-stage" @click.self="closeImagePreview">
            <img
                ref="previewImageRef"
                class="image-preview-full"
                :class="previewImageClass"
                :src="previewImageSrc"
                :style="previewImageStyle"
                alt="图片预览"
                draggable="false"
                @pointerdown="startPreviewDrag"
            >
        </div>
    </div>
</template>

<style scoped src="@/styles/views/MessagesView.part-1.css"></style>
<style scoped src="@/styles/views/MessagesView.part-2.css"></style>
<style scoped src="@/styles/views/MessagesView.part-3.css"></style>
<style scoped src="@/styles/views/MessagesView.part-4.css"></style>
