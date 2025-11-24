import { Outlet, Link, useLocation } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';

export default function Layout() {
  const { user, clearAuth } = useAuthStore();
  const location = useLocation();

  const handleLogout = () => {
    clearAuth();
    window.location.href = '/login';
  };

  const navItems = [
    { path: '/symptom', label: '증상 분석', public: true },
    { path: '/side-effect', label: '부작용 분석', public: true },
    { path: '/ocr', label: '성분표 분석', public: true },
    { path: '/posts', label: '커뮤니티', public: false },
    { path: '/allergies', label: '알러지 관리', public: false },
  ];

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-white shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex">
              <div className="flex-shrink-0 flex items-center">
                <Link to="/" className="text-xl font-bold text-blue-600 hover:text-blue-800 cursor-pointer">
                  MED
                </Link>
              </div>
              <div className="hidden sm:ml-6 sm:flex sm:space-x-8">
                {navItems
                  .filter((item) => item.public || user)
                  .map((item) => (
                    <Link
                      key={item.path}
                      to={item.path}
                      className={`inline-flex items-center px-1 pt-1 border-b-2 text-sm font-medium ${
                        location.pathname === item.path
                          ? 'border-blue-500 text-gray-900'
                          : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700'
                      }`}
                    >
                      {item.label}
                    </Link>
                  ))}
              </div>
            </div>
            <div className="flex items-center">
              {user ? (
                <>
                  <Link
                    to="/mypage"
                    className="text-sm text-gray-700 mr-4 underline hover:text-gray-900 cursor-pointer"
                  >
                    {user.nickname}님
                  </Link>
                  <button
                    onClick={handleLogout}
                    className="text-sm text-gray-500 hover:text-gray-700"
                  >
                    로그아웃
                  </button>
                </>
              ) : (
                <>
                  <Link
                    to="/login"
                    className="text-sm text-gray-700 mr-4 hover:text-gray-900"
                  >
                    로그인
                  </Link>
                  <Link
                    to="/register"
                    className="text-sm text-blue-600 hover:text-blue-800"
                  >
                    회원가입
                  </Link>
                </>
              )}
            </div>
          </div>
        </div>
      </nav>

      <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        <Outlet />
      </main>
    </div>
  );
}

