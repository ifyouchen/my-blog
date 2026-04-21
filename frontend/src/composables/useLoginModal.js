import { ref } from 'vue';
import { useSession } from '@/stores/session';

const loginModalVisible = ref(false);
const loginCallback = ref(null);
const defaultPrompt = {
    title: '继续前请先登录',
    message: '登录后可以同步你的操作记录，并继续刚才的动作。',
    actionText: '登录并继续'
};
const loginPrompt = ref({ ...defaultPrompt });

const showLoginModal = (callback = null, prompt = {}) => {
    loginModalVisible.value = true;
    loginCallback.value = callback;
    loginPrompt.value = {
        ...defaultPrompt,
        ...prompt
    };
};

const hideLoginModal = () => {
    loginModalVisible.value = false;
    loginCallback.value = null;
    loginPrompt.value = { ...defaultPrompt };
};

const onLoginSuccess = () => {
    if (loginCallback.value) {
        loginCallback.value();
    }
    loginCallback.value = null;
};

export const useLoginModal = () => {
    const { isLoggedIn } = useSession();

    const requireLogin = (callback = null, prompt = {}) => {
        if (isLoggedIn.value) {
            return true;
        }
        showLoginModal(callback, prompt);
        return false;
    };

    return {
        loginModalVisible,
        loginPrompt,
        showLoginModal,
        requireLogin,
        hideLoginModal,
        onLoginSuccess
    };
};
