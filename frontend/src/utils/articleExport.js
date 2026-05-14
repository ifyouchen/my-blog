import JSZip from 'jszip';
import {renderMarkdownForExport} from '@/utils/markdown';

const EXPORT_CONTENT_STYLES = `
    :root {
        color: #2c3e50;
        background: #ffffff;
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", Arial, sans-serif;
    }

    * {
        box-sizing: border-box;
    }

    body {
        margin: 0;
        color: #2c3e50;
        background: #ffffff;
        font-size: 15px;
        line-height: 1.9;
        print-color-adjust: exact;
        -webkit-print-color-adjust: exact;
    }

    .article-export-body {
        max-width: 760px;
        margin: 0 auto;
        padding: 32px;
        color: #2c3e50;
        background: #ffffff;
        font-size: 15px;
        line-height: 1.9;
    }

    .markdown-preview h1,
    .markdown-preview h2,
    .markdown-preview h3,
    .markdown-preview h4,
    .markdown-preview h5,
    .markdown-preview h6 {
        line-height: 1.4;
        color: #1a2535;
        page-break-after: avoid;
        break-after: avoid;
    }

    .markdown-preview h2 {
        padding-bottom: 6px;
        border-bottom: 1px solid #e8eaed;
    }

    .markdown-preview p,
    .markdown-preview ul,
    .markdown-preview ol,
    .markdown-preview blockquote,
    .markdown-preview table,
    .markdown-preview pre {
        page-break-inside: avoid;
        break-inside: avoid;
    }

    .markdown-preview a {
        color: #2563eb;
        text-decoration: underline;
        text-underline-offset: 2px;
    }

    .markdown-preview img {
        display: inline-block;
        max-width: 100%;
        height: auto;
        vertical-align: middle;
    }

    .markdown-preview p:has(> img:only-child) {
        text-align: center;
    }

    .markdown-preview blockquote {
        padding: 4px 0 4px 14px;
        margin: 16px 0;
        color: #7f8fa4;
        background: #eff6ff;
        border-left: 3px solid #2563eb;
    }

    .markdown-preview code:not(pre code) {
        padding: 1px 5px;
        color: #2563eb;
        font-family: "JetBrains Mono", Consolas, monospace;
        font-size: 0.88em;
        background: #eff6ff;
        border-radius: 4px;
    }

    .markdown-preview table {
        width: 100%;
        margin: 16px 0;
        overflow: hidden;
        border: 1px solid #e8eaed;
        border-spacing: 0;
        border-radius: 6px;
    }

    .markdown-preview th,
    .markdown-preview td {
        min-width: 80px;
        padding: 8px 10px;
        font-size: 14px;
        text-align: left;
        border-right: 1px solid #e8eaed;
        border-bottom: 1px solid #e8eaed;
    }

    .markdown-preview th {
        color: #1a2535;
        font-weight: 700;
        background: #f5f6f8;
    }

    .markdown-preview tr:last-child td {
        border-bottom: 0;
    }

    .markdown-preview th:last-child,
    .markdown-preview td:last-child {
        border-right: 0;
    }

    .markdown-preview pre {
        max-width: 100%;
        padding: 14px 16px;
        margin: 16px 0;
        overflow-x: auto;
        color: #d4d4d4;
        white-space: pre;
        background: #1e1e1e;
        border: 1px solid #333333;
        border-radius: 6px;
    }

    .markdown-preview pre code {
        display: block;
        min-width: max-content;
        color: inherit;
        font-family: "JetBrains Mono", Consolas, monospace;
        font-size: 13px;
        white-space: pre;
        background: transparent;
    }

    .hljs-keyword,
    .hljs-selector-tag,
    .hljs-literal {
        color: #569cd6;
    }

    .hljs-string,
    .hljs-title,
    .hljs-name {
        color: #9cdca6;
    }

    .hljs-number,
    .hljs-built_in,
    .hljs-type {
        color: #ce9178;
    }

    .hljs-comment {
        color: #6a9955;
    }

    @page {
        size: A4;
        margin: 14mm 16mm;

        @top-left {
            content: "";
        }

        @top-center {
            content: "";
        }

        @top-right {
            content: "";
        }

        @bottom-left {
            content: "";
        }

        @bottom-center {
            color: #8a94a6;
            content: counter(page) "/" counter(pages);
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", Arial, sans-serif;
            font-size: 9pt;
        }

        @bottom-right {
            content: "";
        }
    }

    @media print {
        html,
        body {
            width: auto;
            min-width: 0;
            margin: 0;
            background: #ffffff;
        }

        .article-export-body {
            max-width: none;
            width: auto;
            margin: 0;
            padding: 0;
            font-size: 11pt;
            line-height: 1.75;
        }

        .markdown-preview h1,
        .markdown-preview h2,
        .markdown-preview h3,
        .markdown-preview h4,
        .markdown-preview h5,
        .markdown-preview h6,
        .markdown-preview img,
        .markdown-preview table,
        .markdown-preview pre,
        .markdown-preview blockquote {
            break-inside: avoid;
            page-break-inside: avoid;
        }

        .markdown-preview pre {
            overflow: visible;
            white-space: pre-wrap;
            word-break: break-word;
        }

        .markdown-preview pre code {
            min-width: 0;
            white-space: inherit;
        }
    }
`;

