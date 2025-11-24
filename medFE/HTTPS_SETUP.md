# HTTPS Mixed Content 문제 해결 가이드

## 문제 상황

Vercel은 HTTPS로 배포되지만, 백엔드 API가 HTTP(`http://16.184.46.179:8080`)로 제공되어 브라우저가 Mixed Content 정책으로 요청을 차단합니다.

**에러 메시지:**
```
Mixed Content: The page at 'https://...' was loaded over HTTPS, 
but requested an insecure XMLHttpRequest endpoint 'http://...'. 
This request has been blocked.
```

## 해결 방법

### 방법 1: 백엔드 서버에 HTTPS 설정 (권장) ⭐

가장 안전하고 권장되는 방법입니다.

#### 1.1 Let's Encrypt로 SSL 인증서 발급

EC2 서버에서:

```bash
# Certbot 설치 (Ubuntu/Debian)
sudo apt update
sudo apt install certbot

# Nginx가 있는 경우
sudo certbot --nginx -d your-domain.com

# 또는 standalone 모드
sudo certbot certonly --standalone -d your-domain.com
```

#### 1.2 Spring Boot에 HTTPS 설정

`application.properties` 또는 `application.yml`:

```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: your-password
    key-store-type: PKCS12
    key-alias: tomcat
```

#### 1.3 Nginx 리버스 프록시 사용 (권장)

Nginx 설정:

```nginx
server {
    listen 443 ssl;
    server_name your-domain.com;

    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

#### 1.4 프론트엔드 환경 변수 업데이트

Vercel 환경 변수:
```
VITE_API_BASE_URL = https://your-domain.com
```

또는

```
VITE_API_BASE_URL = https://16.184.46.179:8443
```

---

### 방법 2: Vercel 프록시 사용 (임시 해결책)

Vercel의 `vercel.json`에서 프록시 설정:

```json
{
  "rewrites": [
    {
      "source": "/api/:path*",
      "destination": "http://16.184.46.179:8080/api/:path*"
    }
  ]
}
```

그리고 프론트엔드에서 상대 경로 사용:
```typescript
const API_BASE_URL = ''; // 빈 문자열 (상대 경로)
```

**단점:**
- Vercel 서버를 경유하므로 지연 발생 가능
- 백엔드 서버가 외부에서 접근 가능해야 함

---

### 방법 3: 개발 환경에서만 HTTP 허용 (비권장)

**⚠️ 프로덕션에서는 사용하지 마세요!**

브라우저 보안 설정을 변경하는 것은 권장되지 않습니다.

---

## 권장 해결 순서

1. **도메인 설정** (선택사항)
   - EC2에 도메인 연결 (예: `api.yourdomain.com`)
   - Route 53 또는 다른 DNS 서비스 사용

2. **SSL 인증서 발급**
   - Let's Encrypt 사용 (무료)
   - Certbot으로 자동 갱신 설정

3. **Nginx 리버스 프록시 설정**
   - HTTPS로 요청 받아서 HTTP 백엔드로 전달
   - 가장 안전하고 효율적

4. **프론트엔드 환경 변수 업데이트**
   - Vercel에서 `VITE_API_BASE_URL`을 HTTPS URL로 변경
   - 재배포

---

## 빠른 테스트 방법

임시로 테스트하려면:

1. **로컬에서 HTTP 서버로 테스트**
   ```bash
   npm run dev
   # http://localhost:3000 에서 테스트
   ```

2. **Vercel 프록시 사용** (위의 방법 2)
   - 빠르게 적용 가능
   - 프로덕션용으로는 권장하지 않음

---

## 참고 자료

- [Let's Encrypt 공식 문서](https://letsencrypt.org/)
- [Certbot 사용 가이드](https://certbot.eff.org/)
- [Nginx SSL 설정](https://nginx.org/en/docs/http/configuring_https_servers.html)
- [Spring Boot HTTPS 설정](https://spring.io/guides/gs/spring-boot-https/)

