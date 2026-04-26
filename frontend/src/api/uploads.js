import { request } from './http';

export const uploadImageApi = async (file, scope = 'cover') => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('scope', scope);
    return await request('/uploads/images', {
        method: 'POST',
        body: formData
    });
};

export const uploadFileApi = async (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return await request('/uploads/files', {
        method: 'POST',
        body: formData
    });
};
