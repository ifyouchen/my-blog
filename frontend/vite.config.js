import { fileURLToPath, URL } from 'node:url';
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
    plugins: [vue()],
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
                        if (id.includes('@tiptap') || id.includes('prosemirror')) {
                            return 'editor';
                        }
                        if (id.includes('markdown-it') || id.includes('highlight.js') || id.includes('lowlight')) {
                            return 'markdown';
                        }
                    }
                    if (id.includes('/src/views/admin/')) {
                        return 'admin';
                    }
                    return undefined;
                }
            }
        }
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
