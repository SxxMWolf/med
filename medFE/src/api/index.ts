import axios, { AxiosInstance, InternalAxiosRequestConfig } from 'axios';

// Vite 구조에 맞는 간단한 API 클라이언트
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

// 디버깅: baseURL 확인
if (typeof window !== 'undefined') {
  console.log('🔍 API 설정 확인:');
  console.log('  - VITE_API_BASE_URL:', import.meta.env.VITE_API_BASE_URL || '(설정되지 않음)');
  console.log('  - 사용할 baseURL:', API_BASE_URL || '(빈 값 - 상대 경로 사용)');
  
  if (!API_BASE_URL && import.meta.env.PROD) {
    console.error('❌ 프로덕션 환경에서 VITE_API_BASE_URL이 설정되지 않았습니다!');
    console.error('   Vercel 대시보드에서 환경 변수를 설정하세요:');
    console.error('   Key: VITE_API_BASE_URL');
    console.error('   Value: http://16.184.46.179:8080');
  }
}

const api: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: false,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 요청 인터셉터: JWT 토큰 자동 추가
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('accessToken');
    const fullURL = config.baseURL 
      ? `${config.baseURL}${config.url}` 
      : config.url || '';
    
    console.log('API 요청:', {
      url: config.url,
      method: config.method,
      baseURL: config.baseURL || '(상대 경로)',
      fullURL: fullURL,
      hasToken: !!token,
    });
    
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 응답 인터셉터: 토큰 만료 처리
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // 401 (Unauthorized) 처리
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      localStorage.removeItem('accessToken');
      localStorage.removeItem('user');
      window.location.href = '/login';
      return Promise.reject(error);
    }

    return Promise.reject(error);
  }
);

export default api;

