import {request} from './http';

export const createReportApi = async (payload) => {
    return await request('/reports', {
        method: 'POST',
        body: payload
    });
};
