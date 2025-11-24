import { useState, useRef } from 'react';
import { Link } from 'react-router-dom';
import { analysisApi } from '../api/analysis';
import { useAuthStore } from '../store/authStore';
import type { OcrAnalysisResponse } from '../types/api';

export default function OcrAnalysisPage() {
  const { user } = useAuthStore();
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<OcrAnalysisResponse | null>(null);
  const [error, setError] = useState('');
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      if (!file.type.startsWith('image/')) {
        setError('이미지 파일만 업로드 가능합니다.');
        return;
      }
      setImageFile(file);
      setError('');
      setResult(null);

      // 미리보기 생성
      const reader = new FileReader();
      reader.onloadend = () => {
        setPreview(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!imageFile) {
      setError('이미지를 선택해주세요.');
      return;
    }

    setError('');
    setLoading(true);
    setResult(null);

    try {
      // 이미지를 base64로 변환
      const base64 = await new Promise<string>((resolve, reject) => {
        const reader = new FileReader();
        reader.onloadend = () => {
          const base64String = (reader.result as string).split(',')[1];
          resolve(base64String);
        };
        reader.onerror = reject;
        reader.readAsDataURL(imageFile);
      });

      const data = await analysisApi.analyzeOcr({
        ...(user?.id && { userId: user.id }), // 로그인한 경우에만 userId 전송
        imageData: base64,
        base64: true,
      });
      setResult(data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'OCR 분석에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleReset = () => {
    setImageFile(null);
    setPreview(null);
    setResult(null);
    setError('');
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const getSafetyLevelColor = (level: string) => {
    switch (level.toUpperCase()) {
      case 'SAFE':
        return 'bg-green-100 text-green-800 border-green-200';
      case 'CAUTION':
        return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'WARNING':
        return 'bg-orange-100 text-orange-800 border-orange-200';
      case 'DANGER':
        return 'bg-red-100 text-red-800 border-red-200';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const getSafetyLevelLabel = (level: string) => {
    switch (level.toUpperCase()) {
      case 'SAFE':
        return '복용 가능';
      case 'CAUTION':
        return '주의 필요';
      case 'WARNING':
        return '주의 깊게 복용';
      case 'DANGER':
        return '고위험 성분 포함';
      default:
        return level;
    }
  };

  const getRiskLevelColor = (level: string) => {
    switch (level.toUpperCase()) {
      case 'LOW':
        return 'bg-green-100 text-green-800';
      case 'MEDIUM':
        return 'bg-yellow-100 text-yellow-800';
      case 'HIGH':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  return (
    <div className="px-4 py-8">
      <div className="max-w-4xl mx-auto">
        <h1 className="text-3xl font-bold text-gray-900 mb-6">성분표 분석</h1>
        <p className="text-gray-600 mb-6">
          약물 성분표 사진을 업로드하시면, OCR로 성분을 추출하고 알러지 정보와 비교하여 안전성을 평가합니다.
        </p>

        {!user && (
          <div className="mb-6 bg-blue-50 border border-blue-200 rounded-lg p-4">
            <p className="text-blue-800 text-sm">
              💡 <strong>더 정확한 성분 분석을 받으시려면?</strong>
            </p>
            <p className="text-blue-700 text-sm mt-2">
              <Link to="/login" className="underline font-medium hover:text-blue-900">
                로그인
              </Link>
              {' '}후 알러지 정보를 등록하시면, 본인에게 피해야 할 성분을 더 자세히 알 수 있습니다.
            </p>
          </div>
        )}

        <form onSubmit={handleSubmit} className="mb-8">
          <div className="bg-white rounded-lg shadow p-6">
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                성분표 이미지 업로드
              </label>
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                onChange={handleFileChange}
                className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-md file:border-0 file:text-sm file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100"
              />
            </div>

            {preview && (
              <div className="mb-4">
                <p className="text-sm font-medium text-gray-700 mb-2">미리보기</p>
                <img
                  src={preview}
                  alt="미리보기"
                  className="max-w-full h-auto border border-gray-300 rounded-md"
                />
              </div>
            )}

            <div className="flex gap-2">
              <button
                type="submit"
                disabled={loading || !imageFile}
                className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
              >
                {loading ? '분석 중...' : '분석하기'}
              </button>
              {preview && (
                <button
                  type="button"
                  onClick={handleReset}
                  className="px-4 py-2 bg-gray-200 text-gray-700 rounded-md hover:bg-gray-300"
                >
                  초기화
                </button>
              )}
            </div>
          </div>
        </form>

        {error && (
          <div className="mb-6 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
            {error}
          </div>
        )}

        {result && (
          <div className="space-y-6">
            <div className={`border-2 rounded-lg p-6 ${getSafetyLevelColor(result.analysis.safetyLevel)}`}>
              <h2 className="text-2xl font-semibold mb-2">
                안전성 평가: {getSafetyLevelLabel(result.analysis.safetyLevel)}
              </h2>
              <p className="text-gray-700">{result.analysis.overallAssessment}</p>
            </div>

            {result.ocrText && (
              <div className="bg-gray-50 border border-gray-200 rounded-lg p-6">
                <h2 className="text-xl font-semibold text-gray-900 mb-4">📄 추출된 텍스트</h2>
                <pre className="whitespace-pre-wrap text-sm text-gray-700 bg-white p-4 rounded border">
                  {result.ocrText}
                </pre>
              </div>
            )}

            {result.extractedIngredients.length > 0 && (
              <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
                <h2 className="text-xl font-semibold text-blue-900 mb-4">🔍 추출된 성분</h2>
                <div className="flex flex-wrap gap-2">
                  {result.extractedIngredients.map((ingredient, index) => (
                    <span
                      key={index}
                      className="px-3 py-1 bg-white border border-blue-300 rounded-full text-sm text-blue-700"
                    >
                      {ingredient}
                    </span>
                  ))}
                </div>
              </div>
            )}

            {result.analysis.ingredientRisks.length > 0 && (
              <div className="bg-white border border-gray-200 rounded-lg p-6">
                <h2 className="text-xl font-semibold text-gray-900 mb-4">⚠️ 성분 위험도 분석</h2>
                <div className="space-y-4">
                  {result.analysis.ingredientRisks.map((risk, index) => (
                    <div key={index} className="border border-gray-200 rounded p-4">
                      <div className="flex items-center justify-between mb-2">
                        <h3 className="font-semibold text-lg text-gray-900">
                          {risk.ingredientName}
                        </h3>
                        <span
                          className={`px-2 py-1 text-xs font-medium rounded ${getRiskLevelColor(risk.riskLevel)}`}
                        >
                          {risk.riskLevel}
                        </span>
                      </div>
                      <p className="text-sm text-gray-600 mb-2">함량: {risk.content}</p>
                      <p className="text-gray-700 mb-2">{risk.reason}</p>
                      <p className="text-sm text-red-600 font-medium">
                        알러지 위험: {risk.allergyRisk}
                      </p>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {result.analysis.expectedSideEffects.length > 0 && (
              <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-6">
                <h2 className="text-xl font-semibold text-yellow-900 mb-4">💊 예상 부작용</h2>
                <ul className="space-y-2">
                  {result.analysis.expectedSideEffects.map((effect, index) => (
                    <li key={index} className="flex items-start">
                      <span className="mr-2">•</span>
                      <span className="text-gray-700">{effect}</span>
                    </li>
                  ))}
                </ul>
              </div>
            )}

            {result.analysis.recommendations.length > 0 && (
              <div className="bg-green-50 border border-green-200 rounded-lg p-6">
                <h2 className="text-xl font-semibold text-green-900 mb-4">✅ 권장사항</h2>
                <ul className="space-y-2">
                  {result.analysis.recommendations.map((recommendation, index) => (
                    <li key={index} className="flex items-start">
                      <span className="mr-2">•</span>
                      <span className="text-gray-700">{recommendation}</span>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

