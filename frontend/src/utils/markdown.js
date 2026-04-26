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
        html: false,
        linkify: true,
        breaks: false,
        typographer: true
    });

    const defaultLinkOpen = md.renderer.rules.link_open
        || ((tokens, idx, options, env, self) => self.renderToken(tokens, idx, options));

    md.renderer.rules.link_open = (tokens, idx, options, env, self) => {
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

const sanitize = (html) => DOMPurify.sanitize(html, {
    ADD_ATTR: ['target', 'rel', 'data-language']
});

export const renderMarkdown = (markdown = '') => {
    return sanitize(previewMarkdown.render(markdown || ''));
};

export const markdownToEditorHtml = (markdown = '') => {
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

    const processNode = (node) => {
        if (node.type === 'imageResize') {
            const src = node.attrs.src || '';
            const alt = node.attrs.alt || '';
            let width = '';
            if (node.attrs.containerStyle) {
                const match = node.attrs.containerStyle.match(/width:\s*([0-9.]+)px/);
                if (match) width = match[1];
            }
            if (width) {
                return `![${alt}|${width}x](${src})`;
            }
            return `![${alt}](${src})`;
        }
        if (node.type === 'image') {
            const src = node.attrs.src || '';
            const alt = node.attrs.alt || '';
            let width = '';
            if (node.attrs.containerStyle) {
                const match = node.attrs.containerStyle.match(/width:\s*([0-9.]+)px/);
                if (match) width = match[1];
            }
            if (width) {
                return `![${alt}|${width}x](${src})`;
            }
            return `![${alt}](${src})`;
        }
        if (node.type === 'paragraph') {
            return (node.content || []).map(processNode).join('') + '\n';
        }
        if (node.type === 'heading') {
            const level = node.attrs.level || 1;
            const text = (node.content || []).map(processNode).join('');
            return '#'.repeat(level) + ' ' + text + '\n';
        }
        if (node.type === 'blockquote') {
            const text = (node.content || []).map(processNode).join('');
            return '> ' + text + '\n';
        }
        if (node.type === 'codeBlock') {
            const language = node.attrs.language || '';
            const text = (node.content || []).map(processNode).join('');
            return '```' + language + '\n' + text + '\n```\n';
        }
        if (node.type === 'horizontalRule') {
            return '---\n';
        }
        if (node.type === 'bulletList') {
            return (node.content || []).map(processNode).join('');
        }
        if (node.type === 'orderedList') {
            return (node.content || []).map(processNode).join('');
        }
        if (node.type === 'listItem') {
            const text = (node.content || []).map(processNode).join('');
            return '- ' + text + '\n';
        }
        if (node.type === 'hardBreak') {
            return '\n';
        }
        if (node.type === 'text') {
            let text = node.text || '';
            if (node.marks) {
                for (const mark of node.marks) {
                    if (mark.type === 'bold') text = `**${text}**`;
                    if (mark.type === 'italic') text = `*${text}*`;
                    if (mark.type === 'code') text = `\`${text}\``;
                    if (mark.type === 'strike') text = `~~${text}~~`;
                    if (mark.type === 'link') text = `[${text}](${mark.attrs.href})`;
                }
            }
            return text;
        }
        if (node.content) {
            return node.content.map(processNode).join('');
        }
        return '';
    };

    return json.content.map(processNode).join('').trim();
};
