# Med - 개인 맞춤형 복약 안전성 확인 웹 애플리케이션

약물 알러지와 복용 경험을 기반으로 개인 맞춤형 복약 안전성을 확인할 수 있는 웹 애플리케이션입니다.

## 주요 기능

### 1. 사용자 인증
- 회원가입 및 로그인
- JWT 기반 인증
- 사용자 정보 관리

### 2. 알러지 관리
- 복용하면 안 되는 성분(알러지 성분) 등록 및 관리
- 심각도 설정 (경미/보통/심각)
- 모든 기능에서 자동으로 알러지 정보 참조

### 3. 증상 분석
- 현재 겪고 있는 증상을 텍스트로 입력
- GPT 기반 약물 추천 및 주의사항 제공
- 알러지 성분과 매칭하여 안전한 약 추천
- "추천 가능한 약", "주의해야 할 약", "위험 요소 요약" 제공

### 4. 부작용 분석
- 이전에 복용했을 때 부작용이 있었던 약물명 입력
- 공통 성분 및 부작용 위험 성분 분석
- "당신이 민감할 가능성이 높은 성분" 분석
- "다른 사용자에게도 부작용이 많은 성분" 분석

### 5. OCR 성분표 분석
- 약 성분표 사진 업로드
- Google Vision API를 통한 OCR 텍스트 추출
- GPT 기반 위험도 분석
- "복용 가능", "주의 필요", "고위험 성분 포함" 등 레이블 제공

## 기술 스택

- **Frontend Framework**: React 18
- **Language**: TypeScript
- **Build Tool**: Vite
- **Styling**: Tailwind CSS
- **State Management**: Zustand
- **HTTP Client**: Axios
- **Routing**: React Router v6

## 설치 및 실행

### 필수 요구사항
- Node.js 18 이상
- npm 또는 yarn

### 설치

```bash
npm install
```

### 개발 서버 실행

```bash
npm run dev
```

개발 서버는 `http://localhost:3000`에서 실행됩니다.

### 빌드

```bash
npm run build
```

### 프로덕션 미리보기

```bash
npm run preview
```

## 환경 변수

`.env` 파일을 생성하여 다음 변수를 설정할 수 있습니다:

```env
VITE_API_BASE_URL=http://localhost:8080
```

기본값은 `http://localhost:8080`입니다.

## 프로젝트 구조

```
src/
├── api/              # API 클라이언트
│   ├── client.ts     # Axios 인스턴스 및 인터셉터
│   ├── auth.ts       # 인증 API
│   ├── users.ts      # 사용자 및 알러지 API
│   └── analysis.ts   # 분석 API
├── components/       # 공통 컴포넌트
│   └── Layout.tsx    # 레이아웃 컴포넌트
├── pages/            # 페이지 컴포넌트
│   ├── HomePage.tsx
│   ├── LoginPage.tsx
│   ├── RegisterPage.tsx
│   ├── AllergiesPage.tsx
│   ├── SymptomAnalysisPage.tsx
│   ├── SideEffectAnalysisPage.tsx
│   └── OcrAnalysisPage.tsx
├── store/            # 상태 관리
│   └── authStore.ts  # 인증 상태 관리
├── types/            # TypeScript 타입 정의
│   └── api.ts
├── App.tsx           # 메인 앱 컴포넌트
├── main.tsx          # 진입점
└── index.css         # 전역 스타일
```

## API 연동

백엔드 API는 OpenAPI 3.0 스펙을 따릅니다. 모든 API 요청은 JWT 토큰 기반 인증을 사용하며, 토큰은 자동으로 요청 헤더에 추가됩니다.

### 주요 API 엔드포인트

- `POST /api/auth/register` - 회원가입
- `POST /api/auth/login` - 로그인
- `GET /api/auth/me` - 현재 사용자 정보
- `GET /api/users/{userId}/allergies` - 알러지 목록 조회
- `POST /api/users/{userId}/allergies` - 알러지 추가
- `DELETE /api/users/{userId}/allergies/{allergyId}` - 알러지 삭제
- `POST /api/analysis/symptom` - 증상 분석
- `POST /api/analysis/side-effect` - 부작용 분석
- `POST /api/analysis/ocr` - OCR 분석

## 라이선스

MIT

