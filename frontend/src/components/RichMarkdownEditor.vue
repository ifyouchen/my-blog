<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { EditorContent, useEditor, VueNodeViewRenderer } from '@tiptap/vue-3';
import StarterKit from '@tiptap/starter-kit';
import Link from '@tiptap/extension-link';
import Table from '@tiptap/extension-table';
import TableCell from '@tiptap/extension-table-cell';
import TableHeader from '@tiptap/extension-table-header';
import TableRow from '@tiptap/extension-table-row';
import CodeBlockLowlight from '@tiptap/extension-code-block-lowlight';
import ResizeImage from 'tiptap-extension-resize-image';
import { createBlogLowlight } from '@/utils/codeLanguages';
import { editorHtmlToMarkdown, editorJsonToMarkdown, markdownToEditorHtml } from '@/utils/markdown';
import CodeBlockNodeView from '@/components/CodeBlockNodeView.vue';
import { uploadImageApi } from '@/api/uploads.js';
import { useSession } from '@/stores/session';

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
const imageInputRef = ref(null);
const imageUploading = ref(false);
const uploadError = ref('');

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
            insertImageByUrl();
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
            resizable: true
        }),
        TableRow,
        TableHeader,
        TableCell,
        codeBlockExtension,
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
        const json = currentEditor.getJSON();
        console.log('Editor JSON:', JSON.stringify(json, null, 2));
        const markdown = editorJsonToMarkdown(json);
        console.log('Converted Markdown:', markdown);
        emit('update:modelValue', markdown);
    },
    onSelectionUpdate({ editor: currentEditor }) {
        refreshEditorState();
        syncSlashMenu(currentEditor);
    }
});

