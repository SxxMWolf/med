# Med 프론트엔드 개발기: React + TypeScript로 구현한 개인 맞춤형 복약 안전성 확인 웹 애플리케이션

## 개요

Med는 사용자의 알러지 정보와 복용 경험을 기반으로 안전한 약물을 추천하고 분석하는 웹 애플리케이션입니다. 이 글에서는 프론트엔드 개발 과정을 단계별로 정리했습니다.

## 기술 스택

- **프레임워크**: React 18.2.0
- **언어**: TypeScript 5.2.2
- **빌드 도구**: Vite 5.0.8
- **스타일링**: Tailwind CSS 3.3.6
- **상태 관리**: Zustand 4.4.7
- **HTTP 클라이언트**: Axios 1.6.2
- **라우팅**: React Router v6 6.20.0

---

## 1단계: 프로젝트 초기 설정 및 개발 환경 구성

### 1.1 Vite + React + TypeScript 프로젝트 생성

```bash
npm create vite@latest medFE -- --template react-ts
cd medFE
npm install
```

### 1.2 필수 의존성 설치

```json
{
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.20.0",
    "axios": "^1.6.2",
    "zustand": "^4.4.7"
  },
  "devDependencies": {
    "@types/react": "^18.2.43",
    "@types/react-dom": "^18.2.17",
    "typescript": "^5.2.2",
    "vite": "^5.0.8",
    "tailwindcss": "^3.3.6",
    "autoprefixer": "^10.4.16",
    "postcss": "^8.4.32"
  }
}
```

### 1.3 Tailwind CSS 설정

`tailwind.config.js`와 `postcss.config.js`를 설정하여 유틸리티 기반 CSS를 사용할 수 있도록 구성했습니다.

### 1.4 Vite 프록시 설정

개발 환경에서 CORS 문제를 해결하기 위해 Vite 프록시를 설정했습니다:

```typescript
// vite.config.ts
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      secure: false,
    },
  },
}
```

---

## 2단계: 상태 관리 시스템 구축 (Zustand)

### 2.1 인증 상태 관리 스토어 구현

Zustand를 사용하여 전역 인증 상태를 관리하는 스토어를 구현했습니다:

```typescript
// src/store/authStore.ts
interface AuthState {
  user: UserInfo | null;
  token: string | null;
  isAuthenticated: boolean;
  setAuth: (token: string, user: UserInfo) => void;
  clearAuth: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: JSON.parse(localStorage.getItem('user') || 'null'),
  token: localStorage.getItem('accessToken'),
  isAuthenticated: !!localStorage.getItem('accessToken'),
  setAuth: (token, user) => {
    localStorage.setItem('accessToken', token);
    localStorage.setItem('user', JSON.stringify(user));
    set({ token, user, isAuthenticated: true });
  },
  clearAuth: () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('user');
    set({ token: null, user: null, isAuthenticated: false });
  },
}));
```

**핵심 포인트**:
- localStorage를 활용한 토큰 및 사용자 정보 영구 저장
- 간단한 API로 전역 상태 관리
- 타입 안정성 보장

---

## 3단계: API 클라이언트 구성 및 인터셉터 구현

### 3.1 Axios 인스턴스 생성

```typescript
// src/api/client.ts
class ApiClient {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: API_BASE_URL,
      headers: {
        'Content-Type': 'application/json',
      },
    });
  }
}
```

### 3.2 요청 인터셉터: JWT 토큰 자동 주입

모든 API 요청에 JWT 토큰을 자동으로 추가하는 인터셉터를 구현했습니다:

```typescript
this.client.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('accessToken');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  }
);
```

**장점**:
- 각 API 호출마다 토큰을 수동으로 추가할 필요 없음
- 일관된 인증 헤더 관리
- 코드 중복 제거

### 3.3 응답 인터셉터: 토큰 만료 처리

401 에러 발생 시 자동으로 로그아웃 처리:

