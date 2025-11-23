import apiClient from '../api';

export interface SymptomAnalysisResponse {
  recommended_medications: Array<{
    name: string;
    reason: string;
    safety_level: 'safe' | 'caution' | 'warning';
  }>;
  medications_to_avoid: Array<{
    name: string;
    reason: string;
    risk_level: 'low' | 'medium' | 'high';
  }>;
  risk_summary: {
    total_risks: number;
    high_risk_ingredients: string[];
    warnings: string[];
    gpt_analysis: string;
  };
}

// 증상 기반 약 추천 분석 결과 조회
export const getAnalysisResult = async (analysisId: string): Promise<SymptomAnalysisResponse> => {
  const response = await apiClient.get<SymptomAnalysisResponse>(`/analysis/${analysisId}`);
  return response.data;
};

// 증상 분석 요청
export const requestSymptomAnalysis = async (symptom: string): Promise<{ analysis_id: string }> => {
  const response = await apiClient.post('/analysis/symptom', { symptom });
  return response.data;
};

