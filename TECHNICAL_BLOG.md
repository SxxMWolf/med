# 의약품 안전성 분석 플랫폼 개발기: 단계별 구현 가이드

## 📋 프로젝트 개요

의약품 성분 분석, 부작용 예측, 증상 기반 약물 추천 기능을 제공하는 웹 애플리케이션입니다. OCR, AI(GPT), 마이크로서비스 아키텍처를 활용하여 구현했습니다.

---

## 🏗️ Step 1: 프로젝트 아키텍처 설계 및 초기 설정

### 1.1 마이크로서비스 아키텍처 도입

**목표**: AI/ML 로직과 비즈니스 로직의 분리

**구현 내용**:
- **Java Spring Boot (medBE)**: 메인 API 서버, 인증, 데이터베이스 관리
- **Python FastAPI (medPY)**: AI/ML 처리 (GPT 프롬프트 최적화, NLP 처리)

**주요 기술 스택**:
```gradle
// Spring Boot 3.3.5
- Spring Data JPA (PostgreSQL)
- Spring Security + JWT
- WebClient (비동기 HTTP 통신)
- Swagger/OpenAPI (API 문서화)
```

```python
# Python FastAPI
- FastAPI 0.115.0
- OpenAI GPT API
- Pydantic (데이터 검증)
```

### 1.2 데이터베이스 설계

**PostgreSQL 스키마 설계**:
- `users`: 사용자 정보
- `user_allergies`: 사용자 알러지 정보
- `posts`, `comments`: 커뮤니티 게시글/댓글
- `post_likes`, `comment_likes`: 좋아요 기능
- `ocr_ingredients`: OCR 분석 결과 저장
- `side_effect_reports`: 부작용 보고서

**주요 설계 포인트**:
- Foreign Key CASCADE 삭제
- 인덱싱 최적화 (username, email, created_at)
- `updated_at` 자동 업데이트 트리거

---

## 🔐 Step 2: 사용자 인증 및 보안 구현

### 2.1 JWT 기반 인증 시스템

**구현 파일**: 
- `JwtConfig.java`: JWT 토큰 생성/검증
- `JwtAuthenticationFilter.java`: 요청 필터링
- `SecurityConfig.java`: Spring Security 설정

**주요 기능**:
```java
// JWT 토큰 생성
- HS256 알고리즘 사용
- 토큰 만료 시간: 24시간
- Secret Key: 환경변수로 관리

// 인증 필터
- Authorization 헤더에서 Bearer 토큰 추출
- OPTIONS 요청 (CORS preflight) 처리
- 상세한 로깅 (디버깅 용이)
```

### 2.2 회원가입 및 로그인 API

**엔드포인트**:
- `POST /api/auth/register`: 회원가입
- `POST /api/auth/login`: 로그인 (JWT 토큰 발급)
- `GET /api/auth/me`: 현재 사용자 정보 조회

**보안 기능**:
- BCrypt 패스워드 암호화
- 이메일 중복 검증
- JWT 토큰에 username 저장 (비민감 정보만)

### 2.3 CORS 및 보안 설정

**CorsConfig.java**:
```java
// 특정 origin만 허용 (local development)
- localhost:3000, localhost:3001
- Credentials: true (쿠키/인증 정보 포함)
```

---

## 📸 Step 3: OCR 기반 의약품 성분 분석 구현

### 3.1 Google Vision API 연동

**VisionService.java**:
- 이미지에서 텍스트 추출
- Base64 또는 파일 경로/URL 지원
- 에러 핸들링 및 로깅

### 3.2 마이크로서비스 통신

**Java → Python 통신**:
```java
// PythonApiService.java
- WebClient를 사용한 비동기 HTTP 통신
- 타임아웃 설정 (30초)
- 상세한 에러 로깅
- 연결 실패 시 명확한 에러 메시지
```

**Python 서비스 역할**:
1. **OCR 텍스트 정리**: GPT를 활용한 가독성 향상
2. **성분 추출**: 정규화된 텍스트에서 성분명 추출
3. **성분 분석**: 알러지 성분 비교 및 위험도 평가

### 3.3 GPT 프롬프트 최적화

**2단계 프롬프트 전략**:
1. **1단계**: OCR 텍스트 → 정리된 텍스트 (GPT)
2. **2단계**: 정리된 텍스트 → 성분 목록 추출 (GPT)

