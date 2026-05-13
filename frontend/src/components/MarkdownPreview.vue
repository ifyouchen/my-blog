<script setup>
import {computed, onMounted, onUnmounted, ref} from 'vue';
import {renderMarkdown} from '@/utils/markdown';

const props = defineProps({
    content: {
        type: String,
        default: ''
    }
});

const renderedHtml = computed(() => renderMarkdown(props.content));

const previewRootRef = ref(null);
const previewStageRef = ref(null);
const previewImageRef = ref(null);
const pendingLink = ref(null);
const previewImages = ref([]);
const previewIndex = ref(-1);
const previewScale = ref(1);
const previewOffset = ref({ x: 0, y: 0 });
const previewDragging = ref(false);
let previewDragStart = null;

const PREVIEW_MIN_SCALE = 0.5;
const PREVIEW_MAX_SCALE = 4;
const PREVIEW_SCALE_STEP = 0.25;

const activePreviewImage = computed(() => previewImages.value[previewIndex.value] || null);
const canDragPreviewImage = computed(() => Boolean(activePreviewImage.value));
const previewImageStyle = computed(() => ({
    transform: `translate(${previewOffset.value.x}px, ${previewOffset.value.y}px) scale(${previewScale.value})`
}));
const previewImageClass = computed(() => ({
    'is-draggable': canDragPreviewImage.value,
    'is-dragging': previewDragging.value
}));
const previewScalePercent = computed(() => `${Math.round(previewScale.value * 100)}%`);
const canSwitchPreviewImage = computed(() => previewImages.value.length > 1);
const previewImagePosition = computed(() => {
    if (!activePreviewImage.value || previewIndex.value < 0) {
        return '';
    }
    return `${previewIndex.value + 1} / ${previewImages.value.length}`;
});

const getImageSource = (image) => image?.currentSrc || image?.src || image?.getAttribute('src') || '';

const collectPreviewImages = () => Array.from(previewRootRef.value?.querySelectorAll('img') || [])
    .map((image) => ({
        src: getImageSource(image),
        alt: image.getAttribute('alt') || '图片预览'
    }))
    .filter((image) => image.src);

const clampPreviewScale = (value) => Math.min(PREVIEW_MAX_SCALE, Math.max(PREVIEW_MIN_SCALE, value));
const resetPreviewOffset = () => {
    previewOffset.value = { x: 0, y: 0 };
};

const clampPreviewOffset = (offset) => {
    const stage = previewStageRef.value;
    const image = previewImageRef.value;
    if (!stage || !image) {
        return { x: 0, y: 0 };
    }
    const stageRect = stage.getBoundingClientRect();
    const scaledWidth = image.offsetWidth * previewScale.value;
    const scaledHeight = image.offsetHeight * previewScale.value;
    const maxX = Math.max(0, (scaledWidth - stageRect.width) / 2);
    const maxY = Math.max(0, (scaledHeight - stageRect.height) / 2);
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
    resetPreviewOffset();
};

const openImagePreview = (image) => {
    const src = getImageSource(image);
    if (!src) {
        return;
    }
    const images = collectPreviewImages();
    const index = Math.max(0, images.findIndex((item) => item.src === src));
    previewImages.value = images.length ? images : [{
        src,
        alt: image.getAttribute('alt') || '图片预览'
    }];
    previewIndex.value = index;
    resetPreviewZoom();
};

const closeImagePreview = () => {
    stopPreviewDrag();
    previewImages.value = [];
    previewIndex.value = -1;
    resetPreviewZoom();
};

const switchPreviewImage = (direction) => {
    if (!canSwitchPreviewImage.value) {
        return;
    }
    stopPreviewDrag();
    previewIndex.value = (
        previewIndex.value + direction + previewImages.value.length
    ) % previewImages.value.length;
    resetPreviewZoom();
};

const handlePreviewWheel = (event) => {
    zoomPreview(event.deltaY > 0 ? -PREVIEW_SCALE_STEP : PREVIEW_SCALE_STEP);
};

const handlePreviewDragMove = (event) => {
    if (!previewDragging.value || !previewDragStart) {
        return;
    }
    event.preventDefault();
    const nextOffset = {
        x: previewDragStart.offset.x + event.clientX - previewDragStart.clientX,
        y: previewDragStart.offset.y + event.clientY - previewDragStart.clientY
    };
    previewOffset.value = clampPreviewOffset(nextOffset);
};

