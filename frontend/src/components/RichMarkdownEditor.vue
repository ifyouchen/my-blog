<script setup>
import {computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch} from 'vue';
import {useWindowSize} from '@/composables/useWindowSize';
import {BubbleMenu, EditorContent, useEditor, VueNodeViewRenderer} from '@tiptap/vue-3';
import {Extension} from '@tiptap/core';
import StarterKit from '@tiptap/starter-kit';
import Link from '@tiptap/extension-link';
import Table from '@tiptap/extension-table';
import TableCell from '@tiptap/extension-table-cell';
import TableHeader from '@tiptap/extension-table-header';
import TableRow from '@tiptap/extension-table-row';
import CodeBlockLowlight from '@tiptap/extension-code-block-lowlight';
import TaskList from '@tiptap/extension-task-list';
import TaskItem from '@tiptap/extension-task-item';
import Underline from '@tiptap/extension-underline';
import TextAlign from '@tiptap/extension-text-align';
import TextStyle from '@tiptap/extension-text-style';
import ResizeImage from 'tiptap-extension-resize-image';
import {createBlogLowlight} from '@/utils/codeLanguages';
import {editorJsonToMarkdown, markdownToEditorHtml} from '@/utils/markdown';
import CodeBlockNodeView from '@/components/CodeBlockNodeView.vue';
import {uploadImage} from '@/api/uploads.js';
import {useSession} from '@/stores/session';

const { isLoggedIn } = useSession();

const props = defineProps({
    modelValue: {
        type: String,
        default: ''
    }
});

const emit = defineEmits(['update:modelValue']);

const lowlight = createBlogLowlight();
const editorStateVersion = ref(0);
const editorRoot = ref(null);
const contextMenuRef = ref(null);
const toolbarColorPaletteRef = ref(null);
const bubbleColorPaletteRef = ref(null);
const imageInputRef = ref(null);
const imageUploading = ref(false);
const uploadError = ref('');
const dragOver = ref(false);
const activeColorPalette = ref('');
const savedTextSelection = ref(null);

const DEFAULT_TEXT_COLOR = '#2c3e50';
const TEXT_COLOR_OPTIONS = [
    { label: '默认', value: DEFAULT_TEXT_COLOR },
    { label: '灰色', value: '#86909c' },
    { label: '红色', value: '#f53f3f' },
    { label: '橙色', value: '#f77234' },
    { label: '黄色', value: '#f7ba1e' },
    { label: '绿色', value: '#00a870' },
    { label: '蓝色', value: '#245bdb' },
    { label: '紫色', value: '#722ed1' }
];
const BACKGROUND_COLOR_OPTIONS = [
    { label: '浅灰', value: '#f2f3f5' },
    { label: '浅红', value: '#fbbfbc' },
    { label: '浅橙', value: '#fed4a4' },
    { label: '浅黄', value: '#fff67a' },
    { label: '浅绿', value: '#b7edb1' },
    { label: '浅蓝', value: '#bedaff' },
    { label: '浅紫', value: '#d8c3ff' },
    { label: '灰色', value: '#c9cdd4' },
    { label: '红色', value: '#f76964' },
    { label: '橙色', value: '#ff9a2e' },
    { label: '黄色', value: '#fadc19' },
    { label: '绿色', value: '#5ac84f' },
    { label: '蓝色', value: '#8bb1ff' },
    { label: '紫色', value: '#b38cff' }
];
const FONT_FAMILY_OPTIONS = [
    { label: '默认字体', value: '' },
    { label: '微软雅黑', value: "'Microsoft YaHei', 'PingFang SC', sans-serif" },
    { label: '宋体', value: "'SimSun', 'Songti SC', serif" },
    { label: '黑体', value: "'SimHei', 'Heiti SC', sans-serif" },
    { label: 'Arial', value: 'Arial, Helvetica, sans-serif' },
    { label: 'Georgia', value: 'Georgia, serif' },
    { label: '等宽', value: "'Courier New', Consolas, monospace" }
];
const FONT_SIZE_OPTIONS = [
    { label: '默认字号', value: '' },
    { label: '12px', value: '12px' },
    { label: '14px', value: '14px' },
    { label: '16px', value: '16px' },
    { label: '18px', value: '18px' },
    { label: '20px', value: '20px' },
    { label: '24px', value: '24px' },
    { label: '28px', value: '28px' },
    { label: '32px', value: '32px' }
];
const LINE_HEIGHT_OPTIONS = [
    { label: '默认行距', value: '' },
    { label: '1.4', value: '1.4' },
    { label: '1.6', value: '1.6' },
    { label: '1.8', value: '1.8' },
    { label: '2.0', value: '2' },
    { label: '2.4', value: '2.4' }
];
const FONT_FAMILY_VALUES = new Set(FONT_FAMILY_OPTIONS.map(option => option.value).filter(Boolean));
const FONT_SIZE_VALUES = new Set(FONT_SIZE_OPTIONS.map(option => option.value).filter(Boolean));
const LINE_HEIGHT_VALUES = new Set(LINE_HEIGHT_OPTIONS.map(option => option.value).filter(Boolean));
const FONT_FAMILY_MATCHERS = [
    { value: "'Microsoft YaHei', 'PingFang SC', sans-serif", keys: ['microsoft yahei', 'pingfang sc'] },
    { value: "'SimSun', 'Songti SC', serif", keys: ['simsun', 'songti sc', '宋体'] },
    { value: "'SimHei', 'Heiti SC', sans-serif", keys: ['simhei', 'heiti sc', '黑体'] },
    { value: 'Arial, Helvetica, sans-serif', keys: ['arial', 'helvetica'] },
    { value: 'Georgia, serif', keys: ['georgia'] },
    { value: "'Courier New', Consolas, monospace", keys: ['courier new', 'consolas'] }
];

const normalizeTextColor = (value = '') => {
    const color = String(value).trim().toLowerCase();
    if (/^#[0-9a-f]{6}$/.test(color)) {
        return color;
    }
    const rgbMatched = color.match(/^rgb\(\s*(\d{1,3})\s*,\s*(\d{1,3})\s*,\s*(\d{1,3})\s*\)$/);
    if (!rgbMatched) {
        return '';
    }
    const channels = rgbMatched.slice(1).map(Number);
    if (channels.some(channel => channel < 0 || channel > 255)) {
        return '';
    }
    return `#${channels.map(channel => channel.toString(16).padStart(2, '0')).join('')}`;
};

