const ENABLED = import.meta.env.DEV || import.meta.env.VITE_TRACK_ENABLED === 'true';

export const track = (event, props = {}) => {
    if (!ENABLED) return;
    const payload = {
        event,
        ...props,
        _ts: Date.now()
    };
    console.log(`[track] ${event}`, payload);
    // 可扩展：上报到后端或第三方平台
    // navigator.sendBeacon?.('/api/track', JSON.stringify(payload));
};
