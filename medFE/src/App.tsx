import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from './store/authStore';
import Layout from './components/Layout';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import AllergiesPage from './pages/AllergiesPage';
import SymptomAnalysisPage from './pages/SymptomAnalysisPage';
import SideEffectAnalysisPage from './pages/SideEffectAnalysisPage';
import OcrAnalysisPage from './pages/OcrAnalysisPage';
import HomePage from './pages/HomePage';
import MyPage from './pages/MyPage';
import PostsListPage from './pages/PostsListPage';
import PostCreatePage from './pages/PostCreatePage';
import PostDetailPage from './pages/PostDetailPage';
import TestPage from './pages/TestPage';

function PrivateRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuthStore();
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" />;
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        {/* Public Routes - 로그인 없이 접근 가능 */}
        <Route path="/" element={<Layout />}>
          <Route index element={<HomePage />} />
          <Route path="symptom" element={<SymptomAnalysisPage />} />
          <Route path="side-effect" element={<SideEffectAnalysisPage />} />
          <Route path="ocr" element={<OcrAnalysisPage />} />
          <Route path="test" element={<TestPage />} />
        </Route>
        {/* Private Routes - 로그인 필수 */}
        <Route
          path="/"
          element={
            <PrivateRoute>
              <Layout />
            </PrivateRoute>
          }
        >
          <Route path="posts" element={<PostsListPage />} />
          <Route path="posts/create" element={<PostCreatePage />} />
          <Route path="posts/:postId" element={<PostDetailPage />} />
          <Route path="allergies" element={<AllergiesPage />} />
          <Route path="mypage" element={<MyPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;

