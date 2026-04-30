import {onBeforeUnmount, onMounted, readonly, ref} from 'vue';

const width = ref(typeof window !== 'undefined' ? window.innerWidth : 1024);
const height = ref(typeof window !== 'undefined' ? window.innerHeight : 768);

let listeners = 0;
let handler = null;

function onResize() {
    width.value = window.innerWidth;
    height.value = window.innerHeight;
}

function startListening() {
    if (!handler) {
        handler = onResize;
        window.addEventListener('resize', handler, {passive: true});
    }
    listeners++;
}

function stopListening() {
    listeners--;
    if (listeners <= 0) {
        listeners = 0;
        if (handler) {
            window.removeEventListener('resize', handler);
            handler = null;
        }
    }
}

/**
 * Reactive window size composable.
 * Shared state across all consumers; auto-cleans event listener when no consumers remain.
 */
export function useWindowSize() {
    onMounted(() => startListening());
    onBeforeUnmount(() => stopListening());

    return {
        width: readonly(width),
        height: readonly(height)
    };
}

