import { apiClient } from './client';
import type {
  RegisterRequest,
  LoginRequest,
  LoginResponse,
  UserResponse,
  ChangePasswordRequest,
  ChangeNicknameRequest,
  MessageResponse,
  FindUsernameRequest,
  FindPasswordRequest,
} from '../types/api';

export const authApi = {
  register: async (data: RegisterRequest): Promise<UserResponse> => {
    const response = await apiClient.post<UserResponse>('/api/auth/register', data);
    return response.data;
  },

  login: async (data: LoginRequest): Promise<LoginResponse> => {
    const response = await apiClient.post<LoginResponse>('/api/auth/login', data);
    return response.data;
  },

  getCurrentUser: async (): Promise<UserResponse> => {
    const response = await apiClient.get<UserResponse>('/api/auth/me');
    return response.data;
  },

  findUsername: async (data: FindUsernameRequest): Promise<MessageResponse> => {
    const response = await apiClient.post<MessageResponse>('/api/auth/find-username', data);
    return response.data;
  },

  findPassword: async (data: FindPasswordRequest): Promise<MessageResponse> => {
    const response = await apiClient.post<MessageResponse>('/api/auth/find-password', data);
    return response.data;
  },

  changePassword: async (data: ChangePasswordRequest): Promise<MessageResponse> => {
    const response = await apiClient.post<MessageResponse>('/api/auth/change-password', data);
    return response.data;
  },

  changeNickname: async (data: ChangeNicknameRequest): Promise<MessageResponse> => {
    const response = await apiClient.post<MessageResponse>('/api/auth/change-nickname', data);
    return response.data;
  },
};

