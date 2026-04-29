import { reactive } from 'vue';

export const useConfirmDialog = () => {
    const dialog = reactive({
        visible: false,
        eyebrow: '操作确认',
        title: '',
        message: '',
        confirmText: '确认',
        cancelText: '取消',
        tone: 'primary',
        loading: false,
        onConfirm: null
    });

    const openConfirmDialog = ({
        eyebrow = '操作确认',
        title,
        message,
        confirmText = '确认',
        cancelText = '取消',
        tone = 'primary',
        onConfirm
    }) => {
        dialog.eyebrow = eyebrow;
        dialog.title = title || '';
        dialog.message = message || '';
        dialog.confirmText = confirmText;
        dialog.cancelText = cancelText;
        dialog.tone = tone;
        dialog.onConfirm = onConfirm;
        dialog.visible = true;
    };

    const closeConfirmDialog = () => {
        if (dialog.loading) {
            return;
        }
        dialog.visible = false;
        dialog.eyebrow = '操作确认';
        dialog.title = '';
        dialog.message = '';
        dialog.confirmText = '确认';
        dialog.cancelText = '取消';
        dialog.tone = 'primary';
        dialog.onConfirm = null;
    };

    const executeConfirmDialog = async () => {
        if (dialog.loading || typeof dialog.onConfirm !== 'function') {
            return;
        }
        dialog.loading = true;
        try {
            await dialog.onConfirm();
            closeConfirmDialog();
        } finally {
            dialog.loading = false;
        }
    };

    return {
        confirmDialog: dialog,
        openConfirmDialog,
        closeConfirmDialog,
        executeConfirmDialog
    };
};
