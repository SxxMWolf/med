# 복약 안전 관리 시스템 - React Native 프론트엔드

약물 알러지와 복용 경험을 기반으로 개인 맞춤형 복약 안전성을 확인할 수 있는 Expo 기반 React Native 애플리케이션입니다.

## 주요 기능

### 1. 인증 시스템
- JWT 기반 회원가입 및 로그인
- SecureStore를 사용한 토큰 저장
- 자동 토큰 갱신

### 2. 알러지 관리
- 복용하면 안 되는 성분(알러지 성분) 등록 및 관리
- 모든 기능에서 자동 참조

### 3. 약 검색
- Spring Boot → 식약처 API를 통한 약품 검색
- 약품 상세 정보 조회

### 4. OCR 성분표 분석
- Expo ImagePicker를 사용한 이미지 선택/촬영
- Spring Boot → Python OCR API 연동
- 성분 정규화 및 알러지 매칭
- GPT 분석 결과 표시

### 5. 분석 결과 화면
- 추천 가능한 약 / 피해야 할 약 표시
- 고위험 성분 표시
- GPT 분석 요약
- 위험도 평가

### 6. 커뮤니티 기능
- 게시글 작성/조회/삭제
- 댓글 작성/조회
- 게시글 상세 보기

## 기술 스택

- **Framework**: Expo ~54.0.25
- **Router**: Expo Router 6.0.15
- **Language**: TypeScript
- **HTTP Client**: Axios
- **State Management**: Zustand
- **Form Management**: React Hook Form + Zod
- **Storage**: Expo SecureStore (JWT 토큰)
- **Image Picker**: Expo ImagePicker

## 프로젝트 구조

```
medFront/
├── app/                    # Expo Router 페이지
│   ├── login.tsx          # 로그인 화면
│   ├── signup.tsx         # 회원가입 화면
│   ├── allergies.tsx      # 알러지 관리 화면
│   ├── medicine-search.tsx # 약 검색 화면
│   ├── ocr-upload.tsx     # OCR 업로드 화면
│   ├── analysis-result.tsx # 분석 결과 화면
│   └── (tabs)/            # 탭 네비게이션
│       ├── index.tsx      # 홈 화면
│       └── community.tsx  # 커뮤니티 화면
├── lib/                   # 유틸리티 및 API 클라이언트
│   ├── api.ts             # Axios 인스턴스 및 인터셉터
│   ├── auth.ts            # 인증 관련 함수
│   └── services/          # API 서비스 함수
│       ├── allergy.ts     # 알러지 관련 API
│       ├── medicine.ts    # 약 검색 API
│       ├── ocr.ts         # OCR 분석 API
│       ├── analysis.ts    # 분석 결과 API
│       └── community.ts   # 커뮤니티 API
├── store/                 # 상태 관리
│   └── authStore.ts       # 인증 상태 스토어
└── components/            # 공통 컴포넌트
```

## 설치 및 실행

### 1. 의존성 설치

```bash
npm install
```

### 2. 환경 변수 설정

`.env` 파일을 생성하고 다음 내용을 추가하세요:

```env
EXPO_PUBLIC_API_BASE_URL=http://localhost:8080/api
```

### 3. 개발 서버 실행

```bash
npm start
```

iOS 시뮬레이터에서 실행:
```bash
npm run ios
```

Android 에뮬레이터에서 실행:
```bash
npm run android
```

## API 통신 구조

프론트엔드는 **Spring Boot 서버만 직접 호출**하며, Python 서비스는 프론트에서 직접 호출하지 않습니다.

### Spring Boot API 엔드포인트

#### 인증
- `POST /api/auth/signup` - 회원가입
- `POST /api/auth/login` - 로그인
- `POST /api/auth/logout` - 로그아웃
- `GET /api/auth/me` - 현재 사용자 정보
- `POST /api/auth/refresh` - 토큰 갱신

#### 알러지 관리
- `GET /api/allergies` - 알러지 목록 조회
- `POST /api/allergies` - 알러지 추가
- `DELETE /api/allergies/:id` - 알러지 삭제

#### 약 검색 (Spring Boot → 식약처 API)
- `GET /api/medicine/search` - 약품 검색
- `GET /api/medicine/:id` - 약품 상세 정보

