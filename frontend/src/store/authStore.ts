import { useState, useEffect } from 'react';
import { UserProfile } from '@/types/global';
import { getStoredToken, setStoredToken, clearStoredToken } from '@/services/api/interceptors';

interface AuthState {
  user: UserProfile | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}

let globalAuthState: AuthState = {
  user: null,
  token: getStoredToken(),
  isAuthenticated: !!getStoredToken(),
  isLoading: false,
};

const listeners = new Set<(state: AuthState) => void>();

const notify = () => {
  listeners.forEach((listener) => listener({ ...globalAuthState }));
};

export const authStore = {
  getState: () => globalAuthState,
  setAuth: (user: UserProfile, token: string) => {
    setStoredToken(token);
    globalAuthState = {
      user,
      token,
      isAuthenticated: true,
      isLoading: false,
    };
    notify();
  },
  setUser: (user: UserProfile | null) => {
    globalAuthState = {
      ...globalAuthState,
      user,
      isAuthenticated: !!user,
    };
    notify();
  },
  logout: () => {
    clearStoredToken();
    globalAuthState = {
      user: null,
      token: null,
      isAuthenticated: false,
      isLoading: false,
    };
    notify();
  },
  subscribe: (listener: (state: AuthState) => void) => {
    listeners.add(listener);
    return () => {
      listeners.delete(listener);
    };
  },
};

export function useAuth() {
  const [state, setState] = useState<AuthState>(authStore.getState());

  useEffect(() => {
    return authStore.subscribe(setState);
  }, []);

  return {
    ...state,
    setAuth: authStore.setAuth,
    setUser: authStore.setUser,
    logout: authStore.logout,
  };
}