```typescript
this.client.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401 && !originalRequest._retry) {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

### 3.4 API 모듈화

기능별로 API를 모듈화하여 관리:

- `src/api/auth.ts` - 인증 관련 API
- `src/api/users.ts` - 사용자 및 알러지 관리 API
- `src/api/posts.ts` - 게시글 관리 API
- `src/api/comments.ts` - 댓글 관리 API
- `src/api/analysis.ts` - 분석 API
- `src/api/medications.ts` - 약물 검색 API
- `src/api/images.ts` - 이미지 업로드 API

---

## 4단계: 라우팅 시스템 구축

### 4.1 React Router v6 설정

```typescript
// src/App.tsx
<BrowserRouter>
  <Routes>
    <Route path="/login" element={<LoginPage />} />
    <Route path="/register" element={<RegisterPage />} />
    <Route
      path="/"
      element={
        <PrivateRoute>
          <Layout />
        </PrivateRoute>
      }
    >
      <Route index element={<HomePage />} />
      <Route path="posts" element={<PostsListPage />} />
      <Route path="posts/create" element={<PostCreatePage />} />
      <Route path="posts/:postId" element={<PostDetailPage />} />
      <Route path="allergies" element={<AllergiesPage />} />
      <Route path="symptom" element={<SymptomAnalysisPage />} />
      <Route path="side-effect" element={<SideEffectAnalysisPage />} />
      <Route path="ocr" element={<OcrAnalysisPage />} />
      <Route path="mypage" element={<MyPage />} />
    </Route>
  </Routes>
