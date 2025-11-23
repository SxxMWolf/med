import { apiClient } from './client';
import type { UserAllergy, User } from '../types/api';

export const usersApi = {
  getUser: async (userId: number): Promise<User> => {
    const response = await apiClient.get<User>(`/api/users/${userId}`);
    return response.data;
  },

  getAllergies: async (userId: number): Promise<UserAllergy[]> => {
    const response = await apiClient.get<UserAllergy[]>(`/api/users/${userId}/allergies`);
    return response.data;
  },

  addAllergy: async (userId: number, data: { ingredientName: string; description?: string; severity?: string }): Promise<UserAllergy> => {
    const response = await apiClient.post<UserAllergy>(`/api/users/${userId}/allergies`, data);
    return response.data;
  },

  deleteAllergy: async (userId: number, allergyId: number): Promise<void> => {
    await apiClient.delete(`/api/users/${userId}/allergies/${allergyId}`);
  },
};

