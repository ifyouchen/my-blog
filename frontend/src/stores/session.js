import { computed, reactive } from 'vue';
import { loginApi, registerApi } from '@/api/auth';

const STORAGE_KEY = 'my-blog-session';

const readSession = () => {
    try {
        const raw = localStorage.getItem(STORAGE_KEY);
        return raw ? JSON.parse(raw) : null;
    } catch (error) {
        return null;
    }
};

const initialSession = readSession();

const state = reactive({
    user: initialSession ? initialSession.user : null,
    token: initialSession ? initialSession.token : ''
});

const saveSession = (session) => {
    state.user = session.user;
    state.token = session.token;
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
};

const clearSession = () => {
    state.user = null;
    state.token = '';
    localStorage.removeItem(STORAGE_KEY);
};

const isNetworkError = (error) => {
    return error instanceof TypeError || error.message === 'Failed to fetch';
};

export const useSession = () => {
    const isLoggedIn = computed(() => Boolean(state.user));

    const login = async ({ account, password }) => {
        try {
            saveSession(await loginApi({ account, password }));
            return;
        } catch (error) {
            if (!isNetworkError(error)) {
                throw error;
            }
        }

        const displayName = account.includes('@') ? account.split('@')[0] : account;
        saveSession({
            token: 'local-dev-token',
            user: {
                id: 1001,
                username: displayName,
                nickname: displayName || '技术作者',
                role: 'USER',
                avatar: 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=96&q=80'
            }
        });
    };

    const register = async ({ username, email, password }) => {
        try {
            saveSession(await registerApi({ username, email, password }));
            return;
        } catch (error) {
            if (!isNetworkError(error)) {
                throw error;
            }
        }

        saveSession({
            token: 'local-dev-token',
            user: {
                id: 1002,
                username,
                nickname: username,
                email,
                role: 'USER',
                avatar: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=96&q=80'
            }
        });
    };

    const logout = () => {
        clearSession();
    };

    return {
        state,
        isLoggedIn,
        login,
        register,
        logout
    };
};
