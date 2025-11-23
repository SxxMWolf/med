import * as SecureStore from 'expo-secure-store';
import apiClient from './api';

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface SignupData {
  email: string;
  password: string;
  name: string;
}

export interface AuthResponse {
  access_token: string;
  refresh_token: string;
  user: {
    id: string;
    email: string;
    name: string;
  };
}

// 로그인
export const login = async (credentials: LoginCredentials): Promise<AuthResponse> => {
  const response = await apiClient.post<AuthResponse>('/auth/login', credentials);
  const { access_token, refresh_token, user } = response.data;

  // 토큰을 SecureStore에 저장
  await SecureStore.setItemAsync('access_token', access_token);
  await SecureStore.setItemAsync('refresh_token', refresh_token);

  return response.data;
};

// 회원가입
export const signup = async (data: SignupData): Promise<AuthResponse> => {
  const response = await apiClient.post<AuthResponse>('/auth/signup', data);
  const { access_token, refresh_token, user } = response.data;

  // 토큰을 SecureStore에 저장
  await SecureStore.setItemAsync('access_token', access_token);
  await SecureStore.setItemAsync('refresh_token', refresh_token);

  return response.data;
};

// 로그아웃
export const logout = async (): Promise<void> => {
  try {
    await apiClient.post('/auth/logout');
  } catch (error) {
    console.error('Logout error:', error);
  } finally {
    await SecureStore.deleteItemAsync('access_token');
    await SecureStore.deleteItemAsync('refresh_token');
  }
};

// 현재 사용자 정보 가져오기
export const getCurrentUser = async () => {
  const response = await apiClient.get('/auth/me');
  return response.data;
};

// 로그인 상태 확인
export const isAuthenticated = async (): Promise<boolean> => {
  const token = await SecureStore.getItemAsync('access_token');
  return !!token;
};