#### OCR 분석 (Spring Boot → Python OCR API)
- `POST /api/ocr/analyze` - 약 성분표 이미지 분석
  - FormData로 이미지 파일 전송
  - Spring Boot가 Python OCR API를 호출하여 처리
  - 정규화된 성분 리스트 및 분석 결과 반환

#### 분석 결과
- `POST /api/analysis/symptom` - 증상 분석 요청
- `GET /api/analysis/:id` - 분석 결과 조회

#### 커뮤니티
- `GET /api/community/posts` - 게시글 목록
- `GET /api/community/posts/:id` - 게시글 상세
- `POST /api/community/posts` - 게시글 작성
- `DELETE /api/community/posts/:id` - 게시글 삭제
- `GET /api/community/posts/:id/comments` - 댓글 목록
- `POST /api/community/posts/:id/comments` - 댓글 작성
- `DELETE /api/community/posts/:id/comments/:commentId` - 댓글 삭제

## 주요 기능 상세

### JWT 인증 흐름

1. 로그인/회원가입 시 `access_token`과 `refresh_token`을 SecureStore에 저장
2. 모든 API 요청에 `Authorization` 헤더로 토큰 자동 첨부
3. 401 에러 발생 시 자동으로 토큰 갱신 시도
4. 갱신 실패 시 로그인 화면으로 리다이렉트

### OCR 이미지 업로드

- **Expo ImagePicker**를 사용하여 갤러리에서 선택하거나 카메라로 촬영
- 이미지를 FormData로 Spring Boot에 전송
- Spring Boot가 Python OCR API를 호출하여 성분 추출 및 정규화
- 정규화된 성분과 알러지 매칭 결과를 프론트에서 표시

### 분석 결과 표시

- Spring Boot가 Python/GPT 분석 결과를 JSON으로 반환
- 프론트는 결과를 시각적으로 정리하여 표시:
  - 추천 가능한 약 (안전/주의/경고 레벨)
  - 피해야 할 약 (위험도 레벨)
  - 고위험 성분 리스트
  - GPT 분석 요약

### 커뮤니티 기능

- 게시글 목록 및 상세 보기
- 게시글 작성/삭제 (본인 게시글만 삭제 가능)
- 댓글 작성/조회
- 모달 기반 UI

## 빌드 및 배포

### Android 빌드

```bash
eas build --platform android
```

### iOS 빌드

```bash
eas build --platform ios
```

## 개발 가이드

### 새 화면 추가

1. `app/` 디렉토리에 새 파일 생성
2. 필요시 API 서비스 함수를 `lib/services/`에 추가
3. 네비게이션은 `useRouter`를 사용하여 처리

### API 클라이언트 사용

```typescript
import apiClient from '@/lib/api';

// 인증이 필요한 요청 (자동으로 토큰 첨부)
const response = await apiClient.get('/api/endpoint');
```

### SecureStore 사용

```typescript
import * as SecureStore from 'expo-secure-store';

// 저장
await SecureStore.setItemAsync('key', 'value');

// 조회
const value = await SecureStore.getItemAsync('key');

// 삭제
await SecureStore.deleteItemAsync('key');
```

### 이미지 선택

```typescript
import * as ImagePicker from 'expo-image-picker';

// 갤러리에서 선택
const result = await ImagePicker.launchImageLibraryAsync({
  mediaTypes: ImagePicker.MediaTypeOptions.Images,
  allowsEditing: true,
  quality: 0.8,
});

// 카메라로 촬영
const result = await ImagePicker.launchCameraAsync({
  allowsEditing: true,
  quality: 0.8,
});
```

## 문제 해결

### API 연결 오류

1. `.env` 파일에 올바른 `EXPO_PUBLIC_API_BASE_URL`이 설정되어 있는지 확인
2. Spring Boot 서버가 실행 중인지 확인
3. 네트워크 권한이 허용되어 있는지 확인

### 이미지 업로드 오류

1. 갤러리/카메라 권한이 허용되어 있는지 확인
2. 이미지 파일 형식이 지원되는지 확인
3. Spring Boot 서버의 파일 업로드 설정 확인

### 인증 오류

1. SecureStore에 토큰이 제대로 저장되어 있는지 확인
2. 토큰 만료 시 자동 갱신이 작동하는지 확인
3. Spring Boot의 JWT 설정 확인

## 라이선스

이 프로젝트는 개인 프로젝트입니다.
