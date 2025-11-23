import { apiClient } from './client';
import type {
  SymptomAnalysisRequest,
  SymptomAnalysisResponse,
  SideEffectAnalysisRequest,
  SideEffectAnalysisResponse,
  OcrAnalysisRequest,
  OcrAnalysisResponse,
} from '../types/api';

export const analysisApi = {
  analyzeSymptom: async (data: SymptomAnalysisRequest): Promise<SymptomAnalysisResponse> => {
    const response = await apiClient.post<SymptomAnalysisResponse>('/api/analysis/symptom', data);
    return response.data;
  },

  analyzeSideEffect: async (data: SideEffectAnalysisRequest): Promise<SideEffectAnalysisResponse> => {
    const response = await apiClient.post<SideEffectAnalysisResponse>('/api/analysis/side-effect', data);
    return response.data;
  },

  analyzeOcr: async (data: OcrAnalysisRequest): Promise<OcrAnalysisResponse> => {
    const response = await apiClient.post<OcrAnalysisResponse>('/api/analysis/ocr', data);
    return response.data;
  },
};