const MIME_TYPES = {
    html: 'text/html;charset=utf-8',
    md: 'text/markdown;charset=utf-8',
    zip: 'application/zip'
};
const MARKDOWN_IMAGE_REGEX = /!\[([^\]\n]*(?:\\.[^\]\n]*)*)\]\(([^)\n]*)\)/g;
const HTML_IMAGE_SRC_REGEX = /(<img\b[^>]*?\bsrc\s*=\s*)(["'])([^"']+)(\2)/gi;
const HTML_IMAGE_SRC_UNQUOTED_REGEX = /(<img\b[^>]*?\bsrc\s*=\s*)(?!["'])([^\s>]+)/gi;
const IMAGE_EXTENSION_BY_MIME = {
    'image/apng': 'apng',
    'image/avif': 'avif',
    'image/bmp': 'bmp',
    'image/gif': 'gif',
    'image/jpeg': 'jpg',
    'image/jpg': 'jpg',
    'image/png': 'png',
    'image/svg+xml': 'svg',
    'image/webp': 'webp'
};
const KNOWN_IMAGE_EXTENSIONS = new Set([
    'apng',
    'avif',
    'bmp',
    'gif',
    'jpeg',
    'jpg',
    'png',
    'svg',
    'webp'
]);
const PDF_PRINT_REQUESTED_EVENT = 'article-export:pdf-print-requested';
const PDF_PRINT_FRAME_SELECTOR = 'iframe[data-article-export-print-frame="true"]';
const PDF_PRINT_AFTERPRINT_CLEANUP_DELAY_MS = 1000;
const PDF_PRINT_FALLBACK_CLEANUP_DELAY_MS = 60000;

const stripTrailingDots = (value) => value.replace(/[.\s]+$/g, '');

const sanitizeFilename = (title = '') => {
    const cleaned = stripTrailingDots(String(title || '')
        .trim()
        .replace(/[\\/:*?"<>|]+/g, '-')
        .replace(/\s+/g, ' ')
        .slice(0, 80));
    return cleaned || 'article-content';
};

const escapeHtml = (value = '') => String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');

const saveBlob = (blob, filename) => {
    const objectUrl = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = objectUrl;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.setTimeout(() => URL.revokeObjectURL(objectUrl), 1000);
};

const padImageIndex = (index) => String(index).padStart(3, '0');

const isDataImage = (src = '') => src.trim().toLowerCase().startsWith('data:');

const resolveImageUrl = (src = '') => {
    try {
        return new URL(src, window.location.href).href;
    } catch {
        return src;
    }
};

const blobToDataUrl = (blob) => new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = () => reject(reader.error || new Error('图片读取失败'));
    reader.readAsDataURL(blob);
});

const parseMarkdownImageTarget = (target = '') => {
    const leading = target.match(/^\s*/)?.[0] || '';
    const content = target.slice(leading.length);

    if (content.startsWith('<')) {
        const endIndex = content.indexOf('>');
        if (endIndex > 0) {
            return {
                leading,
                src: content.slice(1, endIndex),
                trailing: content.slice(endIndex + 1)
            };
        }
    }

    const match = content.match(/^(\S+)([\s\S]*)$/);
    if (!match) {
        return {
            leading,
            src: '',
            trailing: ''
        };
    }

    return {
        leading,
        src: match[1],
        trailing: match[2] || ''
    };
};

const addImageSource = (sources, seen, src = '') => {
    const normalized = String(src || '').trim();
    if (!normalized || seen.has(normalized)) {
        return;
    }
    seen.add(normalized);
    sources.push(normalized);
};

const collectMarkdownImageSources = (markdown = '') => {
    const sources = [];
    const seen = new Set();

    for (const match of String(markdown).matchAll(MARKDOWN_IMAGE_REGEX)) {
        const parsed = parseMarkdownImageTarget(match[2] || '');
        addImageSource(sources, seen, parsed.src);
    }

    for (const match of String(markdown).matchAll(HTML_IMAGE_SRC_REGEX)) {
        addImageSource(sources, seen, match[3]);
    }

    for (const match of String(markdown).matchAll(HTML_IMAGE_SRC_UNQUOTED_REGEX)) {
        addImageSource(sources, seen, match[2]);
    }

    return sources;
};

const getImageExtensionFromMime = (mimeType = '') => {
    const normalized = String(mimeType || '').split(';')[0].trim().toLowerCase();
    return IMAGE_EXTENSION_BY_MIME[normalized] || '';
};

const getImageExtensionFromUrl = (src = '') => {
    try {
        const pathname = new URL(src, window.location.href).pathname;
        const extension = pathname.split('.').pop()?.toLowerCase() || '';
        if (KNOWN_IMAGE_EXTENSIONS.has(extension)) {
            return extension === 'jpeg' ? 'jpg' : extension;
        }
    } catch {
        // Fall through to the default extension.
    }
    return '';
};

const getImageExtension = (src = '', blob) => {
    return getImageExtensionFromMime(blob?.type)
        || getImageExtensionFromUrl(src)
        || 'png';
};

const fetchImageBlob = async (src = '') => {
    const response = isDataImage(src)
        ? await fetch(src)
        : await fetch(resolveImageUrl(src), {
            cache: 'force-cache',
            credentials: 'include'
        });
    if (!response.ok) {
        throw new Error('图片下载失败');
    }
    return response.blob();
};

const replaceMarkdownImageSources = (markdown = '', replacements = new Map()) => {
    const withMarkdownImages = String(markdown).replace(MARKDOWN_IMAGE_REGEX, (match, alt, target) => {
        const parsed = parseMarkdownImageTarget(target || '');
        const nextSrc = replacements.get(parsed.src);
        if (!nextSrc) {
            return match;
        }
        return `![${alt}](${parsed.leading}${nextSrc}${parsed.trailing})`;
    });

    const withQuotedHtmlImages = withMarkdownImages.replace(
        HTML_IMAGE_SRC_REGEX,
        (match, prefix, quote, src, endQuote) => {
            const nextSrc = replacements.get(String(src || '').trim());
            return nextSrc ? `${prefix}${quote}${nextSrc}${endQuote}` : match;
        }
    );

    return withQuotedHtmlImages.replace(HTML_IMAGE_SRC_UNQUOTED_REGEX, (match, prefix, src) => {
        const nextSrc = replacements.get(String(src || '').trim());
        return nextSrc ? `${prefix}${nextSrc}` : match;
    });
};

const createMarkdownZip = async ({markdown = '', title = '', imageSources = []} = {}) => {
    const zip = new JSZip();
    const assetsFolder = zip.folder('assets');
    const replacements = new Map();
    let failedImageCount = 0;
    let bundledImageCount = 0;

    for (const [index, src] of imageSources.entries()) {
        try {
            const blob = await fetchImageBlob(src);
            const extension = getImageExtension(src, blob);
            const assetPath = `assets/image-${padImageIndex(index + 1)}.${extension}`;
            assetsFolder.file(`image-${padImageIndex(index + 1)}.${extension}`, await blob.arrayBuffer());
            replacements.set(src, assetPath);
            bundledImageCount += 1;
        } catch {
            failedImageCount += 1;
        }
    }

    zip.file('article.md', replaceMarkdownImageSources(markdown, replacements));
    const zipBlob = await zip.generateAsync({
        type: 'blob',
        compression: 'DEFLATE',
        compressionOptions: {
            level: 6
        },
        mimeType: MIME_TYPES.zip
    });
    saveBlob(zipBlob, `${sanitizeFilename(title)}.zip`);

    return {
        bundledImageCount,
        failedImageCount
    };
};

const inlineImages = async (html = '') => {
    const container = document.createElement('div');
    container.innerHTML = html;
    const images = Array.from(container.querySelectorAll('img'));
    let failedImageCount = 0;

    await Promise.all(images.map(async (image) => {
        const src = image.getAttribute('src') || '';
        if (!src || isDataImage(src)) {
            return;
        }

        const resolvedUrl = resolveImageUrl(src);
        image.setAttribute('src', resolvedUrl);

        try {
            const response = await fetch(resolvedUrl, {
                cache: 'force-cache',
                credentials: 'include'
            });
            if (!response.ok) {
                throw new Error('图片下载失败');
            }
            const blob = await response.blob();
            image.setAttribute('src', await blobToDataUrl(blob));
        } catch {
            failedImageCount += 1;
        }
    }));

    return {
        html: container.innerHTML,
        failedImageCount
    };
};

const createExportBody = async (markdown = '') => {
    const rawHtml = renderMarkdownForExport(markdown);
    return inlineImages(rawHtml);
};

const waitForImages = async (root) => {
    const images = Array.from(root.querySelectorAll('img'));
    await Promise.all(images.map((image) => {
        if (image.complete && image.naturalWidth > 0) {
            return Promise.resolve();
        }
        if (image.decode) {
            return image.decode().catch(() => {});
        }
        return new Promise((resolve) => {
            const finish = () => {
                image.removeEventListener('load', finish);
                image.removeEventListener('error', finish);
                resolve();
            };
            image.addEventListener('load', finish, {once: true});
            image.addEventListener('error', finish, {once: true});
            window.setTimeout(finish, 3000);
        });
    }));
};

const waitForNextPaint = (targetWindow = window) => new Promise((resolve) => {
    const requestFrame = targetWindow.requestAnimationFrame || window.requestAnimationFrame;
    requestFrame.call(targetWindow, () => {
        requestFrame.call(targetWindow, resolve);
    });
});

const createHtmlDocument = ({bodyHtml, title = ''} = {}) => `<!doctype html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${escapeHtml(title)}</title>
    <style>${EXPORT_CONTENT_STYLES}</style>
</head>
<body>
    <main class="markdown-preview article-export-body">${bodyHtml}</main>
</body>
</html>`;

const removeExistingPdfFrames = () => {
    document.querySelectorAll(PDF_PRINT_FRAME_SELECTOR).forEach((frame) => {
        frame.parentNode?.removeChild(frame);
    });
};

const createPdfFrame = () => new Promise((resolve, reject) => {
    const frame = document.createElement('iframe');
    frame.setAttribute('aria-hidden', 'true');
    frame.setAttribute('data-article-export-print-frame', 'true');
    frame.setAttribute('data-testid', 'article-pdf-print-frame');
    frame.title = '文章 PDF 导出';
    frame.tabIndex = -1;
    frame.style.position = 'fixed';
    frame.style.left = '-10000px';
    frame.style.top = '0';
    frame.style.width = '794px';
    frame.style.height = '1123px';
    frame.style.border = '0';
    frame.style.opacity = '0';
    frame.style.pointerEvents = 'none';
    frame.onload = () => resolve(frame);
    frame.onerror = () => reject(new Error('PDF 导出容器创建失败'));
    document.body.appendChild(frame);
});

const writeFrameDocument = (frame, html) => {
    const doc = frame.contentDocument;
    if (!doc) {
        throw new Error('PDF 导出容器不可用');
    }
    doc.open();
    doc.write(html);
    doc.close();
    return doc;
};

const waitForFrameReady = async (frame, bodyHtml, title) => {
    const doc = writeFrameDocument(frame, createHtmlDocument({bodyHtml, title}));
    const body = doc.querySelector('.article-export-body');
    if (!body) {
        throw new Error('PDF 导出内容生成失败');
    }
    if (doc.fonts?.ready) {
        await doc.fonts.ready.catch(() => {});
    }
    await waitForImages(body);
    await waitForNextPaint(frame.contentWindow || window);
    return body;
};

const scheduleFrameRemoval = (frame) => {
    let cleanupTimer = null;
    const removeFrame = () => {
        if (cleanupTimer) {
            window.clearTimeout(cleanupTimer);
        }
        cleanupTimer = window.setTimeout(() => {
            frame.parentNode?.removeChild(frame);
        }, PDF_PRINT_AFTERPRINT_CLEANUP_DELAY_MS);
    };

    frame.contentWindow?.addEventListener('afterprint', removeFrame, {once: true});
    window.setTimeout(removeFrame, PDF_PRINT_FALLBACK_CLEANUP_DELAY_MS);
};

const printPdfFrame = (frame) => {
    const printWindow = frame.contentWindow;
    if (!printWindow) {
        throw new Error('PDF 打印容器不可用');
    }

    scheduleFrameRemoval(frame);
    window.dispatchEvent(new CustomEvent(PDF_PRINT_REQUESTED_EVENT));
    printWindow.focus();
    printWindow.print();
};

export const exportArticleAsMarkdown = async ({markdown = '', title = ''} = {}) => {
    const imageSources = collectMarkdownImageSources(markdown);
    if (imageSources.length > 0) {
        return createMarkdownZip({
            markdown,
            title,
            imageSources
        });
    }

    const filename = `${sanitizeFilename(title)}.md`;
    saveBlob(new Blob([markdown || ''], {type: MIME_TYPES.md}), filename);
    return {
        bundledImageCount: 0,
        failedImageCount: 0
    };
};

export const exportArticleAsHtml = async ({markdown = '', title = ''} = {}) => {
    const {html, failedImageCount} = await createExportBody(markdown);
    const documentHtml = createHtmlDocument({bodyHtml: html});
    saveBlob(new Blob([documentHtml], {type: MIME_TYPES.html}), `${sanitizeFilename(title)}.html`);
    return {failedImageCount};
};

export const exportArticleAsPdf = async ({markdown = '', title = ''} = {}) => {
    const {html, failedImageCount} = await createExportBody(markdown);
    removeExistingPdfFrames();
    const frame = await createPdfFrame();

    try {
        await waitForFrameReady(frame, html);
        printPdfFrame(frame);
    } catch (error) {
        frame.parentNode?.removeChild(frame);
        throw error;
    }

    return {
        failedImageCount,
        printRequested: true
    };
};
