import axios, { type AxiosInstance } from 'axios';
import { setupInterceptors } from '@/services/api/interceptors';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

export const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
  timeout: 15000,
});

setupInterceptors();
