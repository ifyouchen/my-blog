<script setup>
import { computed } from 'vue';
import { NodeViewContent, NodeViewWrapper, nodeViewProps } from '@tiptap/vue-3';
import { getCodeLanguageClass, getCodeLanguageLabel } from '@/utils/codeLanguages';

const props = defineProps(nodeViewProps);

const language = computed(() => props.node.attrs.language || 'text');
const languageLabel = computed(() => getCodeLanguageLabel(language.value));
const languageClass = computed(() => getCodeLanguageClass(language.value));

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

const copyCode = async () => {
    const code = props.node.textContent || '';
    if (!code) {
        return;
    }
    try {
        if (navigator.clipboard?.writeText) {
            await navigator.clipboard.writeText(code);
            return;
        }
    } catch (error) {
        // 降级到传统复制方式，兼容非安全上下文或浏览器权限限制。
    }
    fallbackCopy(code);
};

const keepEditorFocus = () => {};

const getCurrentPosition = () => {
    return typeof props.getPos === 'function' ? props.getPos() : null;
};

const insertParagraphAt = (position) => {
    if (position === null || position === undefined) {
        return;
    }
    props.editor
        .chain()
        .focus()
        .insertContentAt(position, { type: 'paragraph' })
        .focus(position + 1)
        .run();
};

const insertParagraphBefore = () => {
    insertParagraphAt(getCurrentPosition());
};

const insertParagraphAfter = () => {
    const position = getCurrentPosition();
    if (position === null || position === undefined) {
        return;
    }
    insertParagraphAt(position + props.node.nodeSize);
};

const deleteCodeBlock = () => {
    const position = getCurrentPosition();
    if (position === null || position === undefined) {
        return;
    }
    props.editor
        .chain()
        .focus()
        .deleteRange({
            from: position,
            to: position + props.node.nodeSize
        })
        .run();
};
</script>

<template>
    <NodeViewWrapper class="rich-code-block" :class="languageClass">
        <div class="rich-code-toolbar" contenteditable="false">
            <span>{{ languageLabel }}</span>
            <div class="rich-code-actions">
                <button type="button" @mousedown.prevent="keepEditorFocus" @click="insertParagraphBefore">上方插入</button>
                <button type="button" @mousedown.prevent="keepEditorFocus" @click="insertParagraphAfter">下方插入</button>
                <button type="button" @mousedown.prevent="keepEditorFocus" @click="copyCode">复制</button>
                <button type="button" class="danger" @mousedown.prevent="keepEditorFocus" @click="deleteCodeBlock">删除</button>
            </div>
        </div>
        <pre><NodeViewContent as="code" /></pre>
    </NodeViewWrapper>
</template>