**폴백 메커니즘**:
- GPT API 실패 시 기본 정규식 파싱 사용
- 안정성과 정확성의 균형

---

## 🧠 Step 4: AI 기반 의약품 분석 구현

### 4.1 증상 분석 기능

**SymptomAnalysisService.java**:
- 사용자 증상 입력
- 알러지 성분 기반 약물 필터링
- GPT를 통한 약물 추천 및 주의사항 제공

**응답 구조**:
```json
{
  "recommendedMedications": [...],
  "notRecommendedMedications": [...],
  "precautions": [...]
}
```

### 4.2 부작용 분석 기능

**SideEffectAnalysisService.java**:
- 복용 중인 약물들의 부작용 분석
- 공통 성분 추출
- 알러지 성분 매칭
- Python 서비스를 통한 고급 분석

**Python 서비스 (sideeffect_service.py)**:
- 공통 성분 자동 추출
- GPT를 통한 위험 패턴 분석
- 사용자 민감 성분 식별

---

## 👥 Step 5: 커뮤니티 기능 구현

### 5.1 게시글 및 댓글 시스템

**주요 엔드포인트**:
- `GET /api/posts`: 게시글 목록 (페이지네이션)
- `POST /api/posts`: 게시글 작성 (JWT 인증)
- `GET /api/posts/{postId}?withComments=true`: 게시글 상세 + 댓글
- `GET /api/comments/post/{postId}?page=0&size=20`: 댓글 목록 (페이지네이션)

**페이지네이션**:
```java
// Spring Data JPA Pageable 활용
- 기본 페이지 크기: 20
- 정렬: createdAt DESC
- 카테고리별 필터링 지원
```

### 5.2 좋아요 기능

**구현 내용**:
- 게시글 좋아요: `POST /api/posts/{postId}/like`
- 댓글 좋아요: `POST /api/comments/{commentId}/like`
- 중복 좋아요 방지 (DB 제약조건)
- 실시간 좋아요 수 반환

**데이터베이스 설계**:
```sql
-- 복합 Primary Key로 중복 방지
CREATE TABLE post_likes (
    user_id BIGINT,
    post_id BIGINT,
    PRIMARY KEY (user_id, post_id)
);
```

### 5.3 콘텐츠 검증

**ContentValidationService.java**:
- 부적절한 콘텐츠 필터링 (확장 가능한 구조)
- GPT 기반 검증 (선택적)

---

## 🔄 Step 6: 서비스 간 통신 최적화

### 6.1 WebClient 설정

**비동기 HTTP 통신**:
- Reactor 기반 논블로킹 I/O
- 타임아웃 및 재시도 로직
- 에러 핸들링 강화

**에러 처리 전략**:
```java
// PythonApiService.java
- 4xx/5xx 에러 상세 로깅
- Connection timeout 구분
- 사용자 친화적 에러 메시지
```

### 6.2 환경 변수 관리

**application.properties**:
```properties
python.api.url=${PYTHON_API_URL:http://localhost:8000}
```

**빌드 설정 (build.gradle)**:
```gradle
tasks.named('bootRun') {
    environment = System.getenv()  // 환경변수 전달
}
```

---

## 📝 Step 7: API 문서화 및 테스트

### 7.1 Swagger/OpenAPI 통합

**OpenApiConfig.java**:
- JWT Bearer 인증 스키마 설정
- API 그룹화 및 태그 관리

**주요 기능**:
- 인터랙티브 API 문서 (`/swagger-ui.html`)
- 인증 토큰 테스트 가능
- 요청/응답 스키마 자동 생성

### 7.2 DTO 패턴 적용

**엔티티와 DTO 분리**:
- 순환 참조 방지
- API 응답 구조 명확화
- LocalDateTime 직렬화 이슈 해결

**예시**:
```java
// UserAllergyResponse.java
@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
private LocalDateTime createdAt;
```

---

## 🛠️ Step 8: 고급 기능 구현

### 8.1 이메일 서비스

**EmailService.java**:
- 아이디 찾기
- 임시 비밀번호 발급
- Gmail SMTP 연동

### 8.2 알러지 관리

**UserController.java**:
- `GET /api/users/{userId}/allergies`: 알러지 목록
- `POST /api/users/{userId}/allergies`: 알러지 추가
- `DELETE /api/users/{userId}/allergies/{allergyId}`: 알러지 삭제