const stopPreviewDrag = () => {
    if (!previewDragging.value && !previewDragStart) {
        return;
    }
    previewDragging.value = false;
    previewDragStart = null;
    window.removeEventListener('pointermove', handlePreviewDragMove);
    window.removeEventListener('pointerup', stopPreviewDrag);
    window.removeEventListener('pointercancel', stopPreviewDrag);
};

const startPreviewDrag = (event) => {
    if (!canDragPreviewImage.value || event.button !== 0) {
        return;
    }
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

const handlePreviewKeydown = (event) => {
    if (!activePreviewImage.value) {
        return;
    }
    if (event.key === 'Escape') {
        closeImagePreview();
        return;
    }
    if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') {
        event.preventDefault();
        switchPreviewImage(-1);
        return;
    }
    if (event.key === 'ArrowRight' || event.key === 'ArrowDown') {
        event.preventDefault();
        switchPreviewImage(1);
        return;
    }
    if (event.key === '+' || event.key === '=') {
        event.preventDefault();
        zoomPreview(PREVIEW_SCALE_STEP);
        return;
    }
    if (event.key === '-' || event.key === '_') {
        event.preventDefault();
        zoomPreview(-PREVIEW_SCALE_STEP);
        return;
    }
    if (event.key === '0') {
        event.preventDefault();
        resetPreviewZoom();
    }
};

const fallbackCopy = (text) => {
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.setAttribute('readonly', 'readonly');
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    textarea.select();
    document.execCommand('copy');
    document.body.removeChild(textarea);
};

const copyCode = async (button) => {
    const codeBlock = button.closest('.md-code-block');
    const code = codeBlock?.querySelector('code')?.textContent || '';
    if (!code) {
        return;
    }
    try {
        if (navigator.clipboard?.writeText) {
            await navigator.clipboard.writeText(code);
        } else {
            fallbackCopy(code);
        }
        const originalText = button.textContent;
        button.textContent = '已复制';
        window.setTimeout(() => {
            button.textContent = originalText || '复制';
        }, 1400);
    } catch (error) {
        button.textContent = '复制失败';
        window.setTimeout(() => {
            button.textContent = '复制';
        }, 1400);
    }
};

const handleClick = (event) => {
    const target = event.target instanceof Element ? event.target : null;
    if (!target) {
        return;
    }

    const image = target.closest('.markdown-preview img');
    if (image) {
        event.preventDefault();
        openImagePreview(image);
        return;
    }

    const button = target.closest('.code-copy-button');
    if (button) {
        copyCode(button);
        return;
    }

    const link = target.closest('a');
    if (link && link.href) {
        try {
            const linkUrl = new URL(link.href);
            const isExternal = linkUrl.hostname && linkUrl.hostname !== window.location.hostname;
            if (isExternal) {
                event.preventDefault();
                pendingLink.value = {
                    url: link.href,
                    title: link.textContent || link.href
                };
            }
        } catch (e) {
            // Invalid URL, let browser handle it normally
        }
    }
};

const confirmExternalLink = () => {
    if (pendingLink.value) {
        window.open(pendingLink.value.url, '_blank');
        pendingLink.value = null;
    }
};

const cancelExternalLink = () => {
    pendingLink.value = null;
};

onMounted(() => {
    window.addEventListener('keydown', handlePreviewKeydown);
});

onUnmounted(() => {
    window.removeEventListener('keydown', handlePreviewKeydown);
    stopPreviewDrag();
});
</script>