</BrowserRouter>
```

### 4.2 PrivateRoute 컴포넌트

인증이 필요한 페이지를 보호하는 컴포넌트:

```typescript
function PrivateRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuthStore();
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" />;
}
```

---

## 5단계: 레이아웃 컴포넌트 구현

### 5.1 공통 레이아웃 구조

모든 페이지에서 공통으로 사용되는 네비게이션 바와 레이아웃을 구현:

```typescript
// src/components/Layout.tsx
export default function Layout() {
  const { user, clearAuth } = useAuthStore();
  const location = useLocation();

  const navItems = [
    { path: '/posts', label: '커뮤니티' },
    { path: '/allergies', label: '알러지 관리' },
    { path: '/symptom', label: '증상 분석' },
    { path: '/side-effect', label: '부작용 분석' },
    { path: '/ocr', label: '성분표 분석' },
  ];

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-white shadow-sm">
        {/* 네비게이션 바 */}
      </nav>
      <main>
        <Outlet /> {/* 하위 라우트 렌더링 */}
      </main>
    </div>
  );
}
```

**구현 특징**:
- MED 로고 클릭 시 홈으로 이동
- 현재 경로에 따른 활성 상태 표시
- 사용자 정보 및 로그아웃 기능

---

## 6단계: 인증 시스템 구현

### 6.1 로그인 페이지

```typescript
// src/pages/LoginPage.tsx
const handleSubmit = async (e: React.FormEvent) => {
  e.preventDefault();
  try {
    const response = await authApi.login(formData);
    setAuth(response.accessToken, response.user);
    navigate('/');
  } catch (err: any) {
    setError(err.response?.data?.message || '로그인에 실패했습니다.');
  }
};
```

### 6.2 회원가입 페이지

유효성 검사와 함께 사용자 등록 기능 구현.

---

## 7단계: 알러지 관리 기능 구현

### 7.1 알러지 목록 조회 및 표시

```typescript
// src/pages/AllergiesPage.tsx
const loadAllergies = async () => {
  if (!user) return;
  try {
    const data = await usersApi.getAllergies(user.id);
    setAllergies(data);
  } catch (err: any) {
    setError('알러지 정보를 불러오는데 실패했습니다.');
  }
};
```

### 7.2 알러지 추가/삭제

- 심각도 설정 (경미/보통/심각)
- 성분명 및 설명 입력
- CRUD 기능 완전 구현

---

## 8단계: 분석 기능 구현

### 8.1 증상 분석 페이지

사용자가 증상을 입력하면 GPT 기반으로 약물을 추천받는 기능:

```typescript
// src/pages/SymptomAnalysisPage.tsx
const handleAnalyze = async () => {
  if (!user || !symptomText.trim()) return;
  
  try {
    const response = await analysisApi.analyzeSymptom({
      userId: user.id,
      symptomText: symptomText.trim(),
    });
    setAnalysisResult(response);
  } catch (err: any) {
    setError('분석에 실패했습니다.');
  }
};
```

**표시 정보**:
- 추천 가능한 약물 목록
- 주의해야 할 약물 목록
- 주의사항 요약

### 8.2 부작용 분석 페이지

복용했던 약물들의 공통 성분과 위험 패턴 분석.

### 8.3 OCR 성분표 분석 페이지

이미지 업로드 후 OCR을 통해 성분을 추출하고 안전성을 분석.

---

## 9단계: 커뮤니티 기능 구현

### 9.1 게시글 목록 페이지

**주요 기능**:
- 페이지네이션 구현
- 카테고리 필터링 (일반, 질문, 후기, 팁)
- 게시글 제목, 작성자, 날짜, 좋아요 수 표시

```typescript
// src/pages/PostsListPage.tsx
const fetchPosts = async (page: number, categoryFilter?: string) => {
  const response = await postsApi.getAllPosts(
    {
      page,
      size: pageSize,
      sort: ['createdAt,desc'],
    },
    categoryFilter || undefined
  );
  setPosts(response.content);
  setTotalPages(response.totalPages);
};
```

**페이지네이션 UI**:
- 처음/이전/다음/마지막 버튼
- 현재 페이지 및 전체 페이지 수 표시
- 총 게시글 수 표시

### 9.2 게시글 작성 페이지

```typescript
// src/pages/PostCreatePage.tsx
const handleSubmit = async (e: React.FormEvent) => {
  e.preventDefault();
  const request: PostCreateRequest = {
    title: title.trim(),
    content: content.trim(),
    ...(category && { category }),
  };
  const response = await postsApi.createPost(request);
  navigate(`/posts/${response.id}`);
};
```

**구현 내용**:
- 제목 및 내용 입력 폼
- 카테고리 선택
- 유효성 검사
- 작성 완료 시 상세 페이지로 이동

### 9.3 게시글 상세 페이지

**주요 기능**:

1. **게시글 표시**
   - 제목, 내용, 작성자, 작성일
   - 좋아요 기능
   - 수정/삭제 버튼 (작성자 본인만)

2. **댓글 시스템**
   ```typescript
   // 댓글 작성
   const handleCreateComment = async () => {
     await commentsApi.createComment({
       postId: Number(postId),
       content: commentContent.trim(),
     });
     await fetchComments(); // 댓글 목록 새로고침
   };
   
   // 댓글 수정
   const handleUpdateComment = async (commentId: number) => {
     await commentsApi.updateComment(commentId, {
       content: editingCommentContent.trim(),
     });
   };
   
   // 댓글 삭제
   const handleDeleteComment = async (commentId: number) => {
     if (!window.confirm('댓글을 삭제하시겠습니까?')) return;
     await commentsApi.deleteComment(commentId);
   };
   ```

3. **권한 관리**
   - `authorId`와 현재 로그인 사용자 `id` 비교
   - 작성자 본인일 경우에만 수정/삭제 버튼 표시

4. **좋아요 기능**
   - 게시글 좋아요/좋아요 취소
   - 댓글 좋아요
   - 실시간 좋아요 수 업데이트

---

## 10단계: TypeScript 타입 정의

### 10.1 API 응답 타입 정의

OpenAPI 명세서를 기반으로 모든 API 응답 타입을 정의:

```typescript
// src/types/api.ts
export interface PostResponse {
  id: number;
  authorId: number;
  authorNickname: string;
  title: string;
  content: string;
  category?: string;
  likeCount: number;
  isLiked: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CommentResponse {
  id: number;
  postId: number;
  authorId: number;
  authorNickname: string;
  content: string;
  likeCount: number;
  isLiked: boolean;
  createdAt: string;
}
```

**장점**:
- 컴파일 타임 타입 체크
- IDE 자동완성 지원
- API 스펙 변경 시 즉시 오류 감지

---

## 11단계: 에러 처리 및 사용자 경험 개선

### 11.1 에러 메시지 표시

모든 API 호출에 try-catch를 적용하여 사용자 친화적인 에러 메시지 표시:

```typescript
try {
  const response = await api.call();
  // 성공 처리
} catch (err: any) {
  setError(err.response?.data?.message || '작업에 실패했습니다.');
}
```

### 11.2 로딩 상태 관리

```typescript
const [loading, setLoading] = useState(true);

// API 호출 중
setLoading(true);
// API 호출 완료
setLoading(false);
```

### 11.3 빈 상태 처리

데이터가 없을 때 사용자에게 안내 메시지 표시:

```typescript
{posts.length === 0 ? (
  <div className="text-center py-12">
    <div className="text-gray-500">게시글이 없습니다.</div>
  </div>
) : (
  // 게시글 목록 렌더링
)}
```

---

## 12단계: 스타일링 및 UI/UX 개선

### 12.1 Tailwind CSS 활용

유틸리티 클래스를 사용하여 일관된 디자인 시스템 구축:

- 반응형 디자인 (`sm:`, `md:`, `lg:` 브레이크포인트)
- 호버 효과 및 트랜지션
- 색상 시스템 (primary, secondary, danger 등)

### 12.2 컴포넌트 재사용성

공통 스타일 패턴을 유틸리티 클래스로 추출하여 일관성 유지.

---

## 주요 구현 포인트 요약

### ✅ 성공적으로 구현한 기능

1. **JWT 기반 인증 시스템**
   - 자동 토큰 주입
   - 토큰 만료 자동 처리
   - 보호된 라우트

2. **상태 관리**
   - Zustand를 활용한 간단하고 효율적인 상태 관리
   - localStorage와의 동기화

3. **API 통신**
   - 모듈화된 API 클라이언트
   - 타입 안정성 보장
   - 일관된 에러 처리

4. **커뮤니티 기능**
   - 게시글 CRUD
   - 댓글 시스템
   - 좋아요 기능
   - 권한 기반 UI 표시

5. **사용자 경험**
   - 로딩 상태 표시
   - 에러 메시지 표시
   - 빈 상태 처리
   - 반응형 디자인

---

## 프로젝트 구조

```
src/
├── api/              # API 클라이언트 모듈
│   ├── client.ts     # Axios 인스턴스 및 인터셉터
│   ├── auth.ts       # 인증 API
│   ├── users.ts      # 사용자 및 알러지 API
│   ├── posts.ts      # 게시글 API
│   ├── comments.ts   # 댓글 API
│   ├── analysis.ts   # 분석 API
│   ├── medications.ts # 약물 검색 API
│   └── images.ts     # 이미지 업로드 API
├── components/       # 공통 컴포넌트
│   └── Layout.tsx    # 레이아웃 컴포넌트
├── pages/            # 페이지 컴포넌트
│   ├── HomePage.tsx
│   ├── LoginPage.tsx
│   ├── RegisterPage.tsx
│   ├── AllergiesPage.tsx
│   ├── SymptomAnalysisPage.tsx
│   ├── SideEffectAnalysisPage.tsx
│   ├── OcrAnalysisPage.tsx
│   ├── PostsListPage.tsx
│   ├── PostCreatePage.tsx
│   ├── PostDetailPage.tsx
│   └── MyPage.tsx
├── store/            # 상태 관리
│   └── authStore.ts  # 인증 상태 관리
├── types/            # TypeScript 타입 정의
│   └── api.ts
├── App.tsx           # 메인 앱 컴포넌트
├── main.tsx          # 진입점
└── index.css         # 전역 스타일
```

---

## 개발 중 겪은 주요 이슈 및 해결 방법

### 1. CORS 문제
**문제**: 개발 환경에서 백엔드 API 호출 시 CORS 에러 발생

**해결**: Vite 프록시 설정을 통해 `/api` 요청을 백엔드 서버로 프록시

### 2. JWT 토큰 관리
**문제**: 매번 API 호출 시 토큰을 수동으로 추가해야 함

**해결**: Axios 인터셉터를 사용하여 자동으로 토큰 주입

### 3. 타입 안정성
**문제**: API 응답 타입이 명확하지 않아 런타임 에러 발생 가능

**해결**: OpenAPI 명세서를 기반으로 모든 타입을 정의하고 TypeScript로 타입 체크

---

## 향후 개선 사항

1. **이미지 업로드 기능**
   - 게시글에 이미지 첨부 기능 추가
   - `/api/posts/images` API 활용

2. **검색 기능**
   - 게시글 제목/내용 검색
   - 고급 필터링 옵션

3. **정렬 옵션**
   - 최신순, 인기순, 댓글순 정렬

4. **무한 스크롤**
   - 페이지네이션 대신 무한 스크롤 구현

5. **실시간 알림**
   - 댓글 알림
   - 좋아요 알림

---

## 결론

이 프로젝트를 통해 React + TypeScript 기반의 현대적인 웹 애플리케이션 개발 경험을 쌓았습니다. 특히 다음 부분에서 많은 것을 배웠습니다:

- **상태 관리**: Zustand의 간단하고 효율적인 사용법
- **API 통신**: Axios 인터셉터를 활용한 인증 처리
- **타입 안정성**: TypeScript를 활용한 안전한 개발
- **사용자 경험**: 로딩, 에러, 빈 상태 등 다양한 상태 처리

프로젝트는 확장 가능한 구조로 설계되어 있어 향후 기능 추가가 용이합니다.

