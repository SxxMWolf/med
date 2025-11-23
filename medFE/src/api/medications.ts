import { apiClient } from './client';
import type { MedicationInfo } from '../types/api';

export const medicationsApi = {
  searchMedication: async (name: string): Promise<MedicationInfo> => {
    const response = await apiClient.get<MedicationInfo>(
      `/api/medications/search?name=${encodeURIComponent(name)}`
    );
    return response.data;
  },

  searchMedications: async (names: string[]): Promise<MedicationInfo[]> => {
    const response = await apiClient.post<MedicationInfo[]>(
      '/api/medications/search/batch',
      names
    );
    return response.data;
  },
};

