import { apiClient } from './client';
import type { ImageUploadResponse } from '../types/api';

export const imagesApi = {
  uploadImage: async (file: File): Promise<ImageUploadResponse> => {
    const formData = new FormData();
    formData.append('file', file);

    const response = await apiClient.post<ImageUploadResponse>(
      '/api/posts/images',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );
    return response.data;
  },
};