<template>
    <section
        ref="previewRootRef"
        class="markdown-preview"
        @click="handleClick"
        v-html="renderedHtml"
    ></section>

    <Teleport to="body">
        <div
            v-if="pendingLink"
            class="external-link-overlay"
            @click.self="cancelExternalLink"
        >
            <div class="external-link-dialog" role="dialog" aria-modal="true" aria-label="外部链接确认">
                <div class="external-link-icon">🔗</div>
                <h3 class="external-link-title">即将离开本站</h3>
                <p class="external-link-message">链接将跳转到外部站点：</p>
                <p class="external-link-url">{{ pendingLink.title }}</p>
                <div class="external-link-actions">
                    <button
                        type="button"
                        class="external-link-cancel"
                        @click="cancelExternalLink"
                    >
                        取消
                    </button>
                    <button
                        type="button"
                        class="external-link-confirm"
                        @click="confirmExternalLink"
                    >
                        继续访问
                    </button>
                </div>
            </div>
        </div>
    </Teleport>

    <Teleport to="body">
        <div
            v-if="activePreviewImage"
            class="markdown-image-preview-overlay"
            role="dialog"
            aria-modal="true"
            aria-label="图片预览"
            @click.self="closeImagePreview"
            @wheel.prevent="handlePreviewWheel"
        >
            <div class="markdown-image-preview-toolbar">
                <span v-if="previewImagePosition" class="markdown-image-preview-count">
                    {{ previewImagePosition }}
                </span>
                <button
                    class="markdown-image-preview-tool"
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
                    class="markdown-image-preview-scale"
                    type="button"
                    title="恢复 100%"
                    @click="resetPreviewZoom"
                >
                    {{ previewScalePercent }}
                </button>
                <button
                    class="markdown-image-preview-tool"
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
                class="markdown-image-preview-nav markdown-image-preview-prev"
                type="button"
                title="上一张"
                @click="switchPreviewImage(-1)"
            >
                <svg viewBox="0 0 24 24" width="28" height="28" fill="none">
                    <path d="M15 18l-6-6 6-6" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
            </button>
            <button
                v-if="canSwitchPreviewImage"
                class="markdown-image-preview-nav markdown-image-preview-next"
                type="button"
                title="下一张"
                @click="switchPreviewImage(1)"
            >
                <svg viewBox="0 0 24 24" width="28" height="28" fill="none">
                    <path d="M9 18l6-6-6-6" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
            </button>
            <button
                class="markdown-image-preview-close"
                type="button"
                title="关闭预览"
                @click="closeImagePreview"
            >
                <svg viewBox="0 0 20 20" width="20" height="20" fill="none">
                    <path d="M5 5l10 10M15 5L5 15" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
                </svg>
            </button>
            <div ref="previewStageRef" class="markdown-image-preview-stage" @click.self="closeImagePreview">
                <img
                    ref="previewImageRef"
                    class="markdown-image-preview-full"
                    :class="previewImageClass"
                    :src="activePreviewImage.src"
                    :alt="activePreviewImage.alt"
                    :style="previewImageStyle"
                    draggable="false"
                    @pointerdown="startPreviewDrag"
                >
            </div>
        </div>
    </Teleport>
</template>

<style scoped>
.markdown-preview :deep(img) {
    cursor: zoom-in;
}

.markdown-preview :deep(img:hover) {
    filter: brightness(0.97);
}

.external-link-overlay {
    position: fixed;
    inset: 0;
    z-index: 1000;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(0, 0, 0, 0.45);
    backdrop-filter: blur(2px);
    padding: 20px;
}

.external-link-dialog {
    width: 100%;
    max-width: 400px;
    padding: 28px 24px;
    background: var(--surface);
    border-radius: var(--radius-sm);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.16);
    text-align: center;
}

.external-link-icon {
    font-size: 36px;
    margin-bottom: 12px;
    line-height: 1;
}

.external-link-title {
    margin: 0 0 10px;
    font-size: 18px;
    font-weight: 700;
    color: var(--text);
}

.external-link-message {
    margin: 0 0 6px;
    font-size: 14px;
    color: var(--muted);
    line-height: 1.5;
}

.external-link-url {
    margin: 0 0 22px;
    padding: 10px 14px;
    font-size: 13px;
    color: var(--brand);
    background: var(--brand-soft);
    border: 1px solid rgba(37, 99, 235, 0.12);
    border-radius: var(--radius-sm);
    word-break: break-all;
    line-height: 1.5;
    font-weight: 600;
}

.external-link-actions {
    display: flex;
    gap: 10px;
    justify-content: center;
}

.external-link-cancel,
.external-link-confirm {
    min-height: 40px;
    padding: 0 22px;
    border-radius: var(--radius-sm);
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: background-color 0.16s ease, border-color 0.16s ease;
}

.external-link-cancel {
    background: transparent;
    border: 1px solid var(--line);
    color: var(--muted);
}

