import { useState } from 'react';
import { analysisApi } from '../api/analysis';
import { useAuthStore } from '../store/authStore';
import type { SideEffectAnalysisResponse } from '../types/api';

export default function SideEffectAnalysisPage() {
  const { user } = useAuthStore();
  const [medicationNames, setMedicationNames] = useState<string[]>(['']);
  const [description, setDescription] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<SideEffectAnalysisResponse | null>(null);
  const [error, setError] = useState('');

  const handleMedicationChange = (index: number, value: string) => {
    const newMedications = [...medicationNames];
    newMedications[index] = value;
    setMedicationNames(newMedications);
  };

  const addMedicationField = () => {
    setMedicationNames([...medicationNames, '']);
  };

  const removeMedicationField = (index: number) => {
    if (medicationNames.length > 1) {
      setMedicationNames(medicationNames.filter((_, i) => i !== index));
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const validMedications = medicationNames.filter((name) => name.trim() !== '');
    if (validMedications.length === 0) {
      setError('최소 하나의 약물명을 입력해주세요.');
      return;
    }

    setError('');
    setLoading(true);
    setResult(null);

    try {
      const data = await analysisApi.analyzeSideEffect({
        userId: user?.id || 0, // 로그인하지 않은 경우 0 사용
        medicationNames: validMedications,
        description: description || undefined,
      });
      setResult(data);
    } catch (err: any) {
      setError(err.response?.data?.message || '부작용 분석에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="px-4 py-8">
      <div className="max-w-4xl mx-auto">
        <h1 className="text-3xl font-bold text-gray-900 mb-6">부작용 분석</h1>
        <p className="text-gray-600 mb-6">
          이전에 복용했을 때 부작용이 있었던 약물들을 입력하시면, 공통 성분과 위험 패턴을 분석해드립니다.
        </p>

        <form onSubmit={handleSubmit} className="mb-8">
          <div className="bg-white rounded-lg shadow p-6 space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                부작용이 있었던 약물명
              </label>
              {medicationNames.map((med, index) => (
                <div key={index} className="flex gap-2 mb-2">
                  <input
                    type="text"
                    className="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    placeholder="약물명을 입력하세요"
                    value={med}
                    onChange={(e) => handleMedicationChange(index, e.target.value)}
                  />
                  {medicationNames.length > 1 && (
                    <button
                      type="button"
                      onClick={() => removeMedicationField(index)}
                      className="px-3 py-2 text-red-600 hover:text-red-800"
                    >
                      삭제
                    </button>
                  )}
                </div>
              ))}
              <button
                type="button"
                onClick={addMedicationField}
                className="mt-2 text-sm text-blue-600 hover:text-blue-800"
              >
                + 약물 추가
              </button>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                부작용 설명 (선택사항)
              </label>
              <textarea
                rows={4}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                placeholder="경험한 부작용에 대해 설명해주세요"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
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
            {result.summary && (
              <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
                <h2 className="text-2xl font-semibold text-blue-900 mb-4">📊 분석 요약</h2>
                <p className="text-gray-700 whitespace-pre-line">{result.summary}</p>
              </div>
            )}

            {result.userSensitiveIngredients.length > 0 && (
              <div className="bg-red-50 border border-red-200 rounded-lg p-6">
                <h2 className="text-2xl font-semibold text-red-900 mb-4">
                  ⚠️ 당신이 민감할 가능성이 높은 성분
                </h2>
                <div className="space-y-4">
                  {result.userSensitiveIngredients.map((ingredient, index) => (
                    <div key={index} className="bg-white rounded p-4">
                      <h3 className="font-semibold text-lg text-gray-900 mb-2">
                        {ingredient.ingredientName}
                      </h3>
                      <p className="text-gray-700 mb-2">{ingredient.reason}</p>
                      <span className="inline-block px-2 py-1 text-xs font-medium rounded bg-red-100 text-red-800">
                        심각도: {ingredient.severity}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {result.commonSideEffectIngredients.length > 0 && (
              <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-6">
                <h2 className="text-2xl font-semibold text-yellow-900 mb-4">
                  💊 다른 사용자에게도 부작용이 많은 성분
                </h2>
                <div className="space-y-4">
                  {result.commonSideEffectIngredients.map((ingredient, index) => (
                    <div key={index} className="bg-white rounded p-4">
                      <h3 className="font-semibold text-lg text-gray-900 mb-2">
                        {ingredient.ingredientName}
                      </h3>
                      <p className="text-gray-700 mb-2">{ingredient.sideEffectDescription}</p>
                      <span className="inline-block px-2 py-1 text-xs font-medium rounded bg-yellow-100 text-yellow-800">
                        빈도: {ingredient.frequency}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {result.commonIngredients.length > 0 && (
              <div className="bg-gray-50 border border-gray-200 rounded-lg p-6">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">🔍 공통 성분</h2>
                <div className="flex flex-wrap gap-2">
                  {result.commonIngredients.map((ingredient, index) => (
                    <span
                      key={index}
                      className="px-3 py-1 bg-white border border-gray-300 rounded-full text-sm text-gray-700"
                    >
                      {ingredient}
                    </span>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

