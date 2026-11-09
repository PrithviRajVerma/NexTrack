import { apiClient } from '@/lib/axios';
import { ENDPOINTS } from '@/services/api/endpoints';
import { LoginCredentials, AuthUserResponse } from '../types/auth.types';

export const authApi = {
  login: async (credentials: LoginCredentials): Promise<AuthUserResponse> => {
    const res = await apiClient.post<AuthUserResponse>(ENDPOINTS.AUTH.LOGIN, credentials);
    return res.data;
  },

  logout: async (): Promise<void> => {
    await apiClient.post(ENDPOINTS.AUTH.LOGOUT);
  },

  refreshToken: async (): Promise<{ accessToken: string }> => {
    const res = await apiClient.post<{ accessToken: string }>(ENDPOINTS.AUTH.REFRESH);
    return res.data;
  },
};
