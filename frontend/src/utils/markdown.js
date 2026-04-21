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

export const editorHtmlToMarkdown = (html = '') => {
    return turndownService.turndown(html || '').trim();
};
