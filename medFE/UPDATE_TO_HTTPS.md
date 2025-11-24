# HTTPS 백엔드로 업데이트 가이드

## 현재 문제

백엔드를 HTTPS로 변경했지만, 프론트엔드가 여전히 HTTP로 요청하고 있습니다.

## 해결 방법

### 방법 1: Vercel 환경 변수 업데이트 (권장)

1. **Vercel 대시보드 접속**
   - https://vercel.com/dashboard
   - 프로젝트 선택

2. **환경 변수 수정**
   - Settings → Environment Variables
   - `VITE_API_BASE_URL` 찾기
   - **Value를 HTTPS로 변경**:
     ```
     기존: http://16.184.46.179:8080
     변경: https://16.184.46.179:8443
     ```
     또는 도메인을 사용하는 경우:
     ```
     https://your-domain.com
     ```

3. **재배포**
   - Deployments → 최신 배포의 ⋯ → Redeploy
   - 또는 새 커밋 푸시

### 방법 2: 프록시 사용 (환경 변수 제거)

프록시를 사용하려면 환경 변수를 제거하거나 빈 값으로 설정:

1. **Vercel 환경 변수 삭제**
   - Settings → Environment Variables
   - `VITE_API_BASE_URL` 삭제

2. **vercel.json 프록시 설정 확인**
   ```json
   {
     "rewrites": [
       {
         "source": "/api/:path*",
         "destination": "https://16.184.46.179:8443/api/:path*"
       }
     ]
   }
   ```

3. **재배포**

## 백엔드 HTTPS 포트 확인

백엔드 HTTPS 포트를 확인하세요:
- 일반적으로: `8443` (Spring Boot 기본 HTTPS 포트)
- 또는 `443` (Nginx 리버스 프록시 사용 시)

## 확인 방법

재배포 후 브라우저 콘솔에서:

### ✅ 정상인 경우:
```
🔍 API 설정 확인:
  - VITE_API_BASE_URL: https://16.184.46.179:8443
  - 사용할 baseURL: https://16.184.46.179:8443

API 요청: {
  url: "/api/auth/login",
  baseURL: "https://16.184.46.179:8443",
  fullURL: "https://16.184.46.179:8443/api/auth/login"
}
```

### ❌ 여전히 HTTP인 경우:
```
🔍 API 설정 확인:
  - VITE_API_BASE_URL: http://16.184.46.179:8080
  - 사용할 baseURL: http://16.184.46.179:8080
```

## 백엔드 HTTPS 포트가 다른 경우

백엔드 HTTPS 포트가 `8443`이 아닌 경우, 올바른 포트로 변경하세요:

예시:
- `https://16.184.46.179:443` (Nginx 사용 시)
- `https://your-domain.com` (도메인 사용 시)

## SSL 인증서 문제

자체 서명 인증서(self-signed certificate)를 사용하는 경우, 브라우저에서 인증서 오류가 발생할 수 있습니다.

해결:
1. **Let's Encrypt 사용** (권장)
   - 무료이고 브라우저에서 자동으로 신뢰됨
   
2. **프록시 사용**
   - Vercel 프록시를 통해 요청하면 인증서 문제 회피 가능

