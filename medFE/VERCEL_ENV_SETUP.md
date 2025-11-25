# Vercel 환경 변수 설정 가이드

## ⚠️ 현재 문제

요청이 Vercel 도메인(`https://med-rosy.vercel.app/api/auth/login`)으로 가고 있습니다.
이는 `VITE_API_BASE_URL` 환경 변수가 설정되지 않았기 때문입니다.

## 해결 방법

### 1. Vercel 대시보드 접속
https://vercel.com/dashboard

### 2. 프로젝트 선택
- `med` 또는 `med-rosy` 프로젝트 클릭

### 3. 환경 변수 설정
1. **Settings** 탭 클릭
2. 왼쪽 메뉴에서 **Environment Variables** 클릭
3. **Add New** 버튼 클릭
4. 다음 정보 입력:
   - **Key**: `VITE_API_BASE_URL`
   - **Value**: `http://16.184.46.179:8080`
   - **Environment**: 
     - ✅ Production
     - ✅ Preview  
     - ✅ Development
     - 모두 체크!
5. **Save** 클릭

### 4. 재배포
환경 변수를 추가한 후:
1. **Deployments** 탭으로 이동
2. 최신 배포의 **⋯** (점 3개) 메뉴 클릭
3. **Redeploy** 선택
4. 또는 새 커밋을 푸시하면 자동으로 재배포됨

## 확인 방법

재배포 후 브라우저 콘솔에서 확인:

### ✅ 정상인 경우:
```
🔍 API 설정 확인:
  - VITE_API_BASE_URL: http://16.184.46.179:8080
  - 사용할 baseURL: http://16.184.46.179:8080

API 요청: {
  url: "/api/auth/login",
  baseURL: "http://16.184.46.179:8080",
  fullURL: "http://16.184.46.179:8080/api/auth/login"
}
```

### ❌ 문제가 있는 경우:
```
🔍 API 설정 확인:
  - VITE_API_BASE_URL: (설정되지 않음)
  - 사용할 baseURL: (빈 값 - 상대 경로 사용)

❌ 프로덕션 환경에서 VITE_API_BASE_URL이 설정되지 않았습니다!
```

## 중요 사항

1. **환경 변수는 빌드 시점에 주입됩니다**
   - 환경 변수를 추가/수정한 후 **반드시 재배포**해야 합니다
   - 코드만 푸시해도 재배포되지만, 환경 변수 변경은 수동 재배포가 필요할 수 있습니다

2. **모든 환경에 설정**
   - Production, Preview, Development 모두에 설정하는 것을 권장합니다

3. **URL 끝에 `/api` 붙이지 않음**
   - `http://16.184.46.179:8080` ✅
   - `http://16.184.46.179:8080/api` ❌ (중복됨)

## 트러블슈팅

### 환경 변수가 적용되지 않는 경우
1. 환경 변수 이름 확인: `VITE_API_BASE_URL` (대소문자 정확히)
2. 재배포 확인: 환경 변수 추가 후 반드시 재배포
3. 빌드 로그 확인: Vercel 빌드 로그에서 환경 변수 주입 확인

### 여전히 405 에러가 발생하는 경우
1. 브라우저 콘솔에서 실제 요청 URL 확인
2. 백엔드 서버가 실행 중인지 확인
3. CORS 설정 확인 (백엔드에서 Vercel 도메인 허용)

