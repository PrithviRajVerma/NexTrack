import { type InternalAxiosRequestConfig, type AxiosResponse, AxiosError } from 'axios';
import { apiClient } from '@/lib/axios';
import { ENDPOINTS } from './endpoints';

const TOKEN_KEY = 'nextrack_auth_token';

export const getStoredToken = (): string | null => {
  return localStorage.getItem(TOKEN_KEY);
};

export const setStoredToken = (token: string): void => {
  localStorage.setItem(TOKEN_KEY, token);
};

export const clearStoredToken = (): void => {
  localStorage.removeItem(TOKEN_KEY);
};

export class ApiError extends Error {
  status: number;
  data: any;

  constructor(status: number, message: string, data?: any) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;
  }
}

let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value?: unknown) => void;
  reject: (reason?: unknown) => void;
}> = [];

const processQueue = (error: Error | null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve();
    }
  });
  failedQueue = [];
};

export const setupInterceptors = () => {
  // Request Interceptor: Attach JWT Token
  apiClient.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
      const token = getStoredToken();
      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    },
    (error) => Promise.reject(error)
  );

  // Response Interceptor: Handle Refresh Token Rotation & Error Mapping
  apiClient.interceptors.response.use(
    (response: AxiosResponse) => response,
    async (error: AxiosError) => {
      const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

      // Handle 401 Unauthorized with token refresh rotation
      if (
        error.response?.status === 401 &&
        originalRequest &&
        !originalRequest._retry &&
        !originalRequest.url?.includes(ENDPOINTS.AUTH.LOGIN) &&
        !originalRequest.url?.includes(ENDPOINTS.AUTH.REFRESH)
      ) {
        if (isRefreshing) {
          return new Promise((resolve, reject) => {
            failedQueue.push({ resolve, reject });
          })
            .then(() => apiClient(originalRequest))
            .catch((err) => Promise.reject(err));
        }

        originalRequest._retry = true;
        isRefreshing = true;

        try {
          // Attempt silent refresh using HttpOnly refresh token cookie
          const res = await apiClient.post(ENDPOINTS.AUTH.REFRESH);
          const newToken = (res.data as any)?.accessToken;
          if (newToken) {
            setStoredToken(newToken);
            processQueue(null);
            return apiClient(originalRequest);
          }
        } catch (refreshErr) {
          processQueue(refreshErr as Error);
          clearStoredToken();
          return Promise.reject(refreshErr);
        } finally {
          isRefreshing = false;
        }
      }

      // Format RFC 7807 Problem Details
      const status = error.response?.status || 500;
      const data = error.response?.data as any;
      const message = data?.detail || data?.title || data?.message || error.message || 'An unexpected error occurred';

      return Promise.reject(new ApiError(status, message, data));
    }
  );
};
