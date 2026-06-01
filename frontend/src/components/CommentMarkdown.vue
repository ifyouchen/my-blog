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

<style scoped src="@/styles/components/CommentMarkdown.css"></style>
