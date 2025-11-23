import apiClient from '../api';

export interface OCRAnalysisResponse {
  extracted_text: string;
  normalized_ingredients: string[];
  analysis: {
    status: 'safe' | 'caution' | 'high_risk';
    risk_level: 'low' | 'medium' | 'high';
    matching_allergens: string[];
    warnings: string[];
    gpt_summary: string;
  };
}

// 약 성분표 OCR 분석 (Spring Boot → Python API)
export const analyzeMedicationImage = async (
  imageUri: string
): Promise<OCRAnalysisResponse> => {
  // FormData 생성 (React Native)
  const formData = new FormData();
  
  // 파일명 추출
  const filename = imageUri.split('/').pop() || 'image.jpg';
  const match = /\.(\w+)$/.exec(filename);
  const type = match ? `image/${match[1]}` : 'image/jpeg';

  formData.append('image', {
    uri: imageUri,
    name: filename,
    type: type,
  } as any);

  const response = await apiClient.post<OCRAnalysisResponse>(
    '/ocr/analyze',
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  );
  return response.data;
};

