import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { usersApi } from '../api/users';
import { useAuthStore } from '../store/authStore';
import type { UserAllergy } from '../types/api';

export default function AllergiesPage() {
  const { user, isAuthenticated } = useAuthStore();
  const [allergies, setAllergies] = useState<UserAllergy[]>([]);
  const [loading, setLoading] = useState(true);
  const [showAddForm, setShowAddForm] = useState(false);
  const [formData, setFormData] = useState({
    ingredientName: '',
    description: '',
    severity: 'MODERATE' as 'MILD' | 'MODERATE' | 'SEVERE',
  });
  const [error, setError] = useState('');

  useEffect(() => {
    if (user) {
      loadAllergies();
    } else {
      setLoading(false);
    }
  }, [user]);

  const loadAllergies = async () => {
    if (!user) return;
    try {
      setLoading(true);
      const data = await usersApi.getAllergies(user.id);
      setAllergies(data);
    } catch (err: any) {
      setError('알러지 정보를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleAdd = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;

    setError('');
    try {
      await usersApi.addAllergy(user.id, formData);
      setFormData({ ingredientName: '', description: '', severity: 'MODERATE' });
      setShowAddForm(false);
      loadAllergies();
    } catch (err: any) {
      setError(err.response?.data?.message || '알러지 추가에 실패했습니다.');
    }
  };

  const handleDelete = async (allergyId: number) => {
    if (!user) return;
    if (!confirm('정말 삭제하시겠습니까?')) return;

    try {
      await usersApi.deleteAllergy(user.id, allergyId);
      loadAllergies();
    } catch (err: any) {
      setError('알러지 삭제에 실패했습니다.');
    }
  };

  const severityColors = {
    MILD: 'bg-yellow-100 text-yellow-800',
    MODERATE: 'bg-orange-100 text-orange-800',
    SEVERE: 'bg-red-100 text-red-800',
  };

  const severityLabels = {
    MILD: '경미',
    MODERATE: '보통',
    SEVERE: '심각',
  };

  // 로그인하지 않은 경우 안내 메시지 표시
  if (!isAuthenticated) {
    return (
      <div className="px-4 py-8">
        <div className="max-w-4xl mx-auto">
          <h1 className="text-3xl font-bold text-gray-900 mb-6">알러지 관리</h1>
          <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-8 text-center">
            <div className="text-4xl mb-4">🔒</div>
            <h2 className="text-2xl font-semibold text-gray-900 mb-2">
              로그인 후 사용 가능합니다
            </h2>
            <p className="text-gray-600 mb-6">
              알러지 관리를 사용하려면 로그인이 필요합니다.
            </p>
            <Link
              to="/login"
              className="inline-block px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              로그인하기
            </Link>
          </div>
        </div>
      </div>
    );
  }

  if (loading) {
    return <div className="text-center py-8">로딩 중...</div>;
  }

  return (
    <div className="px-4 py-8">
      <div className="max-w-4xl mx-auto">
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-3xl font-bold text-gray-900">알러지 관리</h1>
          <button
            onClick={() => setShowAddForm(!showAddForm)}
            className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
          >
            {showAddForm ? '취소' : '+ 알러지 추가'}
          </button>
        </div>

        {error && (
          <div className="mb-4 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
            {error}
          </div>
        )}

        {showAddForm && (
          <div className="mb-6 bg-white p-6 rounded-lg shadow">
            <h2 className="text-xl font-semibold mb-4">새 알러지 추가</h2>
            <form onSubmit={handleAdd} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  성분명 *
                </label>
                <input
                  type="text"
                  required
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  value={formData.ingredientName}
                  onChange={(e) => setFormData({ ...formData, ingredientName: e.target.value })}
                  placeholder="예: 아세트아미노펜"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  설명
                </label>
                <textarea
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  rows={3}
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  placeholder="알러지에 대한 추가 설명을 입력하세요"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  심각도
                </label>
                <select
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  value={formData.severity}
                  onChange={(e) =>
                    setFormData({ ...formData, severity: e.target.value as any })
                  }
                >
                  <option value="MILD">경미</option>
                  <option value="MODERATE">보통</option>
                  <option value="SEVERE">심각</option>
                </select>
              </div>
              <button
                type="submit"
                className="w-full px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
              >
                추가
              </button>
            </form>
          </div>
        )}

        <div className="bg-white rounded-lg shadow">
          {allergies.length === 0 ? (
            <div className="p-8 text-center text-gray-500">
              등록된 알러지가 없습니다. 알러지를 추가하여 안전한 약물 선택에 도움을 받으세요.
            </div>
          ) : (
            <div className="divide-y divide-gray-200">
              {allergies.map((allergy) => (
                <div key={allergy.id} className="p-6 hover:bg-gray-50">
                  <div className="flex justify-between items-start">
                    <div className="flex-1">
                      <div className="flex items-center gap-3 mb-2">
                        <h3 className="text-lg font-semibold text-gray-900">
                          {allergy.ingredientName}
                        </h3>
                        <span
                          className={`px-2 py-1 text-xs font-medium rounded ${severityColors[allergy.severity]}`}
                        >
                          {severityLabels[allergy.severity]}
                        </span>
                      </div>
                      {allergy.description && (
                        <p className="text-gray-600 mb-2">{allergy.description}</p>
                      )}
                      <p className="text-sm text-gray-500">
                        등록일: {new Date(allergy.createdAt).toLocaleDateString('ko-KR')}
                      </p>
                    </div>
                    <button
                      onClick={() => handleDelete(allergy.id)}
                      className="ml-4 text-red-600 hover:text-red-800"
                    >
                      삭제
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

