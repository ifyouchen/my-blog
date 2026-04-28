import { computed, ref } from 'vue';

export function useStableListRequest() {
    let requestSeq = 0;

    const initialLoading = ref(false);
    const refreshing = ref(false);
    const hasLoadedOnce = ref(false);
    const errorMessage = ref('');
    const inlineError = ref('');

    const loading = computed(() => initialLoading.value || refreshing.value);

    const runStableRequest = async (requestFn, options = {}) => {
        const {
            silent = hasLoadedOnce.value,
            initialErrorMessage = '加载失败，请稍后重试',
            refreshErrorMessage = '刷新失败，请稍后重试'
        } = options;
        const seq = requestSeq + 1;
        requestSeq = seq;
        inlineError.value = '';

        if (!hasLoadedOnce.value && !silent) {
            initialLoading.value = true;
            errorMessage.value = '';
        } else {
            refreshing.value = true;
        }

        try {
            const result = await requestFn();
            if (seq !== requestSeq) {
                return { ignored: true };
            }
            hasLoadedOnce.value = true;
            errorMessage.value = '';
            return { result };
        } catch (error) {
            if (seq !== requestSeq) {
                return { ignored: true };
            }
            const message = error?.message || (hasLoadedOnce.value ? refreshErrorMessage : initialErrorMessage);
            if (!hasLoadedOnce.value) {
                errorMessage.value = message;
            } else {
                inlineError.value = message;
            }
            return { error };
        } finally {
            if (seq === requestSeq) {
                initialLoading.value = false;
                refreshing.value = false;
            }
        }
    };

    const resetStableRequest = () => {
        requestSeq += 1;
        initialLoading.value = false;
        refreshing.value = false;
        hasLoadedOnce.value = false;
        errorMessage.value = '';
        inlineError.value = '';
    };

    return {
        initialLoading,
        refreshing,
        hasLoadedOnce,
        errorMessage,
        inlineError,
        loading,
        runStableRequest,
        resetStableRequest
    };
}