.external-link-cancel:hover {
    border-color: var(--text);
    color: var(--text);
}

.external-link-confirm {
    background: var(--brand);
    border: none;
    color: #ffffff;
    box-shadow: none;
}

.external-link-confirm:hover {
    filter: brightness(1.06);
    box-shadow: none;
}

.markdown-image-preview-overlay {
    position: fixed;
    inset: 0;
    z-index: 1100;
    padding: 48px;
    background: rgba(15, 23, 42, 0.78);
    user-select: none;
}

.markdown-image-preview-stage {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    overflow: hidden;
}

.markdown-image-preview-full {
    max-width: min(92vw, 1120px);
    max-height: 88vh;
    object-fit: contain;
    background: var(--surface);
    border-radius: 10px;
    box-shadow: 0 24px 80px rgba(15, 23, 42, 0.36);
    transition: transform 0.12s ease-out;
    transform-origin: center center;
    touch-action: none;
    user-select: none;
    -webkit-user-drag: none;
}

.markdown-image-preview-full.is-draggable {
    cursor: grab;
}

.markdown-image-preview-full.is-dragging {
    cursor: grabbing;
    transition: none;
}

.markdown-image-preview-toolbar {
    position: fixed;
    top: 20px;
    left: 50%;
    z-index: 1101;
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 6px;
    background: rgba(15, 23, 42, 0.68);
    border: 1px solid rgba(255, 255, 255, 0.24);
    border-radius: 999px;
    transform: translateX(-50%);
}

.markdown-image-preview-count {
    min-width: 48px;
    padding: 0 8px;
    color: rgba(255, 255, 255, 0.9);
    font-size: 13px;
    font-weight: 600;
    text-align: center;
}

.markdown-image-preview-tool,
.markdown-image-preview-scale,
.markdown-image-preview-close,
.markdown-image-preview-nav {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    cursor: pointer;
}

.markdown-image-preview-tool,
.markdown-image-preview-scale {
    height: 32px;
    background: transparent;
    border: 0;
    border-radius: 999px;
}

.markdown-image-preview-tool {
    width: 32px;
    padding: 0;
}

.markdown-image-preview-scale {
    min-width: 58px;
    padding: 0 10px;
    font-size: 13px;
    font-weight: 600;
}

.markdown-image-preview-tool:hover:not(:disabled),
.markdown-image-preview-scale:hover {
    background: rgba(255, 255, 255, 0.12);
}

.markdown-image-preview-tool:disabled {
    cursor: not-allowed;
    opacity: 0.36;
}

.markdown-image-preview-close {
    position: fixed;
    top: 20px;
    right: 20px;
    z-index: 1101;
    width: 40px;
    height: 40px;
    padding: 0;
    background: rgba(15, 23, 42, 0.68);
    border: 1px solid rgba(255, 255, 255, 0.24);
    border-radius: 50%;
}

.markdown-image-preview-close:hover {
    background: rgba(15, 23, 42, 0.86);
}

.markdown-image-preview-nav {
    position: fixed;
    top: 50%;
    z-index: 1101;
    width: 48px;
    height: 64px;
    padding: 0;
    background: rgba(15, 23, 42, 0.58);
    border: 1px solid rgba(255, 255, 255, 0.22);
    border-radius: 999px;
    transform: translateY(-50%);
}

.markdown-image-preview-nav:hover {
    background: rgba(15, 23, 42, 0.82);
}

.markdown-image-preview-prev {
    left: 24px;
}

.markdown-image-preview-next {
    right: 24px;
}

@media (max-width: 760px) {
    .markdown-image-preview-overlay {
        padding: 18px;
    }

    .markdown-image-preview-full {
        max-width: 96vw;
        max-height: 86vh;
        border-radius: 8px;
    }

    .markdown-image-preview-toolbar {
        top: 12px;
    }

    .markdown-image-preview-count {
        min-width: 40px;
        padding: 0 4px;
    }

    .markdown-image-preview-close {
        top: 12px;
        right: 12px;
    }

    .markdown-image-preview-nav {
        width: 40px;
        height: 54px;
    }

    .markdown-image-preview-prev {
        left: 10px;
    }

    .markdown-image-preview-next {
        right: 10px;
    }
}
</style>
