import { useState } from 'react';
import { analysisApi } from '../api/analysis';
import { useAuthStore } from '../store/authStore';
import type { SideEffectAnalysisResponse, GroupRequest } from '../types/api';

interface Group {
  id: string;
  type: 'food' | 'drug';
  items: string[];
}

export default function SideEffectAnalysisPage() {
  const { user } = useAuthStore();
  const [groups, setGroups] = useState<Group[]>([]);
  const [currentInput, setCurrentInput] = useState('');
  const [currentGroupType, setCurrentGroupType] = useState<'food' | 'drug'>('food');
  const [description, setDescription] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<SideEffectAnalysisResponse | null>(null);
  const [error, setError] = useState('');

  const addGroup = () => {
    const trimmed = currentInput.trim();
    if (trimmed) {
      // 공백으로 구분하여 여러 항목 추가
      const items = trimmed.split(/\s+/).filter(item => item.length > 0);
      
      if (items.length > 0) {
        const newGroup: Group = {
          id: Date.now().toString(),
          type: currentGroupType,
          items: items,
        };
        setGroups([...groups, newGroup]);
        setCurrentInput('');
      }
    }
  };

  const removeGroup = (groupId: string) => {
    setGroups(groups.filter(g => g.id !== groupId));
  };

  const removeItemFromGroup = (groupId: string, itemIndex: number) => {
    setGroups(groups.map(group => {
      if (group.id === groupId) {
        const newItems = group.items.filter((_, i) => i !== itemIndex);
        if (newItems.length === 0) {
          return null; // 항목이 없으면 그룹 삭제
        }
        return { ...group, items: newItems };
      }
      return group;
    }).filter((g): g is Group => g !== null));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (groups.length === 0) {
      setError('최소 하나의 그룹을 입력해주세요.');
      return;
    }

    // 모든 그룹에 항목이 있는지 확인
    const hasEmptyGroup = groups.some(group => group.items.length === 0);
    if (hasEmptyGroup) {
      setError('모든 그룹에 최소 하나의 항목이 있어야 합니다.');
      return;
    }

    setError('');
    setLoading(true);
    setResult(null);

    try {
      // 그룹 구조를 API 형식으로 변환
      const groupRequests: GroupRequest[] = groups.map(group => ({
        type: group.type,
        items: group.items,
      }));

      const data = await analysisApi.analyzeSideEffect({
        userId: user?.id || undefined,
        groups: groupRequests,
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
          이전에 복용하거나 섭취했을 때 부작용이 있었던 항목을 그룹 단위로 입력하시면, 공통 성분과 위험 패턴을 분석해드립니다.
          <br />
          <span className="text-sm text-gray-500">
            💡 각 그룹은 식품 또는 의약품으로 구분되며, 그룹 내 항목들의 성분이 합집합으로 처리됩니다. 여러 그룹 간의 교집합으로 공통 성분을 찾습니다.
          </span>
        </p>

        <form onSubmit={handleSubmit} className="mb-8">
          <div className="bg-white rounded-lg shadow p-6 space-y-6">
            {/* 그룹 추가 섹션 */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                그룹 추가
              </label>
              <p className="text-xs text-gray-500 mb-3">
                💡 그룹 타입을 선택하고 항목을 입력한 후 "그룹 추가" 버튼을 클릭하세요. 공백으로 여러 항목을 한 번에 추가할 수 있습니다.
              </p>
              <div className="flex gap-2 mb-3">
                <select
                  value={currentGroupType}
                  onChange={(e) => setCurrentGroupType(e.target.value as 'food' | 'drug')}
                  className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                >
                  <option value="food">🥗 식품</option>
                  <option value="drug">💊 의약품</option>
                </select>
                <input
                  type="text"
                  className="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  placeholder="항목을 입력하세요 (공백으로 여러 항목 구분 가능)"
                  value={currentInput}
                  onChange={(e) => setCurrentInput(e.target.value)}
                  onKeyPress={(e) => {
                    if (e.key === 'Enter') {
                      e.preventDefault();
                      addGroup();
                    }
                  }}
                />
                <button
                  type="button"
                  onClick={addGroup}
                  className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
                >
                  그룹 추가
                </button>
              </div>
            </div>

            {/* 그룹 목록 표시 */}
            {groups.length > 0 && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  입력된 그룹 목록
                </label>
                <div className="space-y-3">
                  {groups.map((group, groupIndex) => (
                    <div
                      key={group.id}
                      className={`p-4 border-2 rounded-lg ${
                        group.type === 'food'
                          ? 'bg-orange-50 border-orange-200'
                          : 'bg-blue-50 border-blue-200'
                      }`}
                    >
                      <div className="flex items-center justify-between mb-2">
                        <div className="flex items-center gap-2">
                          <span className="font-semibold text-gray-900">
                            그룹 {groupIndex + 1}
                          </span>
                          <span
                            className={`px-2 py-1 text-xs font-medium rounded ${
                              group.type === 'food'
                                ? 'bg-orange-100 text-orange-800'
                                : 'bg-blue-100 text-blue-800'
                            }`}
                          >
                            {group.type === 'food' ? '🥗 식품' : '💊 의약품'}
                          </span>
                        </div>
                        <button
                          type="button"
                          onClick={() => removeGroup(group.id)}
                          className="text-red-600 hover:text-red-800 font-bold"
                        >
                          그룹 삭제
                        </button>
                      </div>
                      <div className="flex flex-wrap gap-2">
                        {group.items.map((item, itemIndex) => (
                          <span
                            key={itemIndex}
                            className="inline-flex items-center gap-1 px-3 py-1 bg-white border border-gray-300 rounded-full text-sm text-gray-700"
                          >
                            {item}
                            <button
                              type="button"
                              onClick={() => removeItemFromGroup(group.id, itemIndex)}
                              className="text-gray-600 hover:text-gray-800 font-bold"
                            >
                              ×
                            </button>
                          </span>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* 부작용 설명 */}
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
              disabled={loading || groups.length === 0}
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
            {/* 공통 성분 섹션 - 가장 위에 배치 */}
            <div className="bg-gray-50 border border-gray-200 rounded-lg p-6">
              <h2 className="text-2xl font-semibold text-gray-900 mb-4">🔍 공통 성분</h2>
              {result.commonIngredients.length > 0 ? (
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
              ) : (
                <p className="text-gray-600 italic">모든 그룹에 공통으로 포함된 성분이 없습니다.</p>
              )}
            </div>

            {/* 사용자 민감 가능 성분 섹션 */}
            {result.userSensitiveIngredients.length > 0 && (
              <div className="bg-red-50 border border-red-200 rounded-lg p-6">
                <h2 className="text-2xl font-semibold text-red-900 mb-2">
                  ⚠️ 당신이 민감할 가능성이 높은 성분
                </h2>
                <p className="text-sm text-gray-600 mb-4">
                  이 성분들은 사용자의 알러지 정보(약물/식품 알러지)를 참고하여 분석된 결과입니다.
                </p>
                <div className="space-y-4">
                  {result.userSensitiveIngredients.map((ingredient, index) => (
                    <div key={index} className="bg-white rounded p-4">
                      <div className="flex items-center gap-2 mb-2">
                        <h3 className="font-semibold text-lg text-gray-900">
                          {ingredient.ingredientName}
                        </h3>
                        {ingredient.isFoodOrigin && (
                          <span className="px-2 py-1 text-xs font-medium rounded bg-orange-100 text-orange-800">
                            식품 유래
                          </span>
                        )}
                        {ingredient.foodAllergyMatch && (
                          <span className="px-2 py-1 text-xs font-medium rounded bg-red-100 text-red-800">
                            식품 알러지 매칭
                          </span>
                        )}
                      </div>
                      <p className="text-gray-700 mb-2">{ingredient.reason}</p>
                      <span className="inline-block px-2 py-1 text-xs font-medium rounded bg-red-100 text-red-800">
                        심각도: {ingredient.severity}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* 분석 요약 섹션 */}
            {result.summary && (
              <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
                <h2 className="text-2xl font-semibold text-blue-900 mb-4">📊 분석 요약</h2>
                <p className="text-gray-700 whitespace-pre-line">{result.summary}</p>
              </div>
            )}

            {/* 다른 사용자에게도 부작용이 많은 성분 섹션 */}
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

            {/* 식품 알러지 분석 섹션 */}
            {result.foodAllergyAnalysis && (
              <div className="bg-orange-50 border border-orange-200 rounded-lg p-6">
                <h2 className="text-2xl font-semibold text-orange-900 mb-4">
                  🥜 식품 알러지 분석
                </h2>
                <div className="space-y-4">
                  {result.foodAllergyAnalysis.detectedFoodOriginIngredients.length > 0 && (
                    <div>
                      <p className="text-sm font-semibold text-orange-900 mb-2">
                        검출된 식품 유래 성분:
                      </p>
                      <div className="flex flex-wrap gap-2">
                        {result.foodAllergyAnalysis.detectedFoodOriginIngredients.map((ingredient, index) => (
                          <span
                            key={index}
                            className="px-3 py-1 bg-white border border-orange-300 rounded-full text-sm text-orange-700"
                          >
                            {ingredient}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}
                  {result.foodAllergyAnalysis.matchedAllergens.length > 0 && (
                    <div>
                      <p className="text-sm font-semibold text-red-900 mb-2">
                        매칭된 식품 알러지:
                      </p>
                      <div className="flex flex-wrap gap-2">
                        {result.foodAllergyAnalysis.matchedAllergens.map((allergen, index) => (
                          <span
                            key={index}
                            className="px-3 py-1 bg-red-100 border border-red-300 rounded-full text-sm text-red-800"
                          >
                            {allergen}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}
                  {result.foodAllergyAnalysis.riskAssessment && (
                    <div className="mt-4 p-3 bg-white rounded border border-orange-200">
                      <p className="text-sm font-semibold text-orange-900 mb-1">위험도 평가:</p>
                      <p className="text-gray-700 text-sm">{result.foodAllergyAnalysis.riskAssessment}</p>
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
