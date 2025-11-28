import { useState, useEffect } from 'react';
import { authApi } from '../api/auth';
import { usersApi } from '../api/users';
import { useAuthStore } from '../store/authStore';
import type { UserResponse, UserAllergy } from '../types/api';

export default function MyPage() {
  const { user, setAuth } = useAuthStore();
  const [userInfo, setUserInfo] = useState<UserResponse | null>(null);
  const [allergies, setAllergies] = useState<UserAllergy[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'info' | 'allergies'>('info');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // 닉네임 변경 관련
  const [showNicknameForm, setShowNicknameForm] = useState(false);
  const [newNickname, setNewNickname] = useState('');

  // 비밀번호 변경 관련
  const [showPasswordForm, setShowPasswordForm] = useState(false);
  const [passwordData, setPasswordData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });

  // 알러지 추가 관련
  const [showAllergyForm, setShowAllergyForm] = useState(false);
  const [allergyFormData, setAllergyFormData] = useState({
    ingredientName: '',
    description: '',
    severity: 'MODERATE' as 'MILD' | 'MODERATE' | 'SEVERE',
  });

  useEffect(() => {
    if (user) {
      loadUserInfo();
      loadAllergies();
    } else {
      setError('로그인이 필요합니다.');
      setLoading(false);
    }
  }, [user]);

  const loadUserInfo = async () => {
    try {
      setLoading(true);
      setError('');
      
      // 토큰 확인
      const token = localStorage.getItem('accessToken');
      console.log('토큰 확인:', token ? '토큰 존재' : '토큰 없음');
      
      if (!token) {
        setError('로그인이 필요합니다.');
        setLoading(false);
        return;
      }
      
      console.log('API 호출 시작: /api/auth/me');
      const data = await authApi.getCurrentUser();
      console.log('API 응답 성공:', data);
      setUserInfo(data);
      setNewNickname(data.nickname);
    } catch (err: any) {
      console.error('사용자 정보 로딩 실패:', {
        status: err.response?.status,
        statusText: err.response?.statusText,
        data: err.response?.data,
        message: err.message,
        config: {
          url: err.config?.url,
          method: err.config?.method,
          headers: err.config?.headers,
        },
      });
      
      // 401 에러인 경우에만 로그인 페이지로 리다이렉트
      if (err.response?.status === 401) {
        setError('인증이 만료되었습니다. 로그인 페이지로 이동합니다.');
        setTimeout(() => {
          localStorage.removeItem('accessToken');
          localStorage.removeItem('user');
          window.location.href = '/login';
        }, 2000);
      } else if (err.response?.status === 403) {
        // 403 에러는 권한 문제이므로 더 자세한 정보 제공
        const errorMessage = err.response?.data?.message || '권한이 없습니다. 로그인 상태를 확인해주세요.';
        setError(errorMessage);
        console.error('403 에러 상세:', {
          token: localStorage.getItem('accessToken')?.substring(0, 20) + '...',
          user: localStorage.getItem('user'),
        });
      } else {
        const errorMessage = err.response?.data?.message || err.message || '사용자 정보를 불러오는데 실패했습니다.';
        setError(errorMessage);
      }
    } finally {
      setLoading(false);
    }
  };

  const loadAllergies = async () => {
    if (!user) return;
    try {
      const data = await usersApi.getAllergies(user.id);
      setAllergies(data);
    } catch (err: any) {
      // 알러지 로딩 실패는 별도로 처리하지 않고 조용히 실패
      console.error('알러지 정보 로딩 실패:', err);
      setAllergies([]);
    }
  };

  const handleNicknameChange = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (!newNickname.trim() || newNickname.length < 2 || newNickname.length > 20) {
      setError('닉네임은 2-20자 사이여야 합니다.');
      return;
    }

    try {
      await authApi.changeNickname({ nickname: newNickname });
      setSuccess('닉네임이 변경되었습니다.');
      setShowNicknameForm(false);
      await loadUserInfo();
      // authStore 업데이트
      if (user) {
        setAuth(localStorage.getItem('accessToken') || '', {
          ...user,
          nickname: newNickname,
        });
      }
    } catch (err: any) {
      setError(err.response?.data?.message || '닉네임 변경에 실패했습니다.');
    }
  };

  const handlePasswordChange = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (passwordData.newPassword.length < 6) {
      setError('새 비밀번호는 6자 이상이어야 합니다.');
      return;
    }

    if (passwordData.newPassword !== passwordData.confirmPassword) {
      setError('새 비밀번호와 확인 비밀번호가 일치하지 않습니다.');
      return;
    }

    try {
      await authApi.changePassword({
        currentPassword: passwordData.currentPassword,
        newPassword: passwordData.newPassword,
      });
      setSuccess('비밀번호가 변경되었습니다.');
      setShowPasswordForm(false);
      setPasswordData({
        currentPassword: '',
        newPassword: '',
        confirmPassword: '',
      });
    } catch (err: any) {
      setError(err.response?.data?.message || '비밀번호 변경에 실패했습니다.');
    }
  };

  const handleAddAllergy = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;

    setError('');
    try {
      await usersApi.addAllergy(user.id, allergyFormData);
      setSuccess('알러지가 추가되었습니다.');
      setAllergyFormData({ ingredientName: '', description: '', severity: 'MODERATE' });
      setShowAllergyForm(false);
      loadAllergies();
    } catch (err: any) {
      setError(err.response?.data?.message || '알러지 추가에 실패했습니다.');
    }
  };

  const handleDeleteAllergy = async (allergyId: number) => {
    if (!user) return;
    if (!confirm('정말 삭제하시겠습니까?')) return;

    try {
      await usersApi.deleteAllergy(user.id, allergyId);
      setSuccess('알러지가 삭제되었습니다.');
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

  if (loading) {
    return <div className="text-center py-8">로딩 중...</div>;
  }

  if (!userInfo && error) {
    return (
      <div className="px-4 py-8">
        <div className="max-w-4xl mx-auto">
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4">
            {error}
          </div>
          <button
            onClick={() => {
              setError('');
              loadUserInfo();
            }}
            className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
          >
            다시 시도
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="px-4 py-8">
      <div className="max-w-4xl mx-auto">
        <h1 className="text-3xl font-bold text-gray-900 mb-6">마이페이지</h1>

        {/* 탭 메뉴 */}
        <div className="border-b border-gray-200 mb-6">
          <nav className="-mb-px flex space-x-8">
            <button
              onClick={() => setActiveTab('info')}
              className={`py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'info'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              내 정보
            </button>
            <button
              onClick={() => setActiveTab('allergies')}
              className={`py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'allergies'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              알러지 관리
            </button>
          </nav>
        </div>

        {/* 메시지 표시 */}
        {error && (
          <div className="mb-4 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
            {error}
          </div>
        )}
        {success && (
          <div className="mb-4 bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded">
            {success}
          </div>
        )}

        {/* 내 정보 탭 */}
        {activeTab === 'info' && userInfo && (
          <div className="bg-white rounded-lg shadow p-6 space-y-6">
            <div>
              <h2 className="text-2xl font-semibold text-gray-900 mb-4">기본 정보</h2>
              <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <dt className="text-sm font-medium text-gray-500">아이디</dt>
                  <dd className="mt-1 text-sm text-gray-900">{userInfo.username}</dd>
                </div>
                <div>
                  <dt className="text-sm font-medium text-gray-500">이메일</dt>
                  <dd className="mt-1 text-sm text-gray-900">{userInfo.email}</dd>
                </div>
                <div>
                  <dt className="text-sm font-medium text-gray-500">닉네임</dt>
                  <dd className="mt-1 text-sm text-gray-900 flex items-center gap-2">
                    {userInfo.nickname}
                    <button
                      onClick={() => setShowNicknameForm(!showNicknameForm)}
                      className="text-xs text-blue-600 hover:text-blue-800"
                    >
                      변경
                    </button>
                  </dd>
                </div>
                <div>
                  <dt className="text-sm font-medium text-gray-500">가입일</dt>
                  <dd className="mt-1 text-sm text-gray-900">
                    {new Date(userInfo.createdAt).toLocaleDateString('ko-KR')}
                  </dd>
                </div>
              </dl>
            </div>

            {/* 닉네임 변경 폼 */}
            {showNicknameForm && (
              <div className="border-t pt-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">닉네임 변경</h3>
                <form onSubmit={handleNicknameChange} className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      새 닉네임 (2-20자)
                    </label>
                    <input
                      type="text"
                      required
                      minLength={2}
                      maxLength={20}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                      value={newNickname}
                      onChange={(e) => setNewNickname(e.target.value)}
                    />
                  </div>
                  <div className="flex gap-2">
                    <button
                      type="submit"
                      className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
                    >
                      변경
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setShowNicknameForm(false);
                        setNewNickname(userInfo.nickname);
                      }}
                      className="px-4 py-2 bg-gray-200 text-gray-700 rounded-md hover:bg-gray-300"
                    >
                      취소
                    </button>
                  </div>
                </form>
              </div>
            )}

            {/* 비밀번호 변경 */}
            <div className="border-t pt-6">
              <div className="flex justify-between items-center mb-4">
                <h3 className="text-lg font-semibold text-gray-900">비밀번호 변경</h3>
                <button
                  onClick={() => setShowPasswordForm(!showPasswordForm)}
                  className="text-sm text-blue-600 hover:text-blue-800"
                >
                  {showPasswordForm ? '취소' : '변경하기'}
                </button>
              </div>
              {showPasswordForm && (
                <form onSubmit={handlePasswordChange} className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      현재 비밀번호
                    </label>
                    <input
                      type="password"
                      required
                      className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                      value={passwordData.currentPassword}
                      onChange={(e) =>
                        setPasswordData({ ...passwordData, currentPassword: e.target.value })
                      }
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      새 비밀번호 (6자 이상)
                    </label>
                    <input
                      type="password"
                      required
                      minLength={6}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                      value={passwordData.newPassword}
                      onChange={(e) =>
                        setPasswordData({ ...passwordData, newPassword: e.target.value })
                      }
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      새 비밀번호 확인
                    </label>
                    <input
                      type="password"
                      required
                      minLength={6}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                      value={passwordData.confirmPassword}
                      onChange={(e) =>
                        setPasswordData({ ...passwordData, confirmPassword: e.target.value })
                      }
                    />
                  </div>
                  <button
                    type="submit"
                    className="w-full px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
                  >
                    비밀번호 변경
                  </button>
                </form>
              )}
            </div>
          </div>
        )}

        {/* 알러지 관리 탭 */}
        {activeTab === 'allergies' && (
          <div className="space-y-6">
            <div className="flex justify-between items-center">
              <h2 className="text-2xl font-semibold text-gray-900">알러지 관리</h2>
              <button
                onClick={() => setShowAllergyForm(!showAllergyForm)}
                className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
              >
                {showAllergyForm ? '취소' : '+ 알러지 추가'}
              </button>
            </div>

            {showAllergyForm && (
              <div className="bg-white p-6 rounded-lg shadow">
                <h3 className="text-xl font-semibold mb-4">새 알러지 추가</h3>
                <form onSubmit={handleAddAllergy} className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      성분명 *
                    </label>
                    <input
                      type="text"
                      required
                      className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                      value={allergyFormData.ingredientName}
                      onChange={(e) =>
                        setAllergyFormData({ ...allergyFormData, ingredientName: e.target.value })
                      }
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
                      value={allergyFormData.description}
                      onChange={(e) =>
                        setAllergyFormData({ ...allergyFormData, description: e.target.value })
                      }
                      placeholder="알러지에 대한 추가 설명을 입력하세요"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      심각도
                    </label>
                    <select
                      className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                      value={allergyFormData.severity}
                      onChange={(e) =>
                        setAllergyFormData({
                          ...allergyFormData,
                          severity: e.target.value as any,
                        })
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
                          onClick={() => handleDeleteAllergy(allergy.id)}
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
        )}
      </div>
    </div>
  );
}

