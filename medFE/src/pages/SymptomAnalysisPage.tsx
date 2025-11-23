import { useState } from 'react';
import { analysisApi } from '../api/analysis';
import { useAuthStore } from '../store/authStore';
import type { SymptomAnalysisResponse } from '../types/api';

export default function SymptomAnalysisPage() {
  const { user } = useAuthStore();
  const [symptomText, setSymptomText] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<SymptomAnalysisResponse | null>(null);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;

    setError('');
    setLoading(true);
    setResult(null);

    try {
      const data = await analysisApi.analyzeSymptom({
        userId: user.id,
        symptomText,
      });
      setResult(data);
    } catch (err: any) {
      setError(err.response?.data?.message || '증상 분석에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="px-4 py-8">
      <div className="max-w-4xl mx-auto">
        <h1 className="text-3xl font-bold text-gray-900 mb-6">증상 분석</h1>
        <p className="text-gray-600 mb-6">
          현재 겪고 있는 증상을 입력하시면, 알러지 정보를 고려하여 안전한 의약품을 추천해드립니다.
        </p>

        <form onSubmit={handleSubmit} className="mb-8">
          <div className="bg-white rounded-lg shadow p-6">
            <label htmlFor="symptom" className="block text-sm font-medium text-gray-700 mb-2">
              증상 설명
            </label>
            <textarea
              id="symptom"
              rows={6}
              required
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              placeholder="예: 두통이 있고 열이 나며 기침이 있습니다."
              value={symptomText}
              onChange={(e) => setSymptomText(e.target.value)}
            />
            <button
              type="submit"
              disabled={loading}
              className="mt-4 w-full px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
            >
              {loading ? '분석 중...' : '분석하기'}
            </button>
          </div>
        </form>

        {error && (
          <div className="mb-6 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
            {error}
          </div>
        )}

        {result && (
          <div className="space-y-6">
            {result.recommendedMedications.length > 0 && (
              <div className="bg-green-50 border border-green-200 rounded-lg p-6">
                <h2 className="text-2xl font-semibold text-green-900 mb-4">
                  ✅ 추천 가능한 약
                </h2>
                <div className="space-y-4">
                  {result.recommendedMedications.map((med, index) => (
                    <div key={index} className="bg-white rounded p-4">
                      <h3 className="font-semibold text-lg text-gray-900 mb-2">{med.name}</h3>
                      <p className="text-gray-700 mb-2">{med.reason}</p>
                      {med.dosage && (
                        <p className="text-sm text-gray-600">용법: {med.dosage}</p>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )}

            {result.notRecommendedMedications.length > 0 && (
              <div className="bg-red-50 border border-red-200 rounded-lg p-6">
                <h2 className="text-2xl font-semibold text-red-900 mb-4">
                  ⚠️ 주의해야 할 약
                </h2>
                <div className="space-y-4">
                  {result.notRecommendedMedications.map((med, index) => (
                    <div key={index} className="bg-white rounded p-4">
                      <h3 className="font-semibold text-lg text-gray-900 mb-2">{med.name}</h3>
                      <p className="text-gray-700 mb-2">{med.reason}</p>
                      {med.allergicIngredients.length > 0 && (
                        <div>
                          <p className="text-sm font-medium text-red-700 mb-1">
                            알러지 성분:
                          </p>
                          <ul className="list-disc list-inside text-sm text-red-600">
                            {med.allergicIngredients.map((ing, i) => (
                              <li key={i}>{ing}</li>
                            ))}
                          </ul>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )}

            {result.precautions.length > 0 && (
              <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-6">
                <h2 className="text-2xl font-semibold text-yellow-900 mb-4">
                  📋 위험 요소 요약
                </h2>
                <ul className="space-y-2">
                  {result.precautions.map((precaution, index) => (
                    <li key={index} className="flex items-start">
                      <span className="mr-2">•</span>
                      <span className="text-gray-700">{precaution}</span>
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

