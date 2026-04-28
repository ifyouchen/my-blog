<script setup>
import {computed, ref} from 'vue';
import {renderMarkdown} from '@/utils/markdown';

const props = defineProps({
    content: {
        type: String,
        default: ''
    }
});

const renderedHtml = computed(() => renderMarkdown(props.content));

const pendingLink = ref(null);

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
    const button = event.target.closest('.code-copy-button');
    if (button) {
        copyCode(button);
        return;
    }

    const link = event.target.closest('a');
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
</script>

<template>
    <section
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
</template>

<style scoped>
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
</style>
