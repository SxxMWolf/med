# Swagger API 문서 사용 가이드

## 설정 완료 사항

1. **의존성 추가**: `build.gradle`에 Springdoc OpenAPI 의존성 추가
2. **OpenAPI 설정**: `OpenApiConfig.java`에서 Swagger 설정 완료
3. **Security 설정**: Swagger UI 접근 경로 허용
4. **Controller 어노테이션**: 주요 Controller에 API 설명 추가

## Swagger UI 접속 방법

애플리케이션 실행 후 다음 URL로 접속:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs (JSON)**: http://localhost:8080/v3/api-docs

## 기능

### 1. API 문서 자동 생성
- 모든 Controller의 엔드포인트가 자동으로 문서화됩니다
- Request/Response DTO 구조가 자동으로 표시됩니다

### 2. JWT 인증 테스트
- Swagger UI에서 "Authorize" 버튼 클릭
- JWT 토큰 입력: `Bearer {your-token}`
- 인증이 필요한 API를 바로 테스트할 수 있습니다

### 3. API 테스트
- 각 API의 "Try it out" 버튼으로 직접 호출 가능
- Request Body를 입력하여 테스트 가능
- Response를 바로 확인 가능

## 주요 API 그룹

1. **Authentication**: 인증 및 사용자 관리
2. **Analysis**: 의약품 분석 (증상, 부작용, OCR)
3. **Posts**: 게시글 관리
4. **Comments**: 댓글 관리
5. **Users**: 사용자 정보 및 알러지 관리

## 주의사항

- 프로덕션 환경에서는 Swagger UI 접근을 제한하는 것을 권장합니다
- `SecurityConfig`에서 Swagger 경로에 대한 접근 제어를 추가할 수 있습니다