### 8.3 약물 검색

**MedicationController.java**:
- `GET /api/medications/search?name={약물명}`: 약물 검색
- 외부 API 연동 또는 내부 데이터베이스 검색

---

## 🚀 Step 9: 성능 최적화 및 에러 처리

### 9.1 JSON 직렬화 최적화

**JacksonConfig.java**:
- LocalDateTime ISO-8601 형식 직렬화
- 중첩 깊이 제한 증가 (2000)
- 타임존 설정 (Asia/Seoul)

### 9.2 에러 핸들링

**전략**:
- 계층별 예외 처리 (Controller → Service → Repository)
- 명확한 HTTP 상태 코드 반환
- 상세한 로깅 (SLF4J)

### 9.3 로깅 전략

**주요 로깅 포인트**:
- JWT 토큰 생성/검증
- 외부 API 호출 (Python, Vision, GPT)
- DB 쿼리 (개발 환경)
- 에러 발생 시 스택 트레이스

---

## 📦 Step 10: 배포 준비

### 10.1 환경 변수 관리

**필수 환경 변수**:
```bash
# Database
med_DB_PASSWORD

# JWT
JWT_SECRET

# OpenAI
OPENAI_API_KEY

# Google Vision
GOOGLE_APPLICATION_CREDENTIALS

# Python Service
PYTHON_API_URL
```

### 10.2 서비스 실행 스크립트

**Python 서비스 (start.sh)**:
- 가상환경 자동 활성화
- 의존성 설치 확인
- 환경변수 로드
- Uvicorn 서버 실행 (--reload)

### 10.3 테스트 환경 분리

**application-test.properties**:
- H2 인메모리 데이터베이스 사용
- 외부 서비스 Mock 설정

---

## 🎯 핵심 기술 포인트

### 1. 마이크로서비스 아키텍처
- **장점**: 기술 스택 선택의 자유, 독립적 배포, 확장성
- **구현**: WebClient를 통한 HTTP 통신, 비동기 처리

### 2. AI 통합
- **GPT API**: 증상 분석, 부작용 분석, OCR 텍스트 정리
- **프롬프트 엔지니어링**: 구조화된 JSON 응답 보장
- **폴백 메커니즘**: AI 실패 시 기본 로직 사용

### 3. 보안
- **JWT**: Stateless 인증
- **BCrypt**: 비밀번호 암호화
- **CORS**: 명시적 Origin 허용

### 4. 사용자 경험
- **페이지네이션**: 대용량 데이터 효율적 처리
- **실시간 피드백**: 좋아요 수, 사용자 상태 반영
- **에러 메시지**: 명확하고 사용자 친화적

---

## 📊 기술 스택 요약

### Backend (Java)
- Spring Boot 3.3.5
- Spring Data JPA
- Spring Security + JWT
- PostgreSQL
- Google Vision API
- WebClient (Reactive)

### AI Service (Python)
- FastAPI 0.115.0
- OpenAI GPT API
- Pydantic

### Infrastructure
- AWS RDS (PostgreSQL)
- 환경변수 기반 설정

---

## 🔮 향후 개선 방향

1. **캐싱 전략**: Redis 도입으로 GPT API 호출 최소화
2. **이미지 저장소**: AWS S3 또는 Cloudflare R2로 마이그레이션
3. **모니터링**: Prometheus + Grafana 연동
4. **로깅**: ELK Stack 또는 CloudWatch
5. **부하 분산**: Load Balancer, Auto Scaling
6. **API Rate Limiting**: 사용자별 요청 제한

---

## 💡 배운 점

1. **마이크로서비스 통신**: WebClient를 통한 비동기 통신의 중요성
2. **에러 핸들링**: 명확한 에러 메시지와 로깅의 가치
3. **AI 통합**: 프롬프트 설계와 폴백 전략의 필요성
4. **보안**: JWT 인증과 CORS 설정의 세심한 관리
5. **API 설계**: RESTful 원칙과 DTO 패턴의 유지보수성

---

이 프로젝트는 현대적인 웹 애플리케이션의 핵심 기술들을 학습하고 실전에 적용한 결과물입니다. 특히 AI와 전통적인 백엔드 개발의 융합을 통해 더 나은 사용자 경험을 제공할 수 있었습니다.

