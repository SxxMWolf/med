# Vercel 배포 가이드

## 배포 전 준비사항

### 1. GitHub 저장소에 푸시
```bash
git add .
git commit -m "Vercel 배포 준비"
git push origin main
```

## Vercel 배포 방법

### 방법 1: Vercel 웹 대시보드 사용 (권장)

1. **Vercel 가입 및 로그인**
   - https://vercel.com 접속
   - GitHub 계정으로 로그인

2. **프로젝트 Import**
   - "Add New..." → "Project" 클릭
   - GitHub 저장소 선택
   - 프로젝트 선택 후 "Import"

3. **프로젝트 설정**
   - **Framework Preset**: Vite (자동 감지됨)
   - **Root Directory**: `./` (기본값)
   - **Build Command**: `npm run build` (자동 설정됨)
   - **Output Directory**: `dist` (자동 설정됨)
   - **Install Command**: `npm install` (자동 설정됨)

4. **환경 변수 설정** ⚠️ 중요
   - "Environment Variables" 섹션으로 이동
   - 다음 환경 변수 추가:
     ```
     VITE_API_BASE_URL = http://16.184.46.179:8080
     ```
   - **주의**: 끝에 `/api` 붙이지 않음

5. **배포 실행**
   - "Deploy" 버튼 클릭
   - 배포 완료까지 대기 (약 1-2분)

### 방법 2: Vercel CLI 사용

1. **Vercel CLI 설치**
   ```bash
   npm i -g vercel
   ```

2. **로그인**
   ```bash
   vercel login
   ```

3. **배포**
   ```bash
   vercel
   ```
   - 프로젝트 설정 질문에 답변
   - 환경 변수 설정 시 `VITE_API_BASE_URL=http://16.184.46.179:8080` 입력

4. **프로덕션 배포**
   ```bash
   vercel --prod
   ```

## 환경 변수 설정

Vercel 대시보드에서 다음 환경 변수를 설정해야 합니다:

| 변수명 | 값 | 설명 |
|--------|-----|------|
| `VITE_API_BASE_URL` | `http://16.184.46.179:8080` | 백엔드 API 서버 URL |

**설정 위치**: 
- 프로젝트 → Settings → Environment Variables

## 배포 후 확인

1. **배포 URL 확인**
   - Vercel 대시보드에서 배포된 URL 확인
   - 예: `https://your-project.vercel.app`

2. **헬스체크 테스트**
   - 배포된 URL + `/test` 접속
   - API 연결 상태 확인

3. **기능 테스트**
   - 로그인/회원가입
   - 각 기능 페이지 동작 확인

## 주의사항

1. **CORS 설정**
   - 백엔드 서버에서 Vercel 도메인을 CORS 허용 목록에 추가해야 할 수 있습니다.
   - 예: `https://your-project.vercel.app`

2. **환경 변수**
   - 환경 변수는 빌드 시점에 주입되므로, 변경 후 재배포가 필요합니다.

3. **HTTPS/HTTP 혼용**
   - Vercel은 HTTPS를 사용하지만, 백엔드가 HTTP인 경우 브라우저에서 Mixed Content 경고가 발생할 수 있습니다.
   - 가능하면 백엔드도 HTTPS로 설정하는 것을 권장합니다.

## 트러블슈팅

### API 연결 실패
- 환경 변수 `VITE_API_BASE_URL`이 올바르게 설정되었는지 확인
- 백엔드 서버가 실행 중인지 확인
- CORS 설정 확인

### 빌드 실패
- `npm run build`를 로컬에서 실행하여 오류 확인
- Vercel 빌드 로그 확인

### 라우팅 오류
- `vercel.json`의 `rewrites` 설정 확인
- SPA 라우팅을 위해 모든 경로를 `index.html`로 리다이렉트해야 함

