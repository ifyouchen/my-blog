import hljs from 'highlight.js/lib/core';
import bash from 'highlight.js/lib/languages/bash';
import css from 'highlight.js/lib/languages/css';
import java from 'highlight.js/lib/languages/java';
import javascript from 'highlight.js/lib/languages/javascript';
import json from 'highlight.js/lib/languages/json';
import sql from 'highlight.js/lib/languages/sql';
import typescript from 'highlight.js/lib/languages/typescript';
import xml from 'highlight.js/lib/languages/xml';
import { createLowlight } from 'lowlight';

const languageDefinitions = {
    bash,
    css,
    java,
    javascript,
    json,
    sql,
    typescript,
    xml
};

const lowlightDefinitions = {
    ...languageDefinitions,
    html: xml,
    js: javascript,
    shell: bash,
    sh: bash,
    ts: typescript
};

const aliases = {
    bash: 'bash',
    css: 'css',
    html: 'xml',
    java: 'java',
    js: 'javascript',
    javascript: 'javascript',
    json: 'json',
    shell: 'bash',
    sh: 'bash',
    sql: 'sql',
    ts: 'typescript',
    typescript: 'typescript',
    xml: 'xml'
};

Object.entries(languageDefinitions).forEach(([name, definition]) => {
    if (!hljs.getLanguage(name)) {
        hljs.registerLanguage(name, definition);
    }
});

export const normalizeCodeLanguage = (language = '') => {
    const key = String(language).trim().toLowerCase();
    return aliases[key] || key || 'text';
};

export const getCodeLanguageLabel = (language = '') => {
    const normalized = normalizeCodeLanguage(language);
    if (normalized === 'xml') {
        return 'HTML';
    }
    if (normalized === 'text') {
        return 'CODE';
    }
    return normalized.toUpperCase();
};

export const getCodeLanguageClass = (language = '') => {
    const normalized = normalizeCodeLanguage(language);
    if (normalized === 'xml') {
        return 'lang-html';
    }
    return `lang-${normalized}`;
};

export const highlightCode = (code = '', language = '') => {
    const normalized = normalizeCodeLanguage(language);
    if (hljs.getLanguage(normalized)) {
        return hljs.highlight(code, {
            language: normalized,
            ignoreIllegals: true
        }).value;
    }
    return hljs.highlightAuto(code).value;
};

export const createBlogLowlight = () => {
    return createLowlight(lowlightDefinitions);
};
