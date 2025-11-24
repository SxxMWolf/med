# HTTPS 설정 가이드

이 가이드는 백엔드 서버에 HTTPS를 설정하여 Vercel 프론트엔드와의 Mixed Content 문제를 해결하는 방법을 설명합니다.

## 📋 목차

1. [개요](#개요)
2. [사전 요구사항](#사전-요구사항)
3. [도메인이 있는 경우 (권장)](#도메인이-있는-경우-권장)
4. [도메인이 없는 경우 (임시)](#도메인이-없는-경우-임시)
5. [배포 및 테스트](#배포-및-테스트)
6. [문제 해결](#문제-해결)

## 개요

현재 구조:
- **프론트엔드**: Vercel (HTTPS) - `https://med-rosy.vercel.app`
- **백엔드**: EC2 (HTTP) - `http://16.184.46.179:8080`

**문제**: HTTPS 페이지에서 HTTP API 호출 시 브라우저가 Mixed Content 정책으로 차단

**해결책**: Nginx 리버스 프록시를 통해 백엔드에 HTTPS 제공

## 사전 요구사항

- EC2 서버에 접근 권한
- 도메인 (권장) 또는 IP 주소
- 포트 80, 443이 열려 있어야 함 (AWS Security Group 설정)

### AWS Security Group 설정

EC2 인스턴스의 Security Group에서 다음 포트를 열어주세요:

```
인바운드 규칙:
- 포트 80 (HTTP) - 소스: 0.0.0.0/0
- 포트 443 (HTTPS) - 소스: 0.0.0.0/0
```

## 도메인이 있는 경우 (권장) ⭐

### 1단계: 도메인 DNS 설정

도메인을 EC2 서버 IP로 연결:

```
A 레코드: api.yourdomain.com -> 16.184.46.179
```

### 2단계: SSL 인증서 발급

EC2 서버에서 실행:

```bash
cd ~/med/medBE
./scripts/setup-ssl.sh api.yourdomain.com your-email@example.com
```

또는 수동으로:

```bash
# Certbot 설치
sudo apt-get update
sudo apt-get install -y certbot

# 인증서 발급 (standalone 모드)
sudo certbot certonly --standalone \
    --non-interactive \
    --agree-tos \
    -d api.yourdomain.com \
    --email your-email@example.com

# 인증서 복사
sudo cp /etc/letsencrypt/live/api.yourdomain.com/fullchain.pem nginx/ssl/fullchain.pem
sudo cp /etc/letsencrypt/live/api.yourdomain.com/privkey.pem nginx/ssl/privkey.pem
sudo chmod 644 nginx/ssl/fullchain.pem
sudo chmod 600 nginx/ssl/privkey.pem
```

### 3단계: Nginx 설정 업데이트

`nginx/nginx.conf` 파일에서 `server_name` 수정:

```nginx
server {
    listen 443 ssl http2;
    server_name api.yourdomain.com;  # 여기 수정
    # ...
}
```

### 4단계: Docker Compose 재시작

```bash
cd ~/med
docker-compose down
docker-compose up -d --build
```

### 5단계: 인증서 자동 갱신 설정

Let's Encrypt 인증서는 90일마다 갱신이 필요합니다:

```bash
# 갱신 테스트
sudo certbot renew --dry-run

# Crontab에 자동 갱신 추가
sudo crontab -e

# 다음 줄 추가 (매일 자정에 갱신 확인)
0 0 * * * certbot renew --quiet && cd ~/med && docker-compose restart nginx
```

## 도메인이 없는 경우 (임시)

도메인이 없는 경우 자체 서명 인증서를 사용할 수 있지만, 브라우저에서 보안 경고가 표시됩니다.

### 1단계: 자체 서명 인증서 생성

```bash
cd ~/med/medBE
./scripts/setup-ssl.sh
```

또는 수동으로:

```bash
mkdir -p nginx/ssl
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout nginx/ssl/privkey.pem \
    -out nginx/ssl/fullchain.pem \
    -subj "/C=KR/ST=Seoul/L=Seoul/O=Med/CN=16.184.46.179"
```

### 2단계: Docker Compose 재시작

```bash
cd ~/med
docker-compose down
docker-compose up -d --build
```

⚠️ **주의**: 자체 서명 인증서는 프로덕션 환경에서 사용하지 마세요. 도메인을 구매하고 Let's Encrypt를 사용하는 것을 강력히 권장합니다.

## 배포 및 테스트

### 1. 서비스 상태 확인

```bash
docker-compose ps
docker-compose logs nginx
docker-compose logs med-be
```

### 2. HTTPS 연결 테스트

```bash
# Health check
curl -k https://api.yourdomain.com/api/health
# 또는 IP 주소 사용 (자체 서명 인증서인 경우)
curl -k https://16.184.46.179/api/health

# CORS 테스트
curl -X OPTIONS https://api.yourdomain.com/api/auth/login \
  -H "Origin: https://med-rosy.vercel.app" \
  -H "Access-Control-Request-Method: POST" \
  -v
```

### 3. 프론트엔드 환경변수 업데이트

Vercel 대시보드에서 환경변수 설정:

```
VITE_API_BASE_URL=https://api.yourdomain.com
# 또는 IP 주소 사용 (자체 서명 인증서)
VITE_API_BASE_URL=https://16.184.46.179
```

프론트엔드 재배포 후 테스트하세요.

## 문제 해결

### 문제 1: 인증서 발급 실패

**증상**: `certbot` 실행 시 오류

**해결책**:
- 포트 80이 열려 있는지 확인
- 도메인 DNS 설정이 올바른지 확인 (A 레코드)
- 방화벽 설정 확인

```bash
# 포트 확인
sudo netstat -tlnp | grep :80
sudo ufw status
```

### 문제 2: Nginx 컨테이너가 시작되지 않음

**증상**: `docker-compose logs nginx`에서 오류

**해결책**:
- SSL 인증서 파일이 존재하는지 확인
- 파일 권한 확인

```bash
ls -la nginx/ssl/
# fullchain.pem과 privkey.pem이 있어야 함
```

### 문제 3: 502 Bad Gateway

**증상**: HTTPS로 접속 시 502 에러

**해결책**:
- 백엔드 서비스가 실행 중인지 확인
- 네트워크 연결 확인

```bash
docker-compose ps
docker-compose logs med-be
docker exec med-backend wget -qO- http://localhost:8080/api/health
```

### 문제 4: Mixed Content 경고가 계속 나타남

**증상**: 브라우저 콘솔에 Mixed Content 경고

**해결책**:
- 프론트엔드 환경변수가 올바른지 확인
- 브라우저 캐시 클리어
- 프론트엔드 코드에서 API URL이 상대 경로가 아닌 절대 경로(HTTPS)를 사용하는지 확인

## 아키텍처

```
인터넷
  │
  ├─ HTTPS (443) ──> Nginx (리버스 프록시)
  │                      │
  │                      └─> HTTP (8080) ──> Spring Boot
  │
  └─ HTTP (80) ──> Nginx ──> HTTPS로 리다이렉트
```

## 참고 자료

- [Let's Encrypt 공식 문서](https://letsencrypt.org/)
- [Certbot 사용 가이드](https://certbot.eff.org/)
- [Nginx SSL 설정](https://nginx.org/en/docs/http/configuring_https_servers.html)
- [Docker Compose 문서](https://docs.docker.com/compose/)

## 다음 단계

HTTPS 설정이 완료되면:

1. ✅ 프론트엔드 환경변수 업데이트
2. ✅ 프론트엔드 재배포
3. ✅ 로그인 기능 테스트
4. ✅ 모든 API 엔드포인트 테스트

