import { create } from 'zustand';
import type { UserInfo } from '../types/api';

interface AuthState {
  user: UserInfo | null;
  token: string | null;
  isAuthenticated: boolean;
  setAuth: (token: string, user: UserInfo) => void;
  clearAuth: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: JSON.parse(localStorage.getItem('user') || 'null'),
  token: localStorage.getItem('accessToken'),
  isAuthenticated: !!localStorage.getItem('accessToken'),
  setAuth: (token, user) => {
    localStorage.setItem('accessToken', token);
    localStorage.setItem('user', JSON.stringify(user));
    set({ token, user, isAuthenticated: true });
  },
  clearAuth: () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('user');
    set({ token: null, user: null, isAuthenticated: false });
  },
}));

