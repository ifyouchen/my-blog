import DOMPurify from 'dompurify';
import MarkdownIt from 'markdown-it';
import TurndownService from 'turndown';
import {
    getCodeLanguageClass,
    getCodeLanguageLabel,
    highlightCode,
    normalizeCodeLanguage
} from '@/utils/codeLanguages';

const createMarkdownRenderer = ({ copyableCode = false } = {}) => {
    const md = new MarkdownIt({
        html: true,
        linkify: true,
        breaks: false,
        typographer: true
    });

    const defaultLinkOpen = md.renderer.rules.link_open
        || ((tokens, idx, options, env, self) => self.renderToken(tokens, idx, options));

    md.renderer.rules.link_open = (tokens, idx, options, env, self) => {
        const hrefIndex = tokens[idx].attrIndex('href');
        if (hrefIndex >= 0) {
            const href = tokens[idx].attrs[hrefIndex][1];
            if (href && !/^[a-zA-Z][a-zA-Z0-9+.-]*:\/\//i.test(href) && !/^(\/\/|#|\/|mailto:|tel:)/.test(href)) {
                tokens[idx].attrs[hrefIndex][1] = 'https://' + href;
            }
        }
        const targetIndex = tokens[idx].attrIndex('target');
        const relIndex = tokens[idx].attrIndex('rel');
        if (targetIndex < 0) {
            tokens[idx].attrPush(['target', '_blank']);
        } else {
            tokens[idx].attrs[targetIndex][1] = '_blank';
        }
        if (relIndex < 0) {
            tokens[idx].attrPush(['rel', 'noopener noreferrer']);
        } else {
            tokens[idx].attrs[relIndex][1] = 'noopener noreferrer';
        }
        return defaultLinkOpen(tokens, idx, options, env, self);
    };

    md.renderer.rules.fence = (tokens, idx) => {
        const token = tokens[idx];
        const rawLanguage = token.info ? token.info.trim().split(/\s+/)[0] : '';
        const language = normalizeCodeLanguage(rawLanguage);
        const label = getCodeLanguageLabel(rawLanguage);
        const languageClass = getCodeLanguageClass(rawLanguage);
        const highlighted = highlightCode(token.content, language);

        if (!copyableCode) {
            return `<pre><code class="hljs language-${language}">${highlighted}</code></pre>`;
        }

        return [
            `<figure class="md-code-block ${languageClass}" data-language="${label}">`,
            '<figcaption>',
            `<span>${label}</span>`,
            '<button class="code-copy-button" type="button">复制</button>',
            '</figcaption>',
            `<pre><code class="hljs language-${language}">${highlighted}</code></pre>`,
            '</figure>'
        ].join('');
    };

    const defaultImage = md.renderer.rules.image
        || ((tokens, idx, options, env, self) => self.renderToken(tokens, idx, options));

    md.renderer.rules.image = (tokens, idx, options, env, self) => {
        const token = tokens[idx];
        const src = token.attrGet('src') || '';
        const altText = token.content || '';
        const sizeMatch = altText.match(/^(.+)\|(\d+)x(\d*)$/);
        let actualAlt = altText;
        let width = '';
        let height = '';

        if (sizeMatch) {
            actualAlt = sizeMatch[1];
            width = sizeMatch[2];
            height = sizeMatch[3] || '';
        }

        let style = '';
        if (width) {
            style = `width:${width}px;max-width:100%;`;
        }
        if (height) {
            style += `height:${height}px;`;
        }

        const imageToken = tokens[idx];
        const originalStyle = imageToken.attrGet('style') || '';
        const finalStyle = style + originalStyle;

        if (finalStyle) {
            return `<img src="${src}" alt="${actualAlt}" style="${finalStyle}">`;
        }
        return `<img src="${src}" alt="${actualAlt}">`;
    };

    return md;
};

const previewMarkdown = createMarkdownRenderer({ copyableCode: true });
const editorMarkdown = createMarkdownRenderer({ copyableCode: false });

