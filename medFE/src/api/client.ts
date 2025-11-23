import axios, { AxiosInstance, InternalAxiosRequestConfig } from 'axios';

// 개발 환경에서는 Vite 프록시를 사용하므로 baseURL을 빈 문자열로 설정
// 프로덕션에서는 환경 변수로 설정된 URL 사용
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

class ApiClient {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: API_BASE_URL,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // 요청 인터셉터: JWT 토큰 자동 추가
    this.client.interceptors.request.use(
      (config: InternalAxiosRequestConfig) => {
        const token = localStorage.getItem('accessToken');
        if (token && config.headers) {
          // Bearer 토큰 형식으로 설정
          config.headers.Authorization = `Bearer ${token}`;
          console.log('요청 전송:', {
            url: config.url,
            method: config.method,
            baseURL: config.baseURL,
            fullURL: `${config.baseURL}${config.url}`,
            hasToken: true,
            tokenPrefix: token.substring(0, 20) + '...',
            authHeader: config.headers.Authorization?.substring(0, 30) + '...',
          });
        } else {
          console.warn('No access token found in localStorage', {
            url: config.url,
            method: config.method,
          });
        }
        return config;
      },
      (error) => {
        return Promise.reject(error);
      }
    );

    // 응답 인터셉터: 토큰 만료 처리
    this.client.interceptors.response.use(
      (response) => response,
      async (error) => {
        const originalRequest = error.config;

        // 401 (Unauthorized)만 자동 처리, 403은 각 컴포넌트에서 처리
        if (error.response?.status === 401 && !originalRequest._retry) {
          originalRequest._retry = true;
          // 토큰 만료 시 로그아웃 처리
          localStorage.removeItem('accessToken');
          localStorage.removeItem('user');
          window.location.href = '/login';
          return Promise.reject(error);
        }

        // 403 에러는 로깅만 하고 각 컴포넌트에서 처리하도록 함
        if (error.response?.status === 403) {
          console.error('403 Forbidden 상세:', {
            url: error.config?.url,
            method: error.config?.method,
            hasToken: !!localStorage.getItem('accessToken'),
            responseHeaders: error.response?.headers,
            responseData: error.response?.data,
            requestHeaders: error.config?.headers,
          });
        }

        return Promise.reject(error);
      }
    );
  }

  getInstance(): AxiosInstance {
    return this.client;
  }
}

export const apiClient = new ApiClient().getInstance();

