import {watch} from 'vue';
import {refreshPresenceApi} from '@/api/auth';
import {useSession} from '@/stores/session';

const HEARTBEAT_INTERVAL_MS = 30000;
const ACTIVITY_THROTTLE_MS = 15000;

let initialized = false;
let heartbeatTimer = null;
let lastActivityAt = 0;

const stopHeartbeat = () => {
    if (heartbeatTimer) {
        clearInterval(heartbeatTimer);
        heartbeatTimer = null;
    }
};

const sendHeartbeat = async () => {
    try {
        await refreshPresenceApi();
    } catch {
        // 在线心跳是体验增强，失败时等待下一次心跳或鉴权过期事件处理。
    }
};

const startHeartbeat = () => {
    stopHeartbeat();
    sendHeartbeat();
    heartbeatTimer = setInterval(sendHeartbeat, HEARTBEAT_INTERVAL_MS);
};

const handleActivity = () => {
    const now = Date.now();
    if (now - lastActivityAt < ACTIVITY_THROTTLE_MS) {
        return;
    }
    lastActivityAt = now;
    sendHeartbeat();
};

/**
 * 启动全站在线状态心跳。
 */
export const setupPresenceHeartbeat = () => {
    if (initialized || typeof window === 'undefined') {
        return;
    }
    initialized = true;

    const {state, isLoggedIn} = useSession();
    const activityEvents = ['click', 'keydown', 'scroll', 'mousemove', 'touchstart'];
    activityEvents.forEach((eventName) => {
        window.addEventListener(eventName, handleActivity, {passive: true});
    });
    document.addEventListener('visibilitychange', () => {
        if (!document.hidden && isLoggedIn.value) {
            handleActivity();
        }
    });

    watch(
        () => [state.token, state.verified],
        () => {
            if (isLoggedIn.value) {
                startHeartbeat();
            } else {
                stopHeartbeat();
            }
        },
        {immediate: true}
    );
};