const SAFE_FONT_SIZES = new Set(['12px', '14px', '16px', '18px', '20px', '24px', '28px', '32px']);
const SAFE_LINE_HEIGHTS = new Set(['1.4', '1.6', '1.8', '2', '2.0', '2.4']);
const SAFE_FONT_FAMILIES = [
    { value: "'Microsoft YaHei', 'PingFang SC', sans-serif", keys: ['microsoft yahei', 'pingfang sc'] },
    { value: "'SimSun', 'Songti SC', serif", keys: ['simsun', 'songti sc', '宋体'] },
    { value: "'SimHei', 'Heiti SC', sans-serif", keys: ['simhei', 'heiti sc', '黑体'] },
    { value: 'Arial, Helvetica, sans-serif', keys: ['arial', 'helvetica'] },
    { value: 'Georgia, serif', keys: ['georgia'] },
    { value: "'Courier New', Consolas, monospace", keys: ['courier new', 'consolas'] }
];

const normalizeStyleString = (value = '') => String(value)
    .trim()
    .replace(/\s+/g, ' ')
    .replace(/;$/g, '');

const hasUnsafeCssToken = (value = '') => {
    return /url\s*\(|expression\s*\(|javascript:|data:/i.test(String(value));
};

const normalizeTextColor = (value = '') => {
    const color = normalizeStyleString(value);
    if (!color || hasUnsafeCssToken(color)) return '';
    if (/^#[0-9a-f]{3}([0-9a-f]{3})?$/i.test(color)) return color.toLowerCase();
    if (/^rgba?\(\s*\d{1,3}\s*,\s*\d{1,3}\s*,\s*\d{1,3}(?:\s*,\s*(?:0|1|0?\.\d+))?\s*\)$/i.test(color)) {
        return color;
    }
    if (/^hsla?\(\s*\d{1,3}\s*,\s*\d{1,3}%\s*,\s*\d{1,3}%(?:\s*,\s*(?:0|1|0?\.\d+))?\s*\)$/i.test(color)) {
        return color;
    }
    return '';
};

const normalizeFontSize = (value = '') => {
    const fontSize = normalizeStyleString(value).toLowerCase();
    return SAFE_FONT_SIZES.has(fontSize) ? fontSize : '';
};

const normalizeLineHeight = (value = '') => {
    const lineHeight = normalizeStyleString(value);
    return SAFE_LINE_HEIGHTS.has(lineHeight) ? lineHeight : '';
};

const normalizeFontFamily = (value = '') => {
    const fontFamily = normalizeStyleString(value)
        .replace(/["']/g, '')
        .toLowerCase();
    const matched = SAFE_FONT_FAMILIES.find((item) => item.keys.some((key) => fontFamily.includes(key)));
    return matched?.value || '';
};

const normalizeSizeValue = (value = '') => {
    const size = normalizeStyleString(value).toLowerCase();
    if (/^\d{1,4}(\.\d{1,2})?px$/.test(size)) return size;
    if (/^\d{1,3}(\.\d{1,2})?%$/.test(size)) return size;
    if (size === 'auto') return size;
    return '';
};

const normalizeTextAlign = (value = '') => {
    const align = normalizeStyleString(value).toLowerCase();
    return ['left', 'center', 'right'].includes(align) ? align : '';
};

const normalizeStyleValue = (property = '', value = '') => {
    if (hasUnsafeCssToken(value)) return '';
    switch (property.trim().toLowerCase()) {
        case 'color':
            return normalizeTextColor(value);
        case 'font-size':
            return normalizeFontSize(value);
        case 'font-family':
            return normalizeFontFamily(value);
        case 'line-height':
            return normalizeLineHeight(value);
        case 'width':
        case 'max-width':
        case 'height':
            return normalizeSizeValue(value);
        case 'text-align':
            return normalizeTextAlign(value);
        default:
            return '';
    }
};

const buildStyleAttribute = (styles = []) => {
    return styles
        .map(([property, value]) => {
            const normalized = normalizeStyleValue(property, value);
            return normalized ? `${property}:${normalized}` : '';
        })
        .filter(Boolean)
        .join(';');
};

const sanitizeInlineStyle = (style = '') => {
    return String(style)
        .split(';')
        .map((declaration) => declaration.trim())
        .filter(Boolean)
        .map((declaration) => {
            const separatorIndex = declaration.indexOf(':');
            if (separatorIndex <= 0) return '';
            const property = declaration.slice(0, separatorIndex).trim().toLowerCase();
            const value = declaration.slice(separatorIndex + 1).trim();
            const normalized = normalizeStyleValue(property, value);
            return normalized ? `${property}:${normalized}` : '';
        })
        .filter(Boolean)
        .join(';');
};

const sanitizeHtmlStyles = (html = '') => {
    if (typeof document === 'undefined') {
        return html;
    }

    const template = document.createElement('template');
    template.innerHTML = html;
    template.content.querySelectorAll('[style]').forEach((element) => {
        const cleanStyle = sanitizeInlineStyle(element.getAttribute('style') || '');
        if (cleanStyle) {
            element.setAttribute('style', cleanStyle);
        } else {
            element.removeAttribute('style');
        }
    });
    return template.innerHTML;
};

const sanitize = (html) => sanitizeHtmlStyles(DOMPurify.sanitize(html, {
    ADD_ATTR: ['target', 'rel', 'data-language', 'style'],
    ADD_TAGS: ['u'],
    WHOLE_DOCUMENT: false
}));

export const renderMarkdown = (markdown = '') => {
    return sanitize(previewMarkdown.render(markdown || ''));
};

export const markdownToEditorHtml = (markdown = '') => {
    return sanitize(editorMarkdown.render(markdown || ''));
};

export const renderMarkdownForExport = (markdown = '') => {
    return sanitize(editorMarkdown.render(markdown || ''));
};

const escapeTableCell = (text = '') => {
    return String(text)
        .replace(/\|/g, '\\|')
        .replace(/\n+/g, ' ')
        .trim();
};

const tableToMarkdown = (node) => {
    const rows = Array.from(node.querySelectorAll('tr'))
        .map((row) => Array.from(row.querySelectorAll('th, td'))
            .map((cell) => escapeTableCell(cell.textContent)))
        .filter((cells) => cells.length > 0);

    if (!rows.length) {
        return '';
    }

    const columnCount = Math.max(...rows.map((cells) => cells.length));
    const normalizedRows = rows.map((cells) => {
        const padded = [...cells];
        while (padded.length < columnCount) {
            padded.push('');
        }
        return padded;
    });
    const header = normalizedRows[0];
    const separator = header.map(() => '---');
    const body = normalizedRows.slice(1);
    const formatRow = (cells) => `| ${cells.join(' | ')} |`;

    return [
        '',
        formatRow(header),
        formatRow(separator),
        ...body.map(formatRow),
        ''
    ].join('\n');
};

const turndownService = new TurndownService({
    codeBlockStyle: 'fenced',
    fence: '```',
    headingStyle: 'atx',
    bulletListMarker: '-'
});

turndownService.addRule('table', {
    filter: 'table',
    replacement(content, node) {
        return tableToMarkdown(node);
    }
});

turndownService.addRule('strikethrough', {
    filter: ['s', 'del'],
    replacement(content) {
        return `~~${content}~~`;
    }
});

turndownService.addRule('image', {
    filter: 'img',
    replacement(content, node) {
        const src = node.getAttribute('src') || '';
        const alt = node.getAttribute('alt') || '';
        const width = node.getAttribute('width') || '';
        const height = node.getAttribute('height') || '';

        if (width || height) {
            if (height) {
                return `![${alt}|${width}x${height}](${src})`;
            }
            return `![${alt}|${width}x](${src})`;
        }
        return `![${alt}](${src})`;
    }
});

export const editorHtmlToMarkdown = (html = '') => {
    return turndownService.turndown(html || '').trim();
};

export const editorJsonToMarkdown = (json) => {
    if (!json || !json.content) return '';

    const normalizeBlock = (content = '') => {
        const trimmed = String(content).replace(/\n+$/g, '');
        return trimmed ? `${trimmed}\n\n` : '';
    };

    const getBlockStyle = (attrs = {}) => buildStyleAttribute([
        ['text-align', attrs.textAlign && attrs.textAlign !== 'left' ? attrs.textAlign : ''],
        ['line-height', attrs.lineHeight]
    ]);

    const getTextStyle = (attrs = {}) => buildStyleAttribute([
        ['color', attrs.color],
        ['font-size', attrs.fontSize],
        ['font-family', attrs.fontFamily]
    ]);

    const indentLines = (content = '', depth = 0) => {
        const indent = '  '.repeat(depth);
        return String(content)
            .split('\n')
            .map((line) => (line ? `${indent}${line}` : line))
            .join('\n');
    };

    const processInlineContent = (node) => {
        return (node.content || []).map((child) => processNode(child, { inline: true })).join('');
    };

    const processListItem = (node, marker, depth = 0) => {
        const children = node.content || [];
        const inlineParts = [];
        const nestedBlocks = [];

        children.forEach((child) => {
            if (child.type === 'paragraph') {
                inlineParts.push(processNode(child, { inline: true }));
                return;
            }
            if (child.type === 'bulletList' || child.type === 'orderedList' || child.type === 'taskList') {
                nestedBlocks.push(processNode(child, { depth: depth + 1, nested: true }));
                return;
            }
            inlineParts.push(processNode(child, { inline: true }).trim());
        });

        const prefix = `${'  '.repeat(depth)}${marker} `;
        const firstLine = `${prefix}${inlineParts.join('').trim()}`.trimEnd();
        return `${firstLine}\n${nestedBlocks.join('')}`;
    };

    const renderImage = (node, context = {}) => {
        const src = node.attrs.src || '';
        const alt = node.attrs.alt || '';
        let width = '';
        if (node.attrs.containerStyle) {
            const match = node.attrs.containerStyle.match(/width:\s*([0-9.]+)px/);
            if (match) width = match[1];
        }
        const align = context.textAlign;
        if (align && align !== 'left' && !context.inline) {
            let style = '';
            if (width) style = `width:${width}px;max-width:100%;`;
            const imgTag = `<img src="${src}" alt="${alt}"${style ? ` style="${style}"` : ''}>`;
            return `<div style="text-align:${align}">${imgTag}</div>`;
        }
        if (width) {
            return `![${alt}|${width}x](${src})`;
        }
        return `![${alt}](${src})`;
    };

    const processNode = (node, context = {}) => {
        if (node.type === 'imageResize') {
            return renderImage(node, context);
        }
        if (node.type === 'image') {
            return renderImage(node, context);
        }
        if (node.type === 'paragraph') {
            const align = node.attrs?.textAlign;
            const lineHeight = node.attrs?.lineHeight;
            const childContext = { ...context, textAlign: align };
            const text = (node.content || []).map(child => processNode(child, childContext)).join('');
            if (context.inline) {
                return text;
            }
            const style = getBlockStyle({ textAlign: align, lineHeight });
            if (style) {
                const allImages = (node.content || []).every(c => c.type === 'imageResize' || c.type === 'image');
                if (allImages && align && !lineHeight) {
                    return normalizeBlock(text);
                }
                return normalizeBlock(`<p style="${style}">${text}</p>`);
            }
            return normalizeBlock(text);
        }
        if (node.type === 'heading') {
            const level = node.attrs.level || 1;
            const text = processInlineContent(node);
            const style = getBlockStyle(node.attrs || {});
            if (style) {
                return normalizeBlock(`<h${level} style="${style}">${text}</h${level}>`);
            }
            return normalizeBlock('#'.repeat(level) + ' ' + text);
        }
        if (node.type === 'blockquote') {
            const text = (node.content || [])
                .map((child) => processNode(child).trim())
                .filter(Boolean)
                .join('\n');
            return normalizeBlock(text.split('\n').map((line) => `> ${line}`).join('\n'));
        }
        if (node.type === 'codeBlock') {
            const language = node.attrs.language || '';
            const text = processInlineContent(node);
            return normalizeBlock('```' + language + '\n' + text + '\n```');
        }
        if (node.type === 'horizontalRule') {
            return normalizeBlock('---');
        }
        if (node.type === 'table') {
            const rows = (node.content || []).map(processNode).filter(Boolean);
            if (!rows.length) return '';
            // Find max column count from the first row
            const colCount = rows[0].columns;
            const headerRow = rows.find(r => r.isHeader);
            // Build full markdown table
            let result = '\n';
            for (const row of rows) {
                result += row.markdown + '\n';
                if (row.isHeader) {
                    result += '|' + '---|'.repeat(colCount) + '\n';
                }
            }
            return normalizeBlock(result);
        }
        if (node.type === 'tableRow') {
            const cells = (node.content || []).map(processNode);
            if (!cells.length) return null;
            const isHeader = node.content?.some(c => c.type === 'tableHeader');
            const columns = cells.reduce((sum, c) => sum + (c.colspan || 1), 0);
            const md = '| ' + cells.map(c => c.text).join(' | ') + ' |';
            return { markdown: md, columns, isHeader };
        }
        if (node.type === 'tableHeader' || node.type === 'tableCell') {
            const text = (node.content || []).map((child) => processNode(child, { inline: true })).join('').trim();
            const colspan = node.attrs?.colspan || 1;
            return { text, colspan };
        }
        if (node.type === 'bulletList') {
            const depth = context.depth || 0;
            const content = (node.content || [])
                .map((child) => processListItem(child, '-', depth))
                .join('');
            return context.nested ? content : `${content}\n`;
        }
        if (node.type === 'taskList') {
            const depth = context.depth || 0;
            const content = (node.content || [])
                .map((child) => processListItem(child, child.attrs?.checked ? '- [x]' : '- [ ]', depth))
                .join('');
            return context.nested ? content : `${content}\n`;
        }
        if (node.type === 'taskItem') {
            return processListItem(node, node.attrs?.checked ? '- [x]' : '- [ ]', context.depth || 0);
        }
        if (node.type === 'orderedList') {
            let index = node.attrs?.start || 1;
            const depth = context.depth || 0;
            const content = (node.content || [])
                .map((child) => processListItem(child, `${index++}.`, depth))
                .join('');
            return context.nested ? content : `${content}\n`;
        }
        if (node.type === 'listItem') {
            return processInlineContent(node);
        }
        if (node.type === 'hardBreak') {
            return '\n';
        }
        if (node.type === 'text') {
            let text = node.text || '';
            if (node.marks) {
                // CommonMark：当加粗/斜体文本首尾有标点时，**+34** 不会被识别为定界符
                // 改用 HTML 标签 <strong>/<em> 避免此问题
                const boundaryPunct = /^[\p{P}\p{S}]|[\p{P}\p{S}]$/u.test(text);
                let textStyle = '';
                for (const mark of node.marks) {
                    if (mark.type === 'bold') {
                        text = boundaryPunct ? `<strong>${text}</strong>` : `**${text}**`;
                    } else if (mark.type === 'italic') {
                        text = boundaryPunct ? `<em>${text}</em>` : `*${text}*`;
                    } else if (mark.type === 'underline') {
                        text = `<u>${text}</u>`;
                    } else if (mark.type === 'code') {
                        text = `\`${text}\``;
                    } else if (mark.type === 'strike') {
                        text = `~~${text}~~`;
                    } else if (mark.type === 'link') {
                        text = `[${text}](${mark.attrs.href})`;
                    } else if (mark.type === 'textStyle') {
                        textStyle = getTextStyle(mark.attrs || {});
                    }
                }
                if (textStyle && !node.marks.some((mark) => mark.type === 'code')) {
                    text = `<span style="${textStyle}">${text}</span>`;
                }
            }
            return text;
        }
        if (node.content) {
            const content = node.content.map((child) => processNode(child, context)).join('');
            return context.inline ? content : indentLines(content, context.depth || 0);
        }
        return '';
    };

    return json.content.map(processNode).join('').trim();
};
