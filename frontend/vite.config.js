import { fileURLToPath, URL } from 'node:url';
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import compression from 'vite-plugin-compression';
import { visualizer } from 'rollup-plugin-visualizer';

export default defineConfig({
    plugins: [
        vue(),
        compression({ algorithm: 'gzip', ext: '.gz', deleteOriginFile: false }),
        compression({ algorithm: 'brotliCompress', ext: '.br', deleteOriginFile: false }),
        visualizer({ open: false, gzipSize: true, brotliSize: true, filename: 'dist/stats.html' })
    ],
    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url))
        }
    },
    build: {
        rollupOptions: {
            output: {
                manualChunks(id) {
                    if (id.includes('node_modules')) {
                        if (id.includes('prosemirror-model') || id.includes('prosemirror-state') || id.includes('prosemirror-view')) {
                            return 'editor-core';
                        }
                        if (id.includes('@tiptap/starter-kit') || id.includes('@tiptap/core')) {
                            return 'editor-core';
                        }
                        if (id.includes('@tiptap/extension-table') || id.includes('@tiptap/extension-table-cell') || id.includes('@tiptap/extension-table-header') || id.includes('@tiptap/extension-table-row')) {
                            return 'editor-table';
                        }
                        if (id.includes('@tiptap/extension-task-list') || id.includes('@tiptap/extension-task-item')) {
                            return 'editor-task';
                        }
                        if (id.includes('@tiptap/extension-image') || id.includes('tiptap-extension-resize-image')) {
                            return 'editor-image';
                        }
                        if (id.includes('@tiptap/extension-code-block-lowlight') || id.includes('@tiptap/extension-link') || id.includes('@tiptap/extension-underline') || id.includes('@tiptap/extension-text-align')) {
                            return 'editor-extras';
                        }
                        if (id.includes('markdown-it') || id.includes('highlight.js') || id.includes('lowlight')) {
                            return 'markdown';
                        }
                        if (id.includes('jszip')) {
                            return 'jszip';
                        }
                    }
                    if (id.includes('/src/views/admin/')) {
                        return 'admin';
                    }
                    return undefined;
                }
            }
        },
        chunkSizeWarningLimit: 600
    },
    optimizeDeps: {
        include: ['jszip']
    },
    server: {
        port: 5173,
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true
            }
        }
    }
});
