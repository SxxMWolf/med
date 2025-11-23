import apiClient from '../api';

export interface MedicineSearchResult {
  id: string;
  name: string;
  company: string;
  ingredients: string[];
}

// 약 검색 (Spring Boot → 식약처 API)
export const searchMedicine = async (keyword: string): Promise<MedicineSearchResult[]> => {
  const response = await apiClient.get('/medicine/search', {
    params: { keyword },
  });
  return response.data;
};

// 약 상세 정보 조회
export const getMedicineDetail = async (id: string) => {
  const response = await apiClient.get(`/medicine/${id}`);
  return response.data;
};

