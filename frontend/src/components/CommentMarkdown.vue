<script setup>
import {computed} from 'vue';
import {renderMarkdown} from '@/utils/markdown';

const props = defineProps({
    content: {
        type: String,
        default: ''
    },
    compact: {
        type: Boolean,
        default: false
    }
});

const renderedHtml = computed(() => renderMarkdown(props.content || ''));

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
    } catch {
        button.textContent = '复制失败';
    }
};

const handleClick = (event) => {
    const button = event.target.closest('.code-copy-button');
    if (button) {
        copyCode(button);
    }
};
</script>

<template>
    <div
        :class="['comment-markdown', { compact }]"
        v-html="renderedHtml"
        @click="handleClick"
    ></div>
</template>

<style scoped>
.comment-markdown {
    color: var(--text);
    font-size: 14px;
    line-height: 1.8;
    overflow-wrap: anywhere;
}

.comment-markdown :deep(p) {
    margin: 0 0 8px;
}

.comment-markdown :deep(p:last-child),
.comment-markdown :deep(ul:last-child),
.comment-markdown :deep(ol:last-child),
.comment-markdown :deep(blockquote:last-child),
.comment-markdown :deep(pre:last-child),
.comment-markdown :deep(.md-code-block:last-child) {
    margin-bottom: 0;
}

.comment-markdown :deep(a) {
    color: var(--brand-strong);
    text-decoration: none;
}

.comment-markdown :deep(a:hover) {
    text-decoration: underline;
}

.comment-markdown :deep(code:not(pre code)) {
    padding: 1px 5px;
    color: #0f5132;
    background: rgba(16, 185, 129, 0.12);
    border-radius: 4px;
}

.comment-markdown :deep(blockquote) {
    margin: 8px 0;
    padding: 8px 12px;
    color: var(--muted);
    background: var(--surface-soft);
    border-left: 3px solid var(--brand-hover);
}

.comment-markdown :deep(ul),
.comment-markdown :deep(ol) {
    margin: 8px 0;
    padding-left: 22px;
}

.comment-markdown :deep(.md-code-block) {
    margin: 10px 0;
    overflow: hidden;
    background: #0f172a;
    border: 1px solid rgba(148, 163, 184, 0.25);
    border-radius: var(--radius-md);
}

.comment-markdown :deep(.md-code-block figcaption) {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 7px 10px;
    color: #cbd5e1;
    font-size: 12px;
    background: rgba(15, 23, 42, 0.92);
    border-bottom: 1px solid rgba(148, 163, 184, 0.18);
}

.comment-markdown :deep(.code-copy-button) {
    min-height: 24px;
    padding: 0 8px;
    color: #dbeafe;
    font-size: 12px;
    cursor: pointer;
    background: rgba(59, 130, 246, 0.16);
    border: 1px solid rgba(147, 197, 253, 0.28);
    border-radius: var(--radius-sm);
}

.comment-markdown :deep(pre) {
    margin: 0;
    padding: 12px;
    overflow-x: auto;
}

.comment-markdown :deep(pre code) {
    font-size: 13px;
    line-height: 1.7;
    white-space: pre;
}

.comment-markdown.compact {
    font-size: 13px;
}
</style>
