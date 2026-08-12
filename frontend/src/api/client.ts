import axios, { type AxiosRequestConfig } from 'axios';

import { ApiError, type ApiResponse } from '@/types/api';
import { clearSession, readSession } from '@/utils/session';

export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '/api').replace(/\/$/, '');

function isApiResponse(value: unknown): value is ApiResponse<unknown> {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as Record<string, unknown>).code === 'number' &&
    typeof (value as Record<string, unknown>).message === 'string'
  );
}

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 90_000,
});

apiClient.interceptors.request.use((config) => {
  const session = readSession();
  if (session) {
    config.headers.Authorization = `Bearer ${session.token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => {
    if (isApiResponse(response.data) && response.data.code !== 200) {
      return Promise.reject(new ApiError(response.data.message, { code: response.data.code }));
    }
    return response;
  },
  (error: unknown) => {
    if (axios.isAxiosError(error)) {
      const body = error.response?.data;
      const message = isApiResponse(body) ? body.message : '请求失败，请稍后重试';
      const code = isApiResponse(body) ? body.code : undefined;
      const status = error.response?.status;

      if (status === 401 || code === 401) {
        clearSession();
        window.dispatchEvent(new Event('auth:expired'));
      }

      return Promise.reject(new ApiError(message, { status, code }));
    }

    return Promise.reject(error);
  },
);

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await apiClient.request<ApiResponse<T>>(config);
  return response.data.data;
}