const normalizeFontFamily = (value = '') => {
    if (FONT_FAMILY_VALUES.has(value)) {
        return value;
    }
    const fontFamily = String(value).trim().replace(/["']/g, '').toLowerCase();
    const matched = FONT_FAMILY_MATCHERS.find(item => item.keys.some(key => fontFamily.includes(key)));
    return matched?.value || '';
};
const normalizeFontSize = (value = '') => FONT_SIZE_VALUES.has(value) ? value : '';
const normalizeLineHeight = (value = '') => LINE_HEIGHT_VALUES.has(value) ? value : '';

const tableMarkdownPattern = /^\s*\|.+\|\s*\n\s*\|[\s:|-]+\|\s*\n(?:\s*\|.*\|\s*\n?)+/m;
const markdownBlockPattern = /(^|\n)\s*(#{1,6}\s|[-*]\s|>\s|\|.+\|\s*\n\s*\|[\s:|-]+\||```|---\s*$)/m;

const slashState = reactive({
    open: false,
    query: '',
    selectedIndex: 0,
    top: 84,
    left: 18
});

const contextMenuState = reactive({
    open: false,
    top: 18,
    left: 18
});

const imageUrlDialog = reactive({
    open: false,
    url: '',
    error: ''
});

const linkDialog = reactive({
    open: false,
    url: '',
    error: ''
});

const isTableSeparator = (line = '') => {
    return /^\s*\|?\s*:?-{3,}:?\s*(\|\s*:?-{3,}:?\s*)+\|?\s*$/.test(line);
};

const isTableLine = (line = '') => {
    return /^\s*\|.+\|\s*$/.test(line);
};

const collectTopLevelBlocks = (doc, selectionFrom) => {
    const blocks = [];
    let currentIndex = -1;
    doc.forEach((node, offset, index) => {
        const end = offset + node.nodeSize;
        blocks.push({ node, offset, index });
        if (selectionFrom >= offset && selectionFrom <= end) {
            currentIndex = index;
        }
    });
    return { blocks, currentIndex };
};

const convertRecentMarkdownTable = (editorInstance, view) => {
    const { state } = view;
    const { blocks, currentIndex } = collectTopLevelBlocks(state.doc, state.selection.from);
    if (currentIndex < 0) {
        return false;
    }

    const tableBlocks = [];
    for (let index = currentIndex; index >= 0; index--) {
        const block = blocks[index];
        if (block.node.type.name !== 'paragraph' || !isTableLine(block.node.textContent)) {
            break;
        }
        tableBlocks.unshift(block);
    }

    if (tableBlocks.length < 3) {
        return false;
    }

    const lines = tableBlocks.map((block) => block.node.textContent);
    if (!isTableSeparator(lines[1])) {
        return false;
    }

    const from = tableBlocks[0].offset;
    const lastBlock = tableBlocks[tableBlocks.length - 1];
    const to = lastBlock.offset + lastBlock.node.nodeSize;
    const markdown = lines.join('\n');

    view.dispatch(state.tr.delete(from, to));
    editorInstance.commands.insertContentAt(from, markdownToEditorHtml(markdown));
    return true;
};

const convertCurrentCodeFence = (editorInstance, view) => {
    const { state } = view;
    const { blocks, currentIndex } = collectTopLevelBlocks(state.doc, state.selection.from);
    if (currentIndex < 0) {
        return false;
    }

    const block = blocks[currentIndex];
    if (block.node.type.name !== 'paragraph') {
        return false;
    }

    const text = block.node.textContent.trim();
    const inlineMatched = text.match(/^```([a-zA-Z0-9_-]*)\s+([\s\S]+?)\s*```$/);
    const openMatched = text.match(/^```([a-zA-Z0-9_-]*)\s*$/);
    if (!inlineMatched && !openMatched) {
        return false;
    }

    const language = (inlineMatched?.[1] || openMatched?.[1] || 'text').trim() || 'text';
    const code = inlineMatched?.[2] || '';
    const from = block.offset;
    const to = block.offset + block.node.nodeSize;
    const markdown = `\`\`\`${language}\n${code}\n\`\`\``;

    view.dispatch(state.tr.delete(from, to));
    editorInstance.commands.insertContentAt(from, markdownToEditorHtml(markdown));
    return true;
};

const richTextStyleExtension = TextStyle.extend({
    addAttributes() {
        return {
            color: {
                default: null,
                parseHTML: element => normalizeTextColor(element.style.color),
                renderHTML: attributes => {
                    const color = normalizeTextColor(attributes.color);
                    return color ? { style: `color:${color}` } : {};
                }
            },
            backgroundColor: {
                default: null,
                parseHTML: element => normalizeTextColor(element.style.backgroundColor),
                renderHTML: attributes => {
                    const backgroundColor = normalizeTextColor(attributes.backgroundColor);
                    return backgroundColor ? { style: `background-color:${backgroundColor}` } : {};
                }
            },
            fontSize: {
                default: null,
                parseHTML: element => normalizeFontSize(element.style.fontSize),
                renderHTML: attributes => {
                    const fontSize = normalizeFontSize(attributes.fontSize);
                    return fontSize ? { style: `font-size:${fontSize}` } : {};
                }
            },
            fontFamily: {
                default: null,
                parseHTML: element => normalizeFontFamily(element.style.fontFamily),
                renderHTML: attributes => {
                    const fontFamily = normalizeFontFamily(attributes.fontFamily);
                    return fontFamily ? { style: `font-family:${fontFamily}` } : {};
                }
            }
        };
    }
}).configure({
    mergeNestedSpanStyles: true
});

const lineHeightExtension = Extension.create({
    name: 'lineHeight',
    addGlobalAttributes() {
        return [
            {
                types: ['heading', 'paragraph'],
                attributes: {
                    lineHeight: {
                        default: null,
                        parseHTML: element => normalizeLineHeight(element.style.lineHeight),
                        renderHTML: attributes => {
                            const lineHeight = normalizeLineHeight(attributes.lineHeight);
                            return lineHeight ? { style: `line-height:${lineHeight}` } : {};
                        }
                    }
                }
            }
        ];
    }
});

const codeBlockExtension = CodeBlockLowlight
    .extend({
        addNodeView() {
            return VueNodeViewRenderer(CodeBlockNodeView);
        }
    })
    .configure({
        lowlight,
        defaultLanguage: 'text'
    });

const insertMarkdownContent = (editor, markdown) => {
    editor.commands.insertContent(markdownToEditorHtml(markdown));
};

const refreshEditorState = () => {
    editorStateVersion.value += 1;
};

const keepEditorFocus = () => {};

const closeSlashMenu = () => {
    slashState.open = false;
    slashState.query = '';
    slashState.selectedIndex = 0;
};

const closeContextMenu = () => {
    contextMenuState.open = false;
    contextMenuMode.value = 'default';
};

const getParagraphContext = (editorInstance) => {
    const { state } = editorInstance;
    const { selection } = state;
    const { $from, empty } = selection;
    if (!empty || $from.parent.type.name !== 'paragraph') {
        return null;
    }

    const text = $from.parent.textContent;
    const textBeforeCursor = text.slice(0, $from.parentOffset);
    return {
        text,
        textBeforeCursor,
        start: $from.start(),
        end: $from.end()
    };
};

const clearCurrentParagraph = (editorInstance) => {
    const context = getParagraphContext(editorInstance);
    if (!context) {
        return false;
    }

    const { state, view } = editorInstance;
    if (context.start !== context.end) {
        view.dispatch(state.tr.delete(context.start, context.end));
    }
    return true;
};

const setSlashMenuPosition = (editorInstance) => {
    if (!editorRoot.value) {
        return;
    }
    try {
        const coordinates = editorInstance.view.coordsAtPos(editorInstance.state.selection.from);
        const rootRect = editorRoot.value.getBoundingClientRect();
        slashState.top = Math.max(16, coordinates.bottom - rootRect.top + 8);
        slashState.left = Math.max(18, coordinates.left - rootRect.left);
    } catch (error) {
        slashState.top = 16;
        slashState.left = 18;
    }
};

const rememberTextSelection = ({ allowEmpty = false } = {}) => {
    if (!editor.value || editor.value.isActive('codeBlock')) {
        return;
    }
    const { from, to, empty } = editor.value.state.selection;
    if (empty && !allowEmpty) {
        return;
    }
    savedTextSelection.value = { from, to };
};

const restoreTextSelection = () => {
    if (!editor.value || !savedTextSelection.value) {
        return;
    }
    const { from, to } = savedTextSelection.value;
    const maxPosition = editor.value.state.doc.content.size;
    if (from < 0 || to < from || to > maxPosition) {
        return;
    }
    editor.value.commands.setTextSelection({ from, to });
};

const syncSlashMenu = (editorInstance) => {
    const context = getParagraphContext(editorInstance);
    if (!context) {
        closeSlashMenu();
        return;
    }

    const matched = context.textBeforeCursor.match(/^\/([^\s]*)$/);
    if (!matched || context.text !== context.textBeforeCursor) {
        closeSlashMenu();
        return;
    }

    slashState.open = true;
    closeContextMenu();
    if (slashState.query !== (matched[1] || '')) {
        slashState.selectedIndex = 0;
    }
    slashState.query = matched[1] || '';
    setSlashMenuPosition(editorInstance);
};

const applyHeadingShortcut = (editorInstance, level) => {
    if (!clearCurrentParagraph(editorInstance)) {
        return false;
    }
    editorInstance.chain().focus().setHeading({ level }).run();
    closeSlashMenu();
    return true;
};

const applyBulletListShortcut = (editorInstance) => {
    if (!clearCurrentParagraph(editorInstance)) {
        return false;
    }
    editorInstance.chain().focus().toggleBulletList().run();
    closeSlashMenu();
    return true;
};

const applyBlockquoteShortcut = (editorInstance) => {
    if (!clearCurrentParagraph(editorInstance)) {
        return false;
    }
    editorInstance.chain().focus().toggleBlockquote().run();
    closeSlashMenu();
    return true;
};

const applyHorizontalRuleShortcut = (editorInstance) => {
    if (!clearCurrentParagraph(editorInstance)) {
        return false;
    }
    editorInstance.chain().focus().setHorizontalRule().run();
    closeSlashMenu();
    return true;
};

const applyTableShortcut = (editorInstance) => {
    if (!clearCurrentParagraph(editorInstance)) {
        return false;
    }
    editorInstance.chain().focus().insertTable({
        rows: 3,
        cols: 3,
        withHeaderRow: true
    }).run();
    closeSlashMenu();
    return true;
};

const applyPlainTableShortcut = (editorInstance) => {
    if (!clearCurrentParagraph(editorInstance)) {
        return false;
    }
    editorInstance.chain().focus().insertTable({
        rows: 3,
        cols: 3,
        withHeaderRow: false
    }).run();
    closeSlashMenu();
    return true;
};

const applyCodeBlockShortcut = (editorInstance, language = 'text') => {
    if (!clearCurrentParagraph(editorInstance)) {
        return false;
    }
    editorInstance.chain().focus().setCodeBlock({ language }).run();
    closeSlashMenu();
    return true;
};

const applyInsertImage = () => {
    if (!editor.value) {
        return false;
    }
    triggerImageUpload();
    closeSlashMenu();
    return true;
};

const editorCommands = [
    {
        id: 'heading-1',
        label: '一级标题',
        description: '用于文章主标题',
        keywords: ['h1', 'heading', 'title', '标题', '一级'],
        run: (editorInstance) => applyHeadingShortcut(editorInstance, 1)
    },
    {
        id: 'heading-2',
        label: '二级标题',
        description: '用于章节标题',
        keywords: ['h2', 'heading', '标题', '二级'],
        run: (editorInstance) => applyHeadingShortcut(editorInstance, 2)
    },
    {
        id: 'heading-3',
        label: '三级标题',
        description: '用于小节标题',
        keywords: ['h3', 'heading', '标题', '三级'],
        run: (editorInstance) => applyHeadingShortcut(editorInstance, 3)
    },
    {
        id: 'bullet-list',
        label: '无序列表',
        description: '插入项目列表',
        keywords: ['list', 'bullet', '列表', '无序'],
        run: applyBulletListShortcut
    },
    {
        id: 'blockquote',
        label: '引用',
        description: '突出引用内容',
        keywords: ['quote', 'blockquote', '引用'],
        run: applyBlockquoteShortcut
    },
    {
        id: 'divider',
        label: '分割线',
        description: '分隔不同内容块',
        keywords: ['divider', 'line', 'hr', '分割线'],
        run: applyHorizontalRuleShortcut
    },
    {
        id: 'table',
        label: '表格',
        description: '插入 3 x 3 表格',
        keywords: ['table', '表格'],
        run: applyTableShortcut
    },
    {
        id: 'table-no-header',
        label: '无表头表格',
        description: '插入不带表头的 3 x 3 表格',
        keywords: ['table', '表格', '无表头'],
        run: applyPlainTableShortcut
    },
    {
        id: 'code-block',
        label: '代码块',
        description: '插入代码块',
        keywords: ['code', 'block', '代码', '代码块'],
        run: (editorInstance) => applyCodeBlockShortcut(editorInstance, 'text')
    },
    {
        id: 'insert-image',
        label: '插入图片',
        description: '上传并插入图片',
        keywords: ['image', 'photo', '图片', '插入图片'],
        run: () => applyInsertImage()
    },
    {
        id: 'insert-image-link',
        label: '图片链接',
        description: '通过链接插入外部图片',
        keywords: ['image', 'link', 'url', '图片', '链接', '外部图片'],
        run: () => {
            openImageUrlDialog();
            closeSlashMenu();
        }
    }
];

const filteredSlashCommands = computed(() => {
    const keyword = slashState.query.trim().toLowerCase();
    if (!keyword) {
        return editorCommands;
    }
    return editorCommands.filter((command) => {
        return [command.label, command.description, ...command.keywords]
            .join(' ')
            .toLowerCase()
            .includes(keyword);
    });
});

const activateSlashCommand = (command) => {
    if (!editor.value || !command) {
        return;
    }
    command.run(editor.value);
    nextTick(() => {
        editor.value?.commands.focus();
    });
};

const moveSlashSelection = (step) => {
    if (!filteredSlashCommands.value.length) {
        slashState.selectedIndex = 0;
        return;
    }
    const total = filteredSlashCommands.value.length;
    slashState.selectedIndex = (slashState.selectedIndex + step + total) % total;
};

const tryConvertMarkdownShortcut = (editorInstance) => {
    const context = getParagraphContext(editorInstance);
    if (!context) {
        return false;
    }

    const headingMatched = context.text.match(/^(#{1,3})\s*$/);
    if (headingMatched) {
        return applyHeadingShortcut(editorInstance, headingMatched[1].length);
    }

    if (/^[-*]\s*$/.test(context.text)) {
        return applyBulletListShortcut(editorInstance);
    }

    if (/^>\s*$/.test(context.text)) {
        return applyBlockquoteShortcut(editorInstance);
    }

    return false;
};

const editor = useEditor({
    content: markdownToEditorHtml(props.modelValue),
    extensions: [
        StarterKit.configure({
            codeBlock: false
        }),
        Link.configure({
            openOnClick: false,
            HTMLAttributes: {
                target: '_blank',
                rel: 'noopener noreferrer'
            }
        }),
        Table.configure({
            resizable: true,
            handleWidth: 6,
            cellMinWidth: 80,
            lastColumnResizable: true
        }),
        TableRow,
        TableHeader,
        TableCell,
        codeBlockExtension,
        richTextStyleExtension,
        Underline,
        TaskList.configure({
            HTMLAttributes: {
                class: 'task-list'
            }
        }),
        TaskItem.configure({
            nested: true,
            HTMLAttributes: {
                class: 'task-item'
            }
        }),
        TextAlign.configure({
            types: ['heading', 'paragraph', 'tableHeader', 'tableCell']
        }),
        lineHeightExtension,
        ResizeImage.configure({
            inline: false,
            allowBase64: true,
            HTMLAttributes: {
                class: 'editor-image'
            }
        })
    ],
    editorProps: {
        attributes: {
            class: 'rich-editor-content'
        },
        handlePaste(view, event) {
            if (editor.value?.isActive('codeBlock')) {
                return false;
            }
            const clipboardData = event.clipboardData;
            if (!clipboardData) {
                return false;
            }
            const items = clipboardData.items;
            if (items) {
                for (const item of items) {
                    if (item.kind === 'file') {
                        const file = item.getAsFile();
                        if (file) {
                            const type = file.type;
                            if (type.startsWith('image/')) {
                                event.preventDefault();
                                uploadAndInsertImage(file);
                                return true;
                            }
                        }
                    }
                }
            }
            const text = clipboardData.getData('text/plain') || '';
            if (tableMarkdownPattern.test(text) || markdownBlockPattern.test(text)) {
                event.preventDefault();
                insertMarkdownContent(editor.value, text);
                return true;
            }
            return false;
        },
        handleKeyDown(view, event) {
            if (!editor.value) {
                return false;
            }
            if (slashState.open) {
                if (event.key === 'ArrowDown') {
                    event.preventDefault();
                    moveSlashSelection(1);
                    return true;
                }
                if (event.key === 'ArrowUp') {
                    event.preventDefault();
                    moveSlashSelection(-1);
                    return true;
                }
                if (event.key === 'Enter') {
                    const command = filteredSlashCommands.value[slashState.selectedIndex];
                    if (!command) {
                        return false;
                    }
                    event.preventDefault();
                    activateSlashCommand(command);
                    return true;
                }
                if (event.key === 'Escape') {
                    event.preventDefault();
                    closeSlashMenu();
                    return true;
                }
            }
            if (event.key !== 'Enter') {
                return false;
            }
            if (tryConvertMarkdownShortcut(editor.value)) {
                event.preventDefault();
                return true;
            }
            if (convertCurrentCodeFence(editor.value, view)) {
                event.preventDefault();
                return true;
            }
            if (convertRecentMarkdownTable(editor.value, view)) {
                event.preventDefault();
                return true;
            }
            return false;
        }
    },
    onUpdate({ editor: currentEditor }) {
        refreshEditorState();
        syncSlashMenu(currentEditor);
        if (currentEditor.view.composing) {
            return;
        }
        const json = currentEditor.getJSON();
        const markdown = editorJsonToMarkdown(json);
        if (import.meta.env.DEV) {
            console.log('Editor JSON:', JSON.stringify(json, null, 2));
            console.log('Converted Markdown:', markdown);
        }
        emit('update:modelValue', markdown);
    },
    onSelectionUpdate({ editor: currentEditor }) {
        refreshEditorState();
        rememberTextSelection();
        syncSlashMenu(currentEditor);
    }
});

onBeforeUnmount(() => {
    closeSlashMenu();
    closeContextMenu();
    closeColorPalette();
    document.removeEventListener('mousedown', handleGlobalPointerDown);
    window.removeEventListener('keydown', handleGlobalKeyDown);
    window.removeEventListener('scroll', handleGlobalScroll, true);
    editor.value?.destroy();
});

onMounted(() => {
    document.addEventListener('mousedown', handleGlobalPointerDown);
    window.addEventListener('keydown', handleGlobalKeyDown);
    window.addEventListener('scroll', handleGlobalScroll, true);
});

watch(() => props.modelValue, (value) => {
    if (!editor.value) {
        return;
    }
    // 输入法组合期间跳过 setContent，避免打断 IME 导致首字母重复
    if (editor.value.view.composing) {
        return;
    }
    const currentMarkdown = editorJsonToMarkdown(editor.value.getJSON());
    if (currentMarkdown === value) {
        return;
    }
    editor.value.commands.setContent(markdownToEditorHtml(value), false);
});

watch(filteredSlashCommands, (commands) => {
    if (!commands.length) {
        slashState.selectedIndex = 0;
        return;
    }
    if (slashState.selectedIndex >= commands.length) {
        slashState.selectedIndex = 0;
    }
});

const setLink = () => {
    if (!editor.value) {
        return;
    }
    linkDialog.url = editor.value.getAttributes('link').href || '';
    linkDialog.error = '';
    linkDialog.open = true;
};

const confirmLink = () => {
    const url = linkDialog.url.trim();
    if (!url) {
        editor.value.chain().focus().unsetLink().run();
        linkDialog.open = false;
        return;
    }
    if (!url.startsWith('http://') && !url.startsWith('https://') && !url.startsWith('/') && !url.startsWith('#')) {
        linkDialog.error = '请输入有效的链接地址（以 http:// 或 https:// 开头）';
        return;
    }
    editor.value.chain().focus().extendMarkRange('link').setLink({ href: url }).run();
    linkDialog.open = false;
};

const cancelLink = () => {
    linkDialog.open = false;
};

const insertTable = (withHeaderRow = true) => {
    editor.value?.chain().focus().insertTable({
        rows: 3,
        cols: 3,
        withHeaderRow
    }).run();
};

const insertCodeBlock = (language) => {
    editor.value?.chain().focus().setCodeBlock({ language }).run();
};

const insertDivider = () => {
    editor.value?.chain().focus().setHorizontalRule().run();
};

const runEditorCommand = (callback) => {
    if (!editor.value) {
        return;
    }
    callback(editor.value.chain().focus()).run();
};

const insertImageMarkdown = (url, alt = '图片') => {
    if (!editor.value) {
        return;
    }
    editor.value.commands.setImage({ src: url, alt });
};

const isImageFile = (file) => Boolean(file?.type?.startsWith('image/'));

const openImageUrlDialog = () => {
    imageUrlDialog.open = true;
    imageUrlDialog.url = '';
    imageUrlDialog.error = '';
};

const confirmImageUrl = () => {
    const url = imageUrlDialog.url.trim();
    if (!url) {
        imageUrlDialog.error = '请输入图片链接地址';
        return;
    }
    if (!url.startsWith('http://') && !url.startsWith('https://')) {
        imageUrlDialog.error = '请输入有效的图片链接（以 http:// 或 https:// 开头）';
        return;
    }
    imageUrlDialog.open = false;
    insertImageMarkdown(url, '外部图片');
};

const cancelImageUrl = () => {
    imageUrlDialog.open = false;
};

const uploadAndInsertImage = async (file) => {
    if (!file || !isImageFile(file)) {
        uploadError.value = '请选择图片文件';
        return;
    }
    if (!isLoggedIn.value) {
        uploadError.value = '请先登录后再上传图片';
        return;
    }
    imageUploading.value = true;
    uploadError.value = '';
    try {
        const result = await uploadImage(file, 'content');
        if (result?.url) {
            insertImageMarkdown(result.url, file.name || '图片');
        }
    } catch (error) {
        uploadError.value = error.message || '图片上传失败';
    } finally {
        imageUploading.value = false;
    }
};

const handleImageSelected = async (event) => {
    const [file] = event.target?.files || [];
    event.target.value = '';
    if (!file) {
        return;
    }
    await uploadAndInsertImage(file);
};

const triggerImageUpload = () => {
    imageInputRef.value?.click();
};

const getDraggedImageFiles = (event) => {
    const files = Array.from(event.dataTransfer?.files || []);
    return files.filter(isImageFile);
};

const focusDropPosition = (event) => {
    if (!editor.value) {
        return;
    }
    const position = editor.value.view.posAtCoords({
        left: event.clientX,
        top: event.clientY
    });
    if (position?.pos) {
        editor.value.chain().focus().setTextSelection(position.pos).run();
    } else {
        editor.value.commands.focus();
    }
};

const handleEditorDragOver = (event) => {
    if (!getDraggedImageFiles(event).length) {
        return;
    }
    event.preventDefault();
    event.dataTransfer.dropEffect = 'copy';
    dragOver.value = true;
};

const handleEditorDragLeave = (event) => {
    if (!editorRoot.value?.contains(event.relatedTarget)) {
        dragOver.value = false;
    }
};

const handleEditorDrop = async (event) => {
    const imageFiles = getDraggedImageFiles(event);
    if (!imageFiles.length) {
        dragOver.value = false;
        return;
    }
    event.preventDefault();
    dragOver.value = false;
    focusDropPosition(event);
    for (const file of imageFiles) {
        await uploadAndInsertImage(file);
    }
};

const clearHeadingJumpHighlights = () => {
    if (!editor.value?.view?.dom) {
        return;
    }
    editor.value.view.dom
        .querySelectorAll('.editor-heading-jump-highlight')
        .forEach((node) => node.classList.remove('editor-heading-jump-highlight'));
};

const scrollToHeadingByIndex = async (headingIndex) => {
    if (!editor.value) {
        return false;
    }

    let currentIndex = -1;
    let targetPosition = null;
    editor.value.state.doc.descendants((node, position) => {
        if (node.type.name !== 'heading') {
            return true;
        }
        currentIndex += 1;
        if (currentIndex === headingIndex) {
            targetPosition = position;
            return false;
        }
        return true;
    });

    if (targetPosition === null) {
        return false;
    }

    closeSlashMenu();
    closeContextMenu();
    editor.value.chain().focus().setTextSelection(targetPosition + 1).run();
    await nextTick();

    const domNode = editor.value.view.nodeDOM(targetPosition);
    const headingElement = domNode instanceof HTMLElement ? domNode : domNode?.parentElement;
    if (headingElement) {
        clearHeadingJumpHighlights();
        headingElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
        headingElement.classList.add('editor-heading-jump-highlight');
        window.setTimeout(() => {
            headingElement.classList.remove('editor-heading-jump-highlight');
        }, 1400);
    }
    return true;
};

defineExpose({
    scrollToHeadingByIndex
});

const toggleBold = () => runEditorCommand((chain) => chain.toggleBold());
const toggleItalic = () => runEditorCommand((chain) => chain.toggleItalic());
const toggleStrike = () => runEditorCommand((chain) => chain.toggleStrike());
const toggleCode = () => runEditorCommand((chain) => chain.toggleCode());
const toggleUnderline = () => runEditorCommand((chain) => chain.toggleUnderline());
const toggleHeading = (level) => runEditorCommand((chain) => chain.toggleHeading({ level }));
const toggleBulletList = () => runEditorCommand((chain) => chain.toggleBulletList());
const toggleOrderedList = () => runEditorCommand((chain) => chain.toggleOrderedList());
const toggleTaskList = () => runEditorCommand((chain) => chain.toggleTaskList());
const toggleBlockquote = () => runEditorCommand((chain) => chain.toggleBlockquote());
const undo = () => runEditorCommand((chain) => chain.undo());
const redo = () => runEditorCommand((chain) => chain.redo());
const addRowBefore = () => runEditorCommand((chain) => chain.addRowBefore());
const addRowAfter = () => runEditorCommand((chain) => chain.addRowAfter());
const deleteRow = () => runEditorCommand((chain) => chain.deleteRow());
const addColumnBefore = () => runEditorCommand((chain) => chain.addColumnBefore());
const addColumnAfter = () => runEditorCommand((chain) => chain.addColumnAfter());
const deleteColumn = () => runEditorCommand((chain) => chain.deleteColumn());
const toggleHeaderRow = () => runEditorCommand((chain) => chain.toggleHeaderRow());
const toggleHeaderColumn = () => runEditorCommand((chain) => chain.toggleHeaderColumn());
const toggleHeaderCell = () => runEditorCommand((chain) => chain.toggleHeaderCell());
const mergeCells = () => runEditorCommand((chain) => chain.mergeCells());
const splitCell = () => runEditorCommand((chain) => chain.splitCell());
const mergeOrSplit = () => runEditorCommand((chain) => chain.mergeOrSplit());
const deleteTable = () => runEditorCommand((chain) => chain.deleteTable());
const setTextAlign = (align) => runEditorCommand((chain) => chain.setTextAlign(align));

const getTextStyleAttributes = () => editor.value?.getAttributes('textStyle') || {};

const isRichStyleAvailable = computed(() => {
    editorStateVersion.value;
    return Boolean(editor.value && !editor.value.isActive('codeBlock'));
});

const activeFontFamily = computed(() => {
    editorStateVersion.value;
    return normalizeFontFamily(getTextStyleAttributes().fontFamily || '');
});

const activeFontSize = computed(() => {
    editorStateVersion.value;
    return normalizeFontSize(getTextStyleAttributes().fontSize || '');
});

const activeTextColor = computed(() => {
    editorStateVersion.value;
    return normalizeTextColor(getTextStyleAttributes().color || '');
});

const activeBackgroundColor = computed(() => {
    editorStateVersion.value;
    return normalizeTextColor(getTextStyleAttributes().backgroundColor || '');
});

const isToolbarColorPaletteOpen = computed(() => activeColorPalette.value === 'toolbar');
const isBubbleColorPaletteOpen = computed(() => activeColorPalette.value === 'bubble');

const activeLineHeight = computed(() => {
    editorStateVersion.value;
    const paragraphLineHeight = editor.value?.getAttributes('paragraph')?.lineHeight || '';
    const headingLineHeight = editor.value?.getAttributes('heading')?.lineHeight || '';
    return normalizeLineHeight(paragraphLineHeight || headingLineHeight);
});

const applyTextStyleAttributes = (attrs = {}) => {
    if (!editor.value || editor.value.isActive('codeBlock')) {
        return;
    }
    restoreTextSelection();
    const current = getTextStyleAttributes();
    const next = {
        color: normalizeTextColor(current.color || ''),
        backgroundColor: normalizeTextColor(current.backgroundColor || ''),
        fontSize: normalizeFontSize(current.fontSize || ''),
        fontFamily: normalizeFontFamily(current.fontFamily || ''),
        ...attrs
    };
    Object.keys(next).forEach((key) => {
        if (!next[key]) {
            delete next[key];
        }
    });

    const chain = editor.value.chain().focus();
    if (!Object.keys(next).length) {
        chain.unsetMark('textStyle').run();
    } else {
        chain.setMark('textStyle', next).removeEmptyTextStyle().run();
    }
    refreshEditorState();
};

const applyFontFamily = (value) => {
    applyTextStyleAttributes({ fontFamily: normalizeFontFamily(value) });
};

const applyFontSize = (value) => {
    applyTextStyleAttributes({ fontSize: normalizeFontSize(value) });
};

const applyTextColor = (value) => {
    applyTextStyleAttributes({ color: normalizeTextColor(value) });
    closeColorPalette();
};

const applyBackgroundColor = (value) => {
    applyTextStyleAttributes({ backgroundColor: normalizeTextColor(value) });
    closeColorPalette();
};

const clearTextColor = () => {
    applyTextStyleAttributes({ color: '' });
};

const clearBackgroundColor = () => {
    applyTextStyleAttributes({ backgroundColor: '' });
    closeColorPalette();
};

const resetColorPalette = () => {
    applyTextStyleAttributes({ color: '', backgroundColor: '' });
    closeColorPalette();
};

const toggleColorPalette = (source = 'toolbar') => {
    if (!isRichStyleAvailable.value) {
        return;
    }
    activeColorPalette.value = activeColorPalette.value === source ? '' : source;
};

const closeColorPalette = () => {
    activeColorPalette.value = '';
};

const clearTextStyle = () => {
    if (!editor.value || editor.value.isActive('codeBlock')) {
        return;
    }
    editor.value.chain().focus().unsetMark('textStyle').run();
    refreshEditorState();
};

const applyLineHeight = (value) => {
    if (!editor.value || editor.value.isActive('codeBlock')) {
        return;
    }
    restoreTextSelection();
    const lineHeight = normalizeLineHeight(value) || null;
    const nodeType = editor.value.isActive('heading') ? 'heading' : 'paragraph';
    editor.value.chain().focus().updateAttributes(nodeType, { lineHeight }).run();
    refreshEditorState();
};

const isInTable = computed(() => {
    return editorStateVersion.value >= 0 && Boolean(editor.value?.isActive('table'));
});

const tableHasHeaderRow = computed(() => {
    editorStateVersion.value;
    if (!editor.value?.isActive('table')) {
        return false;
    }
    const { $from } = editor.value.state.selection;
    for (let depth = $from.depth; depth > 0; depth--) {
        const node = $from.node(depth);
        if (node.type.name !== 'table') {
            continue;
        }
        const firstRow = node.firstChild;
        if (!firstRow?.childCount) {
            return false;
        }
        for (let index = 0; index < firstRow.childCount; index++) {
            if (firstRow.child(index).type.name !== 'tableHeader') {
                return false;
            }
        }
        return true;
    }
    return false;
});

const toolbarGroups = computed(() => {
    editorStateVersion.value;
    const currentEditor = editor.value;
    return [
        {
            id: 'history',
            label: '历史',
            items: [
                {
                    id: 'undo',
                    label: '撤销',
                    active: false,
                    run: undo
                },
                {
                    id: 'redo',
                    label: '重做',
                    active: false,
                    run: redo
                }
            ]
        },
        {
            id: 'text',
            label: '文字样式',
            items: [
                {
                    id: 'bold',
                    label: '加粗',
                    active: Boolean(currentEditor?.isActive('bold')),
                    run: toggleBold
                },
                {
                    id: 'italic',
                    label: '斜体',
                    active: Boolean(currentEditor?.isActive('italic')),
                    run: toggleItalic
                },
                {
                    id: 'underline',
                    label: '下划线',
                    active: Boolean(currentEditor?.isActive('underline')),
                    run: toggleUnderline
                },
                {
                    id: 'strike',
                    label: '删除线',
                    active: Boolean(currentEditor?.isActive('strike')),
                    run: toggleStrike
                },
                {
                    id: 'code',
                    label: '行内代码',
                    active: Boolean(currentEditor?.isActive('code')),
                    run: toggleCode
                },
                {
                    id: 'link',
                    label: '链接',
                    active: Boolean(currentEditor?.isActive('link')),
                    run: setLink
                }
            ]
        },
        {
            id: 'heading',
            label: '标题',
            items: [
                {
                    id: 'heading-1',
                    label: 'H1',
                    active: Boolean(currentEditor?.isActive('heading', { level: 1 })),
                    run: () => toggleHeading(1)
                },
                {
                    id: 'heading-2',
                    label: 'H2',
                    active: Boolean(currentEditor?.isActive('heading', { level: 2 })),
                    run: () => toggleHeading(2)
                },
                {
                    id: 'heading-3',
                    label: 'H3',
                    active: Boolean(currentEditor?.isActive('heading', { level: 3 })),
                    run: () => toggleHeading(3)
                }
            ]
        },
        {
            id: 'structure',
            label: '块结构',
            items: [
                {
                    id: 'bullet-list',
                    label: '无序列表',
                    active: Boolean(currentEditor?.isActive('bulletList')),
                    run: toggleBulletList
                },
                {
                    id: 'ordered-list',
                    label: '有序列表',
                    active: Boolean(currentEditor?.isActive('orderedList')),
                    run: toggleOrderedList
                },
                {
                    id: 'task-list',
                    label: '任务列表',
                    active: Boolean(currentEditor?.isActive('taskList')),
                    run: toggleTaskList
                },
                {
                    id: 'blockquote',
                    label: '引用',
                    active: Boolean(currentEditor?.isActive('blockquote')),
                    run: toggleBlockquote
                },
                {
                    id: 'divider',
                    label: '分割线',
                    active: false,
                    run: insertDivider
                },
                {
                    id: 'align-left',
                    label: '左对齐',
                    active: Boolean(currentEditor?.isActive({ textAlign: 'left' })),
                    run: () => setTextAlign('left')
                },
                {
                    id: 'align-center',
                    label: '居中对齐',
                    active: Boolean(currentEditor?.isActive({ textAlign: 'center' })),
                    run: () => setTextAlign('center')
                },
                {
                    id: 'align-right',
                    label: '右对齐',
                    active: Boolean(currentEditor?.isActive({ textAlign: 'right' })),
                    run: () => setTextAlign('right')
                }
            ]
        },
        {
            id: 'insert',
            label: '插入内容',
            items: [
                {
                    id: 'table',
                    label: '表格',
                    active: false,
                    run: () => insertTable(true)
                },
                {
                    id: 'table-no-header',
                    label: '无表头表格',
                    active: false,
                    run: () => insertTable(false)
                },
                {
                    id: 'insert-image',
                    label: imageUploading.value ? '上传中...' : '插入图片',
                    active: false,
                    run: triggerImageUpload,
                    disabled: imageUploading.value
                },
                {
                    id: 'insert-image-link',
                    label: '图片链接',
                    active: false,
                    run: openImageUrlDialog
                },
                {
                    id: 'code-block',
                    label: '代码块',
                    active: Boolean(currentEditor?.isActive('codeBlock')),
                    run: () => insertCodeBlock('text')
                }
            ]
        }
    ];
});

const {width: editorWidth} = useWindowSize();
const isMobileEditor = computed(() => editorWidth.value < 768);
const MOBILE_HIDDEN_GROUPS = new Set(['history']);
const MOBILE_HIDDEN_ITEMS = new Set(['align-left', 'align-center', 'align-right', 'strike', 'underline', 'task-list', 'divider']);
const visibleToolbarGroups = computed(() => {
    if (!isMobileEditor.value) return toolbarGroups.value;
    return toolbarGroups.value
        .filter(g => !MOBILE_HIDDEN_GROUPS.has(g.id))
        .map(g => ({
            ...g,
            items: g.items.filter(item => !MOBILE_HIDDEN_ITEMS.has(item.id))
        }))
        .filter(g => g.items.length > 0);
});

const contextMenuMode = ref('default');

const tableContextMenuItems = [
    {
        id: 'merge',
        label: '合并',
        items: [
            { id: 'merge-cells', label: '合并单元格', run: mergeCells },
            { id: 'split-cell', label: '拆分单元格', run: splitCell }
        ]
    },
    {
        id: 'rows',
        label: '行操作',
        items: [
            { id: 'add-row-before', label: '上方插入行', run: addRowBefore },
            { id: 'add-row-after', label: '下方插入行', run: addRowAfter },
            { id: 'delete-row', label: '删除行', run: deleteRow }
        ]
    },
    {
        id: 'columns',
        label: '列操作',
        items: [
            { id: 'add-col-before', label: '左侧插入列', run: addColumnBefore },
            { id: 'add-col-after', label: '右侧插入列', run: addColumnAfter },
            { id: 'delete-col', label: '删除列', run: deleteColumn }
        ]
    },
    {
        id: 'align',
        label: '对齐',
        items: [
            { id: 'align-left', label: '左对齐', run: () => setTextAlign('left') },
            { id: 'align-center', label: '居中对齐', run: () => setTextAlign('center') },
            { id: 'align-right', label: '右对齐', run: () => setTextAlign('right') }
        ]
    },
    {
        id: 'header',
        label: '表头',
        items: [
            { id: 'toggle-header-row', label: '切换行表头', run: toggleHeaderRow },
            { id: 'toggle-header-column', label: '切换列表头', run: toggleHeaderColumn },
            { id: 'toggle-header-cell', label: '切换单元格表头', run: toggleHeaderCell }
        ]
    },
    {
        id: 'table-actions',
        label: '表格',
        items: [
            { id: 'delete-table', label: '删除表格', run: deleteTable }
        ]
    }
];

const contextMenuItems = computed(() => {
    if (contextMenuMode.value === 'table') {
        return tableContextMenuItems;
    }
    return toolbarGroups.value;
});

// 快捷键 Tooltip 映射
const itemShortcuts = {
    'undo': 'Ctrl+Z',
    'redo': 'Ctrl+Y',
    'bold': 'Ctrl+B',
    'italic': 'Ctrl+I',
    'underline': 'Ctrl+U',
    'strike': 'Ctrl+Shift+S',
    'code': 'Ctrl+`',
    'link': 'Ctrl+K',
    'heading-1': 'Ctrl+Alt+1',
    'heading-2': 'Ctrl+Alt+2',
    'heading-3': 'Ctrl+Alt+3',
    'bullet-list': 'Ctrl+Shift+8',
    'ordered-list': 'Ctrl+Shift+7',
    'blockquote': 'Ctrl+Shift+B',
    'code-block': 'Ctrl+Alt+C',
    'table': '/',
    'table-no-header': '/',
    'insert-image': '/',
};

const getItemTitle = (item) => {
    const shortcut = itemShortcuts[item.id];
    if (shortcut) {
        return `${item.label} (${shortcut})`;
    }
    return item.label;
};

const itemIcons = {
    'undo': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M4.7 5H10a3.5 3.5 0 0 1 0 7H7v-1.5h3a2 2 0 1 0 0-4H4.7l1.8 1.8L5.14 9.7 1.7 6.25 5.14 2.8l1.36 1.36L4.7 5z"/></svg>',
    'redo': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M11.3 5H6a3.5 3.5 0 0 0 0 7h3v-1.5H6a2 2 0 1 1 0-4h5.3l-1.8 1.8L10.86 9.7l3.44-3.45-3.44-3.45L9.5 4.16 11.3 5z"/></svg>',
    'bold': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M11.06 7.44A3.17 3.17 0 0 0 12.5 5c0-1.74-1.3-3-3.26-3H4v12h5.85c2.04 0 3.64-1.16 3.64-3.2 0-1.64-1.16-2.83-2.43-3.36zM6.5 4.5h2.4c.96 0 1.6.52 1.6 1.3 0 .8-.64 1.34-1.6 1.34H6.5V4.5zm2.62 7H6.5v-3.2h2.62c1.06 0 1.78.58 1.78 1.6 0 1.02-.72 1.6-1.78 1.6z"/></svg>',
    'italic': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M9.5 2.5L8 2l-2 11.5 1.5.5 2-11.5z"/><path d="M5 3h6v1.5H5V3zm0 8.5h6V13H5v-1.5z"/></svg>',
    'underline': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M3 14h10v1.5H3V14zM4 1.5h2v5a2 2 0 0 0 2 2V1.5h2v5a4 4 0 0 1-8 0v-5z"/></svg>',
    'strike': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M12.5 8.5c-.12.69-.38 1.23-.78 1.62-.4.39-.9.68-1.5.87s-1.27.3-1.99.3c-.54 0-1.07-.07-1.59-.21s-.98-.34-1.36-.59l-.25 1.35c.4.27.9.5 1.5.68s1.24.27 1.91.27c.92 0 1.74-.14 2.46-.42s1.35-.67 1.86-1.18c.51-.51.9-1.12 1.17-1.85.13-.36.2-.75.2-1.17 0-.68-.17-1.27-.52-1.77-.35-.5-.84-.86-1.47-1.08.7.23 1.23.62 1.59 1.17s.57 1.27.57 2.18zM1.5 7.25h13v1.5h-13v-1.5z"/></svg>',
    'code': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M5.85 4.85L2.7 8l3.15 3.15-1.06 1.06L.59 8.7a1 1 0 0 1 0-1.4l4.2-4.2 1.06 1.06zm4.3 0l3.15 3.15-3.15 3.15 1.06 1.06 4.2-4.2a1 1 0 0 0 0-1.4l-4.2-4.2-1.06 1.06z"/></svg>',
    'heading-1': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M2 3h2v4h4V3h2v10H8V9H4v4H2V3zm12 0h-2v10h2V3z"/></svg>',
    'heading-2': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M2 3h2v4h4V3h2v10H8V9H4v4H2V3zm12.5 4.5c0-.83-.67-1.5-1.5-1.5H11v2h1.5v5H14v-5.5z"/></svg>',
    'heading-3': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M2 3h2v4h4V3h2v10H8V9H4v4H2V3zm9.5 4h2.5l-2.8 3.5 2.8 3.5h-2.5L11 12.5 9.8 14h-2.5l2.8-3.5L7.3 7h2.5l1.2 1.5L12.2 7z"/></svg>',
    'bullet-list': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M3.5 3.5a1 1 0 1 0 0-2 1 1 0 0 0 0 2zm0 5a1 1 0 1 0 0-2 1 1 0 0 0 0 2zm0 5a1 1 0 1 0 0-2 1 1 0 0 0 0 2zM7 4h7V2.5H7V4zm0 5h7V7.5H7V9zm0 5h7v-1.5H7V14z"/></svg>',
    'ordered-list': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M2.5 2.5V1h1v3h-1V3H2v-1h.5zM1 5h2v.5H1.5V6H3v.5H1V7h3V4.5H1V5zm1 5.5V9H1V8h2v2.5h-1zm.5 1H1v-1h2v.5H1.5V13H3v.5H1V14h3v-2.5H2.5zM7 4h7V2.5H7V4zm0 5h7V7.5H7V9zm0 5h7v-1.5H7V14z"/></svg>',
    'task-list': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M3 2.5h10a1.5 1.5 0 0 1 1.5 1.5v8a1.5 1.5 0 0 1-1.5 1.5H3A1.5 1.5 0 0 1 1.5 12V4A1.5 1.5 0 0 1 3 2.5zm0 1a.5.5 0 0 0-.5.5v8a.5.5 0 0 0 .5.5h10a.5.5 0 0 0 .5-.5V4a.5.5 0 0 0-.5-.5H3z"/><path d="M6.5 9.8l3.65-3.65.7.7-4.35 4.35-2.15-2.15.7-.7L6.5 9.8z"/></svg>',
    'blockquote': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M2.5 3h5v5.5c0 2.38-1.07 4.12-3.12 5.12l-1.26-1.5c1.58-.83 2.38-2.12 2.38-3.12V9H2.5V3zm8 0h5v5.5c0 2.38-1.07 4.12-3.12 5.12l-1.26-1.5c1.58-.83 2.38-2.12 2.38-3.12V9h-3V3z"/></svg>',
    'link': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M6.88 3.88a3 3 0 0 1 4.24 0l1 1a3 3 0 0 1-4.24 4.24l-.5-.5.94-.94.5.5a1.5 1.5 0 0 0 2.12-2.12l-1-1a1.5 1.5 0 0 0-2.12 0l-.5.5-.94-.94.5-.5z"/><path d="M4.88 6.88a3 3 0 0 1 4.24 0l.5.5-.94.94-.5-.5a1.5 1.5 0 0 0-2.12 2.12l1 1a1.5 1.5 0 0 0 2.12 0l.5-.5.94.94-.5.5a3 3 0 0 1-4.24-4.24l-1-1a3 3 0 0 1 0-4.24V6.88z"/></svg>',
    'divider': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M2 7.5h12v1H2z"/></svg>',
    'table': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M2 2h12v12H2V2zm1 1v3h4V3H3zm5 0v3h4V3H8zM3 7v3h4V7H3zm5 0v3h4V7H8zM3 11v2h4v-2H3zm5 0v2h4v-2H8z"/></svg>',
    'table-no-header': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M2 3h12v10H2V3zm1 1v3h4V4H3zm5 0v3h4V4H8zM3 8v4h4V8H3zm5 0v4h4V8H8z"/></svg>',
    'insert-image': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M2 2h12v12H2V2zm1 1v10h10V3H3zm2 3a1.5 1.5 0 1 1 3 0 1.5 1.5 0 0 1-3 0zm7 6H4l3-4 2 2.5L11 8l2 4z"/></svg>',
    'insert-image-link': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M2 2h12v12H2V2zm1 1v10h10V3H3zm3 3h4v1H6V6zm0 2h7v1H6V8zm0 2h5v1H6v-1z"/></svg>',
    'code-block': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M5.85 4.85L2.7 8l3.15 3.15-1.06 1.06L.59 8.7a1 1 0 0 1 0-1.4l4.2-4.2 1.06 1.06zm4.3 0l3.15 3.15-3.15 3.15 1.06 1.06 4.2-4.2a1 1 0 0 0 0-1.4l-4.2-4.2-1.06 1.06z"/></svg>',
    'add-row-before': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M2 8h12v1H2V8zm6-6l3 3H9v3H7V5H5l3-3zM2 12h12v1H2v-1z"/></svg>',
    'add-row-after': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M2 8h12v1H2V8zm0-3h12v1H2V5zm3 5l3 3 3-3H9V7H7v3H5z"/></svg>',
    'delete-row': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M2 8h12v1H2V8zm3-5h1v3H5V3zm5 0h1v3h-1V3zM5 9h1v4H5V9zm5 0h1v4h-1V9z"/></svg>',
    'add-col-before': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M8 2v12H7V2h1zM2 3h3v10H2V3zm9 0h3v10h-3V3zM5 6L2 9l3 3V9h3V7H5V6z"/></svg>',
    'add-col-after': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M8 2v12H7V2h1zM2 3h3v10H2V3zm9 3l3 3-3 3V9H9V7h3V6z"/></svg>',
    'delete-col': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M8 2v12H7V2h1zM2 3h1v10H2V3zm11 0h1v10h-1V3zM4 6h1v4H4V6zm2 0h1v4H6V6z"/></svg>',
    'toggle-header-row': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M2 2h12v3H2V2zm0 4h12v1H2V6zm0 3h12v1H2V9zm0 3h12v1H2v-1z"/></svg>',
    'toggle-header-column': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M2 2h3v12H2V2zm4 0h1v12H6V2zm3 0h1v12H9V2zm3 0h2v12h-2V2z"/></svg>',
    'toggle-header-cell': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M2 2h12v12H2V2zm1 1v3h4V3H3zm5 0v3h4V3H8zM3 7v3h4V7H3zm5 0v3h4V7H8zM3 11v3h4v-3H3zm5 0v3h4v-3H8z"/></svg>',
    'merge-cells': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M2 2h12v12H2V2zm1 1v3h4V3H3zm5 0v3h4V3H8zM3 7v3h4V7H3zm5 0v3h4V7H8zM3 11v2h4v-2H3zm5 0v2h4v-2H8z"/><path d="M6.5 8l-2 2V9H1.5V7H4.5V6l2 2zm3 0l2-2v1h3v2h-3v1l-2-2z" fill="currentColor"/></svg>',
    'split-cell': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M2 2h12v12H2V2zm1 1v3h4V3H3zm5 0v3h4V3H8zM3 7v3h4V7H3zm5 0v3h4V7H8zM3 11v2h4v-2H3zm5 0v2h4v-2H8z"/><path d="M8 6.5l2 2H9v3H7V8.5H6l2-2zm0 3l-2 2h1v3h2v-3h1l-2-2z" fill="currentColor"/></svg>',
    'align-left': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M1 2h14v1.5H1V2zm0 4h10v1.5H1V6zm0 4h14v1.5H1V10zm0 4h10v1.5H1V14z"/></svg>',
    'align-center': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M1 2h14v1.5H1V2zm2 4h10v1.5H3V6zm-2 4h14v1.5H1V10zm2 4h10v1.5H3V14z"/></svg>',
    'align-right': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M1 2h14v1.5H1V2zm4 4h10v1.5H5V6zm-4 4h14v1.5H1V10zm4 4h10v1.5H5V14z"/></svg>',
    'delete-table': '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M2 2h12v12H2V2zm1 1v3h4V3H3zm5 0v3h4V3H8zM3 7v3h4V7H3zm5 0v3h4V7H8zM3 11v2h4v-2H3zm5 0v2h4v-2H8z"/><path d="M10.5 4.5l3 3m-3 0l3-3" stroke="currentColor" stroke-width="1.5"/></svg>'
};

const itemIconsHtml = (id) => itemIcons[id] || '';

const runToolbarItem = (item) => {
    item?.run();
};

const runContextMenuItem = (item) => {
    if (!item) {
        return;
    }
    item.run();
    closeContextMenu();
};

const adjustContextMenuPosition = (clientX, clientY) => {
    if (!contextMenuState.open || !contextMenuRef.value) {
        return;
    }
    const menu = contextMenuRef.value.getBoundingClientRect();
    const margin = 10;
    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;

    let left = clientX;
    let top = clientY;

    // Prevent right edge overflow
    if (left + menu.width > viewportWidth - margin) {
        left = viewportWidth - margin - menu.width;
    }
    // Prevent left edge overflow
    if (left < margin) {
        left = margin;
    }

    // Prevent bottom edge overflow — flip above cursor if there's room
    if (top + menu.height > viewportHeight - margin) {
        const flippedTop = clientY - menu.height;
        if (flippedTop >= margin) {
            top = flippedTop;
        } else {
            top = margin;
        }
    }
    // Prevent top edge overflow
    if (top < margin) {
        top = margin;
    }

    contextMenuState.left = Math.round(left);
    contextMenuState.top = Math.round(top);
};

const handleEditorContextMenu = (event) => {
    if (!editor.value || !editorRoot.value) {
        return;
    }
    const target = event.target;
    if (!(target instanceof HTMLElement)) {
        return;
    }
    if (!target.closest('.ProseMirror')) {
        return;
    }
    if (target.closest('.rich-code-block') || target.closest('.editor-context-menu')) {
        closeContextMenu();
        return;
    }

    event.preventDefault();
    closeSlashMenu();
    contextMenuMode.value = editor.value.isActive('table') ? 'table' : 'default';

    if (editor.value.state.selection.empty) {
        const position = editor.value.view.posAtCoords({
            left: event.clientX,
            top: event.clientY
        });
        if (position?.pos) {
            editor.value.chain().focus().setTextSelection(position.pos).run();
        }
    } else {
        editor.value.chain().focus().run();
    }

    contextMenuState.open = true;

    // Use viewport-relative coordinates for fixed positioning
    nextTick(() => {
        adjustContextMenuPosition(event.clientX, event.clientY);
    });
};

const handleGlobalPointerDown = (event) => {
    const target = event.target;
    const insideColorPalette = toolbarColorPaletteRef.value?.contains(target)
        || bubbleColorPaletteRef.value?.contains(target);
    if (activeColorPalette.value && !insideColorPalette) {
        closeColorPalette();
    }
    if (!contextMenuState.open) {
        return;
    }
    if (contextMenuRef.value?.contains(target)) {
        return;
    }
    closeContextMenu();
};

const handleGlobalKeyDown = (event) => {
    if (event.key === 'Escape') {
        closeContextMenu();
        closeColorPalette();
    }
};

const handleGlobalScroll = (event) => {
    if (toolbarColorPaletteRef.value?.contains(event.target) || bubbleColorPaletteRef.value?.contains(event.target)) {
        return;
    }
    closeColorPalette();
    if (contextMenuRef.value?.contains(event.target)) {
        return;
    }
    closeContextMenu();
};
</script>

<template>
    <section class="rich-markdown-editor">
        <input
            ref="imageInputRef"
            type="file"
            accept="image/*"
            class="hidden-input"
            @change="handleImageSelected"
        >
        <div class="editor-sticky-stack">
            <div
                v-if="editor"
                class="editor-toolbar"
                contenteditable="false"
                :data-editor-state="editorStateVersion"
            >
                <div
                    class="editor-toolbar-group editor-style-controls"
                    aria-label="文字外观"
                >
                    <select
                        class="editor-style-select editor-style-select--font"
                        data-testid="editor-font-family-select"
                        :value="activeFontFamily"
                        :disabled="!isRichStyleAvailable"
                        title="字体"
                        aria-label="字体"
                        @mousedown.stop="rememberTextSelection({ allowEmpty: true })"
                        @change="applyFontFamily($event.target.value)"
                    >
                        <option
                            v-for="option in FONT_FAMILY_OPTIONS"
                            :key="option.label"
                            :value="option.value"
                        >
                            {{ option.label }}
                        </option>
                    </select>
                    <select
                        class="editor-style-select editor-style-select--size"
                        data-testid="editor-font-size-select"
                        :value="activeFontSize"
                        :disabled="!isRichStyleAvailable"
                        title="字号"
                        aria-label="字号"
                        @mousedown.stop="rememberTextSelection({ allowEmpty: true })"
                        @change="applyFontSize($event.target.value)"
                    >
                        <option
                            v-for="option in FONT_SIZE_OPTIONS"
                            :key="option.label"
                            :value="option.value"
                        >
                            {{ option.label }}
                        </option>
                    </select>
                    <select
                        class="editor-style-select editor-style-select--line"
                        data-testid="editor-line-height-select"
                        :value="activeLineHeight"
                        :disabled="!isRichStyleAvailable"
                        title="行距"
                        aria-label="行距"
                        @mousedown.stop="rememberTextSelection({ allowEmpty: true })"
                        @change="applyLineHeight($event.target.value)"
                    >
                        <option
                            v-for="option in LINE_HEIGHT_OPTIONS"
                            :key="option.label"
                            :value="option.value"
                        >
                            {{ option.label }}
                        </option>
                    </select>
                    <div
                        ref="toolbarColorPaletteRef"
                        class="editor-color-picker"
                    >
                        <button
                            type="button"
                            class="editor-color-trigger"
                            data-testid="editor-text-color-input"
                            title="文字颜色"
                            aria-label="文字颜色"
                            :aria-expanded="isToolbarColorPaletteOpen"
                            :disabled="!isRichStyleAvailable"
                            @mousedown.prevent="rememberTextSelection({ allowEmpty: true })"
                            @click="toggleColorPalette('toolbar')"
                        >
                            <span
                                class="editor-color-letter"
                                :style="{ color: activeTextColor || DEFAULT_TEXT_COLOR }"
                            >A</span>
                            <span
                                class="editor-color-indicator"
                                :style="{ backgroundColor: activeTextColor || DEFAULT_TEXT_COLOR }"
                            ></span>
                        </button>
                        <div
                            v-if="isToolbarColorPaletteOpen"
                            class="editor-color-popover"
                            role="dialog"
                            aria-label="颜色选择"
                        >
                            <section class="editor-color-section">
                                <p class="editor-color-title">字体颜色</p>
                                <div class="editor-color-grid editor-color-grid--text">
                                    <button
                                        v-for="option in TEXT_COLOR_OPTIONS"
                                        :key="option.value"
                                        type="button"
                                        class="editor-color-option editor-color-option--text"
                                        :class="{ active: (activeTextColor || DEFAULT_TEXT_COLOR) === option.value }"
                                        :title="option.label"
                                        :aria-label="`字体颜色：${option.label}`"
                                        @mousedown.prevent="rememberTextSelection({ allowEmpty: true })"
                                        @click="applyTextColor(option.value)"
                                    >
                                        <span :style="{ color: option.value }">A</span>
                                    </button>
                                </div>
                            </section>
                            <section class="editor-color-section">
                                <p class="editor-color-title">背景颜色</p>
                                <div class="editor-color-grid editor-color-grid--background">
                                    <button
                                        type="button"
                                        class="editor-color-option editor-color-option--none"
                                        :class="{ active: !activeBackgroundColor }"
                                        title="无背景色"
                                        aria-label="无背景色"
                                        @mousedown.prevent="rememberTextSelection({ allowEmpty: true })"
                                        @click="clearBackgroundColor"
                                    ></button>
                                    <button
                                        v-for="option in BACKGROUND_COLOR_OPTIONS"
                                        :key="option.value"
                                        type="button"
                                        class="editor-color-option editor-color-option--background"
                                        :class="{ active: activeBackgroundColor === option.value }"
                                        :style="{ backgroundColor: option.value }"
                                        :title="option.label"
                                        :aria-label="`背景颜色：${option.label}`"
                                        @mousedown.prevent="rememberTextSelection({ allowEmpty: true })"
                                        @click="applyBackgroundColor(option.value)"
                                    ></button>
                                </div>
                            </section>
                            <button
                                type="button"
                                class="editor-color-reset"
                                @mousedown.prevent="rememberTextSelection({ allowEmpty: true })"
                                @click="resetColorPalette"
                            >
                                恢复默认
                            </button>
                        </div>
                    </div>
                    <button
                        type="button"
                        class="editor-style-clear"
                        data-testid="editor-text-color-clear"
                        title="清除文字颜色"
                        aria-label="清除文字颜色"
                        :disabled="!activeTextColor || !isRichStyleAvailable"
                        @mousedown.prevent="keepEditorFocus"
                        @click="clearTextColor"
                    >
                        A
                    </button>
                    <button
                        type="button"
                        class="editor-style-clear"
                        data-testid="editor-text-style-clear"
                        title="清除文字样式"
                        aria-label="清除文字样式"
                        :disabled="!isRichStyleAvailable"
                        @mousedown.prevent="keepEditorFocus"
                        @click="clearTextStyle"
                    >
                        Tx
                    </button>
                </div>
                <div
                    v-for="group in visibleToolbarGroups"
                    :key="group.id"
                    class="editor-toolbar-group"
                    :aria-label="group.label"
                >
                    <button
                        v-for="item in group.items"
                        :key="item.id"
                        type="button"
                        :class="{ active: item.active, disabled: item.disabled }"
                        :disabled="item.disabled"
                        :title="getItemTitle(item)"
                        :aria-label="item.label"
                        @mousedown.prevent="keepEditorFocus"
                        @click="runToolbarItem(item)"
                    >
                        <span v-html="itemIconsHtml(item.id)"></span>
                    </button>
                </div>
            </div>
            <div v-if="editor && isInTable" class="table-context-toolbar" contenteditable="false">
                <span class="table-toolbar-label">表格</span>
                <span class="table-toolbar-divider"></span>
                <button type="button" class="table-toolbar-btn" title="上方插入行" @mousedown.prevent="keepEditorFocus" @click="addRowBefore"><span v-html="itemIconsHtml('add-row-before')"></span></button>
                <button type="button" class="table-toolbar-btn" title="下方插入行" @mousedown.prevent="keepEditorFocus" @click="addRowAfter"><span v-html="itemIconsHtml('add-row-after')"></span></button>
                <button type="button" class="table-toolbar-btn" title="删除行" @mousedown.prevent="keepEditorFocus" @click="deleteRow"><span v-html="itemIconsHtml('delete-row')"></span></button>
                <span class="table-toolbar-divider"></span>
                <button type="button" class="table-toolbar-btn" title="左侧插入列" @mousedown.prevent="keepEditorFocus" @click="addColumnBefore"><span v-html="itemIconsHtml('add-col-before')"></span></button>
                <button type="button" class="table-toolbar-btn" title="右侧插入列" @mousedown.prevent="keepEditorFocus" @click="addColumnAfter"><span v-html="itemIconsHtml('add-col-after')"></span></button>
                <button type="button" class="table-toolbar-btn" title="删除列" @mousedown.prevent="keepEditorFocus" @click="deleteColumn"><span v-html="itemIconsHtml('delete-col')"></span></button>
                <span class="table-toolbar-divider"></span>
                <button
                    type="button"
                    class="table-header-toggle"
                    :class="{ active: tableHasHeaderRow }"
                    :title="tableHasHeaderRow ? '关闭表头' : '启用表头'"
                    :aria-pressed="tableHasHeaderRow"
                    @mousedown.prevent="keepEditorFocus"
                    @click="toggleHeaderRow"
                >
                    <span v-html="itemIconsHtml('toggle-header-row')"></span>
                    <span>表头</span>
                    <strong>{{ tableHasHeaderRow ? '开' : '关' }}</strong>
                </button>
                <span class="table-toolbar-divider"></span>
                <button type="button" class="table-toolbar-btn table-toolbar-btn--danger" title="删除表格" @mousedown.prevent="keepEditorFocus" @click="deleteTable"><span v-html="itemIconsHtml('delete-table')"></span></button>
            </div>
        </div>
        <div
            ref="editorRoot"
            class="editor-body"
            :class="{ 'drag-over': dragOver }"
            @contextmenu="handleEditorContextMenu"
            @dragover="handleEditorDragOver"
            @dragleave="handleEditorDragLeave"
            @drop="handleEditorDrop"
        >
            <div v-if="dragOver" class="editor-drop-hint" contenteditable="false">松开上传图片</div>
            <div
                v-if="slashState.open"
                class="slash-command-panel"
                :style="{ top: `${slashState.top}px`, left: `${slashState.left}px` }"
                contenteditable="false"
            >
                <p class="slash-command-title">插入内容</p>
                <button
                    v-for="(command, index) in filteredSlashCommands"
                    :key="command.id"
                    type="button"
                    class="slash-command-item"
                    :class="{ active: index === slashState.selectedIndex }"
                    @mousedown.prevent="activateSlashCommand(command)"
                >
                    <span>{{ command.label }}</span>
                    <small>{{ command.description }}</small>
                </button>
                <p v-if="!filteredSlashCommands.length" class="slash-command-empty">没有找到匹配命令</p>
            </div>
            <div
                v-if="contextMenuState.open"
                ref="contextMenuRef"
                class="editor-context-menu"
                :style="{ top: `${contextMenuState.top}px`, left: `${contextMenuState.left}px` }"
                contenteditable="false"
            >
                <template v-for="(group, groupIndex) in contextMenuItems" :key="group.id">
                    <div v-if="groupIndex > 0" class="context-menu-divider"></div>
                    <div class="context-menu-group">
                        <span class="context-menu-group-label">{{ group.label }}</span>
                        <button
                            v-for="item in group.items"
                            :key="item.id"
                            type="button"
                            class="editor-context-item"
                            :class="{ active: item.active }"
                            @mousedown.prevent="runContextMenuItem(item)"
                        >
                            <span class="context-item-icon" v-html="itemIconsHtml(item.id)"></span>
                            <span class="context-item-label">{{ item.label }}</span>
                        </button>
                    </div>
                </template>
            </div>
            <BubbleMenu
                v-if="editor"
                :editor="editor"
                class="editor-bubble-menu"
                :tippy-options="{ duration: 150, placement: 'top' }"
            >
                <button
                    type="button"
                    class="bubble-menu-btn"
                    :class="{ active: editor.isActive('bold') }"
                    title="加粗"
                    @click="toggleBold"
                ><span v-html="itemIconsHtml('bold')"></span></button>
                <button
                    type="button"
                    class="bubble-menu-btn"
                    :class="{ active: editor.isActive('italic') }"
                    title="斜体"
                    @click="toggleItalic"
                ><span v-html="itemIconsHtml('italic')"></span></button>
                <button
                    type="button"
                    class="bubble-menu-btn"
                    :class="{ active: editor.isActive('strike') }"
                    title="删除线"
                    @click="toggleStrike"
                ><span v-html="itemIconsHtml('strike')"></span></button>
                <button
                    type="button"
                    class="bubble-menu-btn"
                    :class="{ active: editor.isActive('code') }"
                    title="行内代码"
                    @click="toggleCode"
                ><span v-html="itemIconsHtml('code')"></span></button>
                <span class="bubble-menu-sep"></span>
                <div
                    ref="bubbleColorPaletteRef"
                    class="editor-color-picker editor-color-picker--bubble"
                >
                    <button
                        type="button"
                        class="editor-color-trigger bubble-menu-btn"
                        title="文字颜色"
                        aria-label="文字颜色"
                        :aria-expanded="isBubbleColorPaletteOpen"
                        :disabled="!isRichStyleAvailable"
                        @mousedown.prevent="rememberTextSelection({ allowEmpty: true })"
                        @click="toggleColorPalette('bubble')"
                    >
                        <span
                            class="editor-color-letter"
                            :style="{ color: activeTextColor || DEFAULT_TEXT_COLOR }"
                        >A</span>
                        <span
                            class="editor-color-indicator"
                            :style="{ backgroundColor: activeTextColor || DEFAULT_TEXT_COLOR }"
                        ></span>
                    </button>
                    <div
                        v-if="isBubbleColorPaletteOpen"
                        class="editor-color-popover editor-color-popover--bubble"
                        role="dialog"
                        aria-label="颜色选择"
                    >
                        <section class="editor-color-section">
                            <p class="editor-color-title">字体颜色</p>
                            <div class="editor-color-grid editor-color-grid--text">
                                <button
                                    v-for="option in TEXT_COLOR_OPTIONS"
                                    :key="option.value"
                                    type="button"
                                    class="editor-color-option editor-color-option--text"
                                    :class="{ active: (activeTextColor || DEFAULT_TEXT_COLOR) === option.value }"
                                    :title="option.label"
                                    :aria-label="`字体颜色：${option.label}`"
                                    @mousedown.prevent="rememberTextSelection({ allowEmpty: true })"
                                    @click="applyTextColor(option.value)"
                                >
                                    <span :style="{ color: option.value }">A</span>
                                </button>
                            </div>
                        </section>
                        <section class="editor-color-section">
                            <p class="editor-color-title">背景颜色</p>
                            <div class="editor-color-grid editor-color-grid--background">
                                <button
                                    type="button"
                                    class="editor-color-option editor-color-option--none"
                                    :class="{ active: !activeBackgroundColor }"
                                    title="无背景色"
                                    aria-label="无背景色"
                                    @mousedown.prevent="rememberTextSelection({ allowEmpty: true })"
                                    @click="clearBackgroundColor"
                                ></button>
                                <button
                                    v-for="option in BACKGROUND_COLOR_OPTIONS"
                                    :key="option.value"
                                    type="button"
                                    class="editor-color-option editor-color-option--background"
                                    :class="{ active: activeBackgroundColor === option.value }"
                                    :style="{ backgroundColor: option.value }"
                                    :title="option.label"
                                    :aria-label="`背景颜色：${option.label}`"
                                    @mousedown.prevent="rememberTextSelection({ allowEmpty: true })"
                                    @click="applyBackgroundColor(option.value)"
                                ></button>
                            </div>
                        </section>
                        <button
                            type="button"
                            class="editor-color-reset"
                            @mousedown.prevent="rememberTextSelection({ allowEmpty: true })"
                            @click="resetColorPalette"
                        >
                            恢复默认
                        </button>
                    </div>
                </div>
                <button
                    type="button"
                    class="bubble-menu-btn"
                    :class="{ active: editor.isActive('link') }"
                    title="链接"
                    @click="setLink"
                ><span v-html="itemIconsHtml('link')"></span></button>
            </BubbleMenu>
            <EditorContent :editor="editor" />
        </div>
        <p v-if="uploadError" class="upload-error">{{ uploadError }}</p>
    </section>

    <Teleport to="body">
        <div
            v-if="imageUrlDialog.open"
            class="image-url-overlay"
            @click.self="cancelImageUrl"
        >
            <div class="image-url-dialog" role="dialog" aria-modal="true" aria-label="插入图片链接">
                <div class="image-url-header">
                    <h3>插入图片链接</h3>
                    <button type="button" class="image-url-close" @click="cancelImageUrl">&times;</button>
                </div>
                <div class="image-url-body">
                    <label class="image-url-label" for="image-url-input">图片链接地址</label>
                    <input
                        id="image-url-input"
                        v-model="imageUrlDialog.url"
                        type="url"
                        placeholder="https://example.com/image.jpg"
                        class="image-url-input"
                        autofocus
                        @keydown.enter="confirmImageUrl"
                    >
                    <p v-if="imageUrlDialog.error" class="image-url-error">{{ imageUrlDialog.error }}</p>
                    <div v-if="imageUrlDialog.url && !imageUrlDialog.error" class="image-url-preview">
                        <img :src="imageUrlDialog.url" alt="图片预览" @error="false" decoding="async">
                    </div>
                </div>
                <div class="image-url-actions">
                    <button type="button" class="image-url-cancel" @click="cancelImageUrl">取消</button>
                    <button type="button" class="image-url-confirm" @click="confirmImageUrl">插入</button>
                </div>
            </div>
        </div>
    </Teleport>

    <Teleport to="body">
        <div
            v-if="linkDialog.open"
            class="image-url-overlay"
            @click.self="cancelLink"
        >
            <div class="image-url-dialog" role="dialog" aria-modal="true" aria-label="插入链接">
                <div class="image-url-header">
                    <h3>插入链接</h3>
                    <button type="button" class="image-url-close" @click="cancelLink">&times;</button>
                </div>
                <div class="image-url-body">
                    <label class="image-url-label" for="link-url-input">链接地址</label>
                    <input
                        id="link-url-input"
                        v-model="linkDialog.url"
                        type="url"
                        placeholder="https://example.com 或 /articles/123"
                        class="image-url-input"
                        autofocus
                        @keydown.enter="confirmLink"
                    >
                    <p v-if="linkDialog.error" class="image-url-error">{{ linkDialog.error }}</p>
                </div>
                <div class="image-url-actions">
                    <button type="button" class="image-url-cancel" @click="cancelLink">取消</button>
                    <button type="button" class="image-url-confirm" @click="confirmLink">确认</button>
                </div>
            </div>
        </div>
    </Teleport>
</template>

<style scoped>
.hidden-input {
    position: absolute;
    width: 1px;
    height: 1px;
    padding: 0;
    margin: -1px;
    overflow: hidden;
    clip: rect(0, 0, 0, 0);
    white-space: nowrap;
    border: 0;
}

.upload-error {
    margin: 8px 0;
    padding: 8px 12px;
    color: #d14343;
    font-size: 13px;
    background: rgba(209, 67, 67, 0.06);
    border-radius: var(--radius-sm);
}

.editor-body {
    position: relative;
}

.editor-body.drag-over {
    outline: 2px dashed rgba(37, 99, 235, 0.55);
    outline-offset: 4px;
}

.editor-drop-hint {
    position: absolute;
    inset: 10px;
    z-index: 30;
    display: grid;
    place-items: center;
    color: var(--brand);
    font-size: 15px;
    font-weight: 700;
    pointer-events: none;
    background: rgba(239, 246, 255, 0.82);
    border: 1px solid rgba(37, 99, 235, 0.18);
    border-radius: var(--radius-sm);
}

button:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

.image-url-overlay {
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

.image-url-dialog {
    width: 100%;
    max-width: 460px;
    background: var(--surface);
    border-radius: var(--radius-sm);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.16);
    overflow: hidden;
}

.image-url-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 20px 22px 0;
}

.image-url-header h3 {
    margin: 0;
    font-size: 17px;
    font-weight: 700;
    color: var(--text);
}

.image-url-close {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    padding: 0;
    border: none;
    background: transparent;
    color: var(--muted);
    font-size: 22px;
    cursor: pointer;
    border-radius: var(--radius-sm);
    transition: background-color 0.15s ease, color 0.15s ease;
}

.image-url-close:hover {
    background: rgba(0, 0, 0, 0.06);
    color: var(--text);
}

.image-url-body {
    padding: 18px 22px 0;
    display: grid;
    gap: 8px;
}

.image-url-label {
    font-size: 13px;
    font-weight: 600;
    color: var(--text);
}

.image-url-input {
    width: 100%;
    min-height: 44px;
    padding: 0 14px;
    border: 1px solid var(--line);
    border-radius: var(--radius-lg);
    font-size: 14px;
    outline: 0;
    transition: border-color 0.18s ease;
    box-sizing: border-box;
}

.image-url-input:focus {
    border-color: var(--brand);
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.image-url-input::placeholder {
    color: #b0b8c4;
}

.image-url-error {
    margin: 0;
    padding: 8px 12px;
    font-size: 13px;
    color: #d14343;
    background: rgba(209, 67, 67, 0.06);
    border-radius: var(--radius-md);
    line-height: 1.4;
}

.image-url-preview {
    margin-top: 4px;
    border-radius: var(--radius-lg);
    overflow: hidden;
    border: 1px solid var(--line);
    background: var(--surface-soft);
}

.image-url-preview img {
    display: block;
    width: 100%;
    max-height: 200px;
    object-fit: contain;
}

.image-url-actions {
    display: flex;
    gap: 10px;
    justify-content: flex-end;
    padding: 18px 22px 22px;
}

.image-url-cancel,
.image-url-confirm {
    min-height: 40px;
    padding: 0 22px;
    border-radius: var(--radius-sm);
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: background-color 0.16s ease, border-color 0.16s ease;
}

.image-url-cancel {
    background: transparent;
    border: 1px solid var(--line);
    color: var(--muted);
}

.image-url-cancel:hover {
    border-color: var(--text);
    color: var(--text);
}

.image-url-confirm {
    background: var(--brand);
    border: none;
    color: #ffffff;
    box-shadow: none;
}

.image-url-confirm:hover {
    filter: brightness(1.06);
    box-shadow: none;
}

.link-dialog-hint {
    margin: 0;
    font-size: 12px;
    color: var(--muted);
    line-height: 1.4;
}
</style>