onBeforeUnmount(() => {
    closeSlashMenu();
    closeContextMenu();
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
    const previousUrl = editor.value.getAttributes('link').href || '';
    const url = window.prompt('请输入链接地址', previousUrl);
    if (url === null) {
        return;
    }
    if (!url) {
        editor.value.chain().focus().unsetLink().run();
        return;
    }
    editor.value.chain().focus().extendMarkRange('link').setLink({ href: url }).run();
};

const insertTable = () => {
    editor.value?.chain().focus().insertTable({
        rows: 3,
        cols: 3,
        withHeaderRow: true
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

const insertImageByUrl = () => {
    if (!editor.value) {
        return;
    }
    const url = window.prompt('请输入图片链接地址');
    if (!url) {
        return;
    }
    if (!url.startsWith('http://') && !url.startsWith('https://')) {
        alert('请输入有效的图片链接（以 http:// 或 https:// 开头）');
        return;
    }
    insertImageMarkdown(url, '外部图片');
};

const uploadAndInsertImage = async (file) => {
    if (!isLoggedIn.value) {
        uploadError.value = '请先登录后再上传图片';
        return;
    }
    imageUploading.value = true;
    uploadError.value = '';
    try {
        const result = await uploadImageApi(file, 'content');
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
    if (!file) {
        return;
    }
    if (!isLoggedIn.value) {
        uploadError.value = '请先登录后再上传图片';
        return;
    }
    event.target.value = '';
    imageUploading.value = true;
    uploadError.value = '';
    try {
        const result = await uploadImageApi(file, 'content');
        if (result?.url) {
            insertImageMarkdown(result.url, file.name || '图片');
        }
    } catch (error) {
        uploadError.value = error.message || '图片上传失败';
    } finally {
        imageUploading.value = false;
    }
};

const triggerImageUpload = () => {
    imageInputRef.value?.click();
};

const toggleBold = () => runEditorCommand((chain) => chain.toggleBold());
const toggleHeading = (level) => runEditorCommand((chain) => chain.toggleHeading({ level }));
const toggleBulletList = () => runEditorCommand((chain) => chain.toggleBulletList());
const toggleBlockquote = () => runEditorCommand((chain) => chain.toggleBlockquote());
const addRowBefore = () => runEditorCommand((chain) => chain.addRowBefore());
const addRowAfter = () => runEditorCommand((chain) => chain.addRowAfter());
const deleteRow = () => runEditorCommand((chain) => chain.deleteRow());
const addColumnBefore = () => runEditorCommand((chain) => chain.addColumnBefore());
const addColumnAfter = () => runEditorCommand((chain) => chain.addColumnAfter());
const deleteColumn = () => runEditorCommand((chain) => chain.deleteColumn());
const toggleHeaderRow = () => runEditorCommand((chain) => chain.toggleHeaderRow());
const deleteTable = () => runEditorCommand((chain) => chain.deleteTable());

const isInTable = computed(() => {
    return editorStateVersion.value >= 0 && Boolean(editor.value?.isActive('table'));
});

const toolbarGroups = computed(() => {
    editorStateVersion.value;
    const currentEditor = editor.value;
    return [
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
                    label: '列表',
                    active: Boolean(currentEditor?.isActive('bulletList')),
                    run: toggleBulletList
                },
                {
                    id: 'blockquote',
                    label: '引用',
                    active: Boolean(currentEditor?.isActive('blockquote')),
                    run: toggleBlockquote
                },
                {
                    id: 'link',
                    label: '链接',
                    active: Boolean(currentEditor?.isActive('link')),
                    run: setLink
                },
                {
                    id: 'divider',
                    label: '分割线',
                    active: false,
                    run: insertDivider
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
                    run: insertTable
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
                    run: insertImageByUrl
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

const contextMenuItems = computed(() => {
    return toolbarGroups.value;
});

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

const adjustContextMenuPosition = () => {
    if (!contextMenuState.open || !editorRoot.value || !contextMenuRef.value) {
        return;
    }
    const container = editorRoot.value.getBoundingClientRect();
    const menu = contextMenuRef.value.getBoundingClientRect();

    const viewportHeight = window.innerHeight;
    const viewportWidth = window.innerWidth;

    // 菜单相对于视口的实际位置
    const menuRight = contextMenuState.left + container.left + menu.width;
    const menuBottom = contextMenuState.top + container.top + menu.height;

    let left = contextMenuState.left;
    let top = contextMenuState.top;

    // 水平方向：确保不超出右边界
    if (menuRight > viewportWidth - 12) {
        left = Math.max(12, viewportWidth - menu.width - container.left - 12);
    }

    // 垂直方向：确保不超出底边界，如果下方空间不够则向上展开
    if (menuBottom > viewportHeight - 12) {
        // 尝试向上展开
        const spaceAbove = contextMenuState.top;
        const spaceBelow = viewportHeight - menuBottom;

        if (spaceAbove > spaceBelow && spaceAbove >= menu.height) {
            // 上方空间更多且足够
            top = Math.max(12, contextMenuState.top - menu.height);
        } else if (spaceAbove < menu.height) {
            // 上方空间也不够，紧贴顶部
            top = 12;
        }
        // 否则保持原位置，让菜单部分可见
    }

    contextMenuState.left = left;
    contextMenuState.top = top;
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

    const rootRect = editorRoot.value.getBoundingClientRect();
    contextMenuState.left = Math.max(12, event.clientX - rootRect.left);
    contextMenuState.top = Math.max(12, event.clientY - rootRect.top);
    contextMenuState.open = true;

    nextTick(() => {
        adjustContextMenuPosition();
    });
};

const handleGlobalPointerDown = (event) => {
    if (!contextMenuState.open) {
        return;
    }
    const target = event.target;
    if (contextMenuRef.value?.contains(target)) {
        return;
    }
    closeContextMenu();
};

const handleGlobalKeyDown = (event) => {
    if (event.key === 'Escape') {
        closeContextMenu();
    }
};

const handleGlobalScroll = () => {
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
                    v-for="group in toolbarGroups"
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
                        @mousedown.prevent="keepEditorFocus"
                        @click="runToolbarItem(item)"
                    >
                        {{ item.label }}
                    </button>
                </div>
            </div>
            <div v-if="editor && isInTable" class="table-context-toolbar" contenteditable="false">
                <span>表格</span>
                <button type="button" @mousedown.prevent="keepEditorFocus" @click="addRowBefore">上方插入行</button>
                <button type="button" @mousedown.prevent="keepEditorFocus" @click="addRowAfter">下方插入行</button>
                <button type="button" @mousedown.prevent="keepEditorFocus" @click="deleteRow">删除行</button>
                <button type="button" @mousedown.prevent="keepEditorFocus" @click="addColumnBefore">左侧插入列</button>
                <button type="button" @mousedown.prevent="keepEditorFocus" @click="addColumnAfter">右侧插入列</button>
                <button type="button" @mousedown.prevent="keepEditorFocus" @click="deleteColumn">删除列</button>
                <button type="button" @mousedown.prevent="keepEditorFocus" @click="toggleHeaderRow">切换表头</button>
                <button type="button" class="danger" @mousedown.prevent="keepEditorFocus" @click="deleteTable">删除表格</button>
            </div>
        </div>
        <div ref="editorRoot" class="editor-body" @contextmenu="handleEditorContextMenu">
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
                            {{ item.label }}
                        </button>
                    </div>
                </template>
            </div>
            <EditorContent :editor="editor" />
        </div>
        <p v-if="uploadError" class="upload-error">{{ uploadError }}</p>
    </section>
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
    border-radius: 4px;
}

button:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}
</style>
