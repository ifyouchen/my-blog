import { request } from './http';

/**
 * 上传图片（经服务端代理转发）。
 */
export const uploadImageApi = async (file, scope = 'cover') => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('scope', scope);
    return await request('/uploads/images', {
        method: 'POST',
        body: formData
    });
};

/**
 * 上传附件（经服务端代理转发）。
 */
export const uploadFileApi = async (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return await request('/uploads/files', {
        method: 'POST',
        body: formData
    });
};

/**
 * 请求预签名 URL 信息。
 */
const getPresignedUrlInfo = async (file, scope) => {
    return await request('/uploads/presigned-url', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            fileName: file.name,
            fileSize: file.size,
            contentType: file.type,
            scope
        })
    });
};

/**
 * 将文件直接 PUT 到预签名 URL（直传 COS）。
 */
const uploadDirectlyToCos = async (presignedUrl, file) => {
    const response = await fetch(presignedUrl, {
        method: 'PUT',
        body: file,
        headers: { 'Content-Type': file.type }
    });
    if (!response.ok) {
        throw new Error('图片上传到云存储失败');
    }
};

/**
 * 统一图片上传入口。
 *
 * - COS 模式：预签名 URL 直传，图片处理由 COS 按需完成。
 * - 本地开发模式：降级为服务端代理上传（uploadImageApi）。
 *
 * 返回值与 uploadImageApi 一致：{ url, originalUrl, thumbnailUrl?, mediumUrl? }
 */
export const uploadImage = async (file, scope = 'cover') => {
    const info = await getPresignedUrlInfo(file, scope);

    if (info.uploadType === 'proxy') {
        return uploadImageApi(file, scope);
    }

    await uploadDirectlyToCos(info.presignedUrl, file);

    return {
        url: info.url,
        originalUrl: info.originalUrl,
        thumbnailUrl: info.thumbnailUrl || null,
        mediumUrl: info.mediumUrl || null
    };
};
