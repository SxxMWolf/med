# Mixed Content 문제 해결

## 문제
HTTPS 페이지에서 HTTP API를 호출하려고 해서 브라우저가 차단합니다.

## 임시 해결책: Vercel 프록시 사용

`vercel.json`에 프록시 설정을 추가했습니다. 이제 `/api/*` 요청이 Vercel 서버를 통해 백엔드로 전달됩니다.

### 적용 방법

1. **환경 변수 제거 또는 빈 값으로 설정**
   - Vercel 대시보드 → Settings → Environment Variables
   - `VITE_API_BASE_URL`을 삭제하거나 빈 값(`""`)으로 설정
   - 또는 로컬 `.env` 파일에서도 확인

2. **코드 확인**
   - `src/api/client.ts`와 `src/api/index.ts`에서 `baseURL`이 빈 문자열이면 상대 경로 사용
   - 상대 경로(`/api/...`)는 Vercel 프록시를 통해 전달됨

3. **재배포**
   - 변경사항 커밋 및 푸시
   - Vercel이 자동으로 재배포

### 작동 원리

```
브라우저 → https://med-rosy.vercel.app/api/auth/login
         ↓ (Vercel 프록시)
         → http://16.184.46.179:8080/api/auth/login
```

### 장점
- 빠르게 적용 가능
- HTTPS → HTTP 변환 자동 처리
- 코드 변경 최소화

### 단점
- Vercel 서버를 경유하므로 약간의 지연 발생
- 백엔드 서버가 외부에서 접근 가능해야 함

## 장기 해결책: 백엔드 HTTPS 설정

더 안전하고 효율적인 방법은 백엔드 서버에 HTTPS를 설정하는 것입니다.

자세한 내용은 `HTTPS_SETUP.md` 파일을 참고하세요.

### 주요 단계
1. 도메인 설정 (선택사항)
2. Let's Encrypt로 SSL 인증서 발급
3. Nginx 리버스 프록시 설정
4. 프론트엔드 환경 변수를 HTTPS URL로 변경

