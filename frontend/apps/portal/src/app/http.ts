import { createApiClient } from '@yunshu/http';

// 该实例仅属于 学员端，不从公共包导出全局登录态。
export const api = createApiClient('portal');
