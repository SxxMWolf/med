import apiClient from '../api';

// 알러지 성분 목록 가져오기
export const getAllergies = async () => {
  const response = await apiClient.get('/allergies');
  return response.data;
};

// 알러지 성분 추가
export const addAllergy = async (ingredient: string) => {
  const response = await apiClient.post('/allergies', { ingredient });
  return response.data;
};

// 알러지 성분 삭제
export const deleteAllergy = async (id: string) => {
  const response = await apiClient.delete(`/allergies/${id}`);
  return response.data;
};

