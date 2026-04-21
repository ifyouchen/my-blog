import { computed, reactive } from 'vue';
import { currentUserApi, loginApi, registerApi } from '@/api/auth';

const STORAGE_KEY = 'my-blog-session';

const isValidStoredSession = (session) => {
    return Boolean(
        session
        && session.token
        && session.user
        && session.token !== 'local-dev-token'
    );
};

const readSession = () => {
    try {
        const raw = localStorage.getItem(STORAGE_KEY);
        const session = raw ? JSON.parse(raw) : null;
        if (isValidStoredSession(session)) {
            return session;
        }
        localStorage.removeItem(STORAGE_KEY);
        return null;
    } catch (error) {
        localStorage.removeItem(STORAGE_KEY);
        return null;
    }
};

const initialSession = readSession();

const state = reactive({
    user: initialSession ? initialSession.user : null,
    token: initialSession ? initialSession.token : '',
    verified: false,
    ready: !initialSession
});

const saveSession = (session) => {
    state.user = session.user;
    state.token = session.token;
    state.verified = true;
    state.ready = true;
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
};

const clearSession = () => {
    state.user = null;
    state.token = '';
    state.verified = false;
    state.ready = true;
    localStorage.removeItem(STORAGE_KEY);
};

if (typeof window !== 'undefined') {
    window.addEventListener('my-blog-auth-expired', clearSession);
}

export const useSession = () => {
    const isLoggedIn = computed(() => Boolean(state.user && state.token && state.verified));

    const initializeSession = async () => {
        if (!state.token) {
            clearSession();
            return;
        }
        state.ready = false;
        try {
            const user = await currentUserApi();
            saveSession({
                token: state.token,
                user
            });
        } catch (error) {
            clearSession();
        }
    };

    const login = async ({ account, password }) => {
        saveSession(await loginApi({ account, password }));
    };

    const register = async ({ username, email, password }) => {
        saveSession(await registerApi({ username, email, password }));
    };

    const logout = () => {
        clearSession();
    };

    const updateCurrentUser = (user) => {
        saveSession({
            token: state.token,
            user
        });
    };

    return {
        state,
        isLoggedIn,
        initializeSession,
        login,
        register,
        logout,
        updateCurrentUser
    };
};
