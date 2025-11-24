# HTTPS 설정 완료 ✅

백엔드 서버에 HTTPS를 설정하여 Vercel 프론트엔드와의 Mixed Content 문제를 해결했습니다.

## 📁 생성된 파일

```
medBE/
├── nginx/
│   ├── nginx.conf          # Nginx 리버스 프록시 설정
│   └── Dockerfile          # Nginx Docker 이미지
├── scripts/
│   ├── setup-ssl.sh        # SSL 인증서 발급 스크립트
│   └── quick-start-https.sh # 빠른 시작 스크립트
├── docker-compose.yml      # Nginx 서비스 추가됨
└── HTTPS_SETUP.md          # 상세 설정 가이드
```

## 🚀 빠른 시작

### 옵션 1: 자동 스크립트 사용 (권장)

```bash
cd ~/med/medBE
./scripts/quick-start-https.sh
```

### 옵션 2: 수동 설정

#### 도메인이 있는 경우

```bash
# 1. SSL 인증서 발급
./scripts/setup-ssl.sh api.yourdomain.com your-email@example.com

# 2. Nginx 설정 업데이트 (server_name 수정)
# nginx/nginx.conf 파일에서 server_name을 도메인으로 변경

# 3. Docker Compose 재시작
cd ~/med
docker-compose down
docker-compose up -d --build
```

#### 도메인이 없는 경우 (임시)

```bash
# 1. 자체 서명 인증서 생성
./scripts/setup-ssl.sh

# 2. Docker Compose 재시작
cd ~/med
docker-compose down
docker-compose up -d --build
```

## 📋 설정 단계

1. **AWS Security Group 설정**
   - 포트 80 (HTTP) 열기
   - 포트 443 (HTTPS) 열기

2. **SSL 인증서 발급**
   - 도메인 있음: Let's Encrypt 사용
   - 도메인 없음: 자체 서명 인증서 (임시)

3. **Docker Compose 재시작**
   ```bash
   docker-compose up -d --build
   ```

4. **프론트엔드 환경변수 업데이트**
   - Vercel: `VITE_API_BASE_URL=https://api.yourdomain.com`
   - 또는: `VITE_API_BASE_URL=https://16.184.46.179`

## 🔍 확인 방법

```bash
# 서비스 상태 확인
docker-compose ps

# 로그 확인
docker-compose logs nginx
docker-compose logs med-be

# HTTPS 테스트
curl -k https://your-domain/api/health
```

## ⚠️ 주의사항

1. **자체 서명 인증서**: 브라우저에서 보안 경고가 표시됩니다. 프로덕션에서는 도메인을 사용하세요.

2. **Let's Encrypt 인증서**: 90일마다 갱신이 필요합니다. 자동 갱신을 설정하세요.

3. **포트 변경**: 백엔드는 이제 내부 네트워크(8080)에서만 접근 가능합니다. 외부에서는 Nginx(443)를 통해 접근하세요.

## 📚 상세 가이드

더 자세한 내용은 [HTTPS_SETUP.md](./HTTPS_SETUP.md)를 참고하세요.

## 🎯 다음 단계

1. ✅ HTTPS 설정 완료
2. ⏳ 프론트엔드 환경변수 업데이트
3. ⏳ 프론트엔드 재배포
4. ⏳ 로그인 기능 테스트

