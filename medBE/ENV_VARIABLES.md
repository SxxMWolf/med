# 환경 변수 설정 가이드

의약품 분석 플랫폼 배포를 위한 환경 변수 설정 가이드입니다.

## 📋 필수 환경 변수

### 데이터베이스 설정

```bash
# ============================================
# AWS RDS 설정 (주석 처리됨 - 복원 시 주석 해제)
# ============================================
# 데이터베이스 사용자명
# med_DB_USERNAME=sxxm

# 데이터베이스 비밀번호
# med_DB_PASSWORD=your_secure_password_here

# 데이터베이스 URL (선택적, application.properties에 기본값 있음)
# DB_URL=jdbc:postgresql://med-db.ct8g22igmvtq.ap-northeast-2.rds.amazonaws.com:5432/postgres

# ============================================
# 로컬 PostgreSQL 설정 (활성화됨)
# ============================================
# 로컬 데이터베이스는 application.properties에서 직접 설정됨
# DB 이름: localMED_DB
# 사용자: sxxm
# 비밀번호: sxxmpass
# 연결 URL: jdbc:postgresql://localhost:5432/localMED_DB
```

### JWT 설정

```bash
# JWT 시크릿 키 (최소 256비트 권장)
# 예: openssl rand -base64 32
JWT_SECRET=your_jwt_secret_key_minimum_256_bits_here
```

### OpenAI GPT API 설정

```bash
# OpenAI API 키
OPENAI_API_KEY=sk-your_openai_api_key_here

# GPT API URL (기본값 사용 가능)
GPT_API_URL=https://api.openai.com/v1/chat/completions

# GPT 모델 (기본값: gpt-4o-mini)
GPT_MODEL=gpt-4o-mini
```

### Google Vision API 설정

```bash
# Google Cloud 서비스 계정 인증 파일 경로
GOOGLE_APPLICATION_CREDENTIALS=/path/to/google-credentials.json
```

### Python API 서비스 설정

```bash
# Python 서비스 URL
PYTHON_API_URL=http://localhost:8000
# 또는 Docker Compose 사용 시
PYTHON_API_URL=http://python-service:8000
```

---

## 🔧 선택적 환경 변수

### 의약품 DB API 설정

```bash
# 식품의약품안전처 API (선택적)
MFDS_API_URL=https://api.mfds.go.kr
MFDS_API_KEY=your_mfds_api_key_here
```

### 이메일 설정

```bash
# Gmail SMTP 설정
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password_here
# 참고: Gmail 앱 비밀번호 사용 필요
```

### 콘텐츠 검증 설정

```bash
# GPT 기반 콘텐츠 검증 활성화 (기본값: false)
CONTENT_VALIDATION_ENABLED=false
```

### 서버 설정

```bash
# 서버 포트 (기본값: 8080)
SERVER_PORT=8080
```

---

## 🚀 환경 변수 설정 방법

### 방법 1: .env 파일 사용 (로컬 개발)

```bash
# .env 파일 생성
cp .env.example .env

# .env 파일 편집
nano .env

# 환경 변수 로드하여 실행
source .env
./gradlew bootRun
```

### 방법 2: 시스템 환경 변수 (프로덕션)

```bash
# Linux/macOS
export med_DB_USERNAME=your_username
export med_DB_PASSWORD=your_password
export JWT_SECRET=your_jwt_secret
# ... 기타 환경 변수

# Windows (PowerShell)
$env:med_DB_USERNAME="your_username"
$env:med_DB_PASSWORD="your_password"
$env:JWT_SECRET="your_jwt_secret"
```

### 방법 3: Docker Compose

```yaml
# docker-compose.yml
services:
  med-be:
    environment:
      - med_DB_USERNAME=${med_DB_USERNAME}
      - med_DB_PASSWORD=${med_DB_PASSWORD}
      # ... 기타 환경 변수
```

또는 `.env` 파일을 사용:

```bash
# .env 파일 생성
med_DB_USERNAME=your_username
med_DB_PASSWORD=your_password
# ...

# docker-compose.yml에서 자동으로 로드됨
docker-compose up -d
```

### 방법 4: AWS Elastic Beanstalk

```bash
eb setenv \
  med_DB_USERNAME=your_username \
  med_DB_PASSWORD=your_password \
  JWT_SECRET=your_jwt_secret \
  OPENAI_API_KEY=your_openai_key
```

### 방법 5: AWS ECS Task Definition

```json
{
  "containerDefinitions": [{
    "environment": [
      {"name": "med_DB_USERNAME", "value": "your_username"},
      {"name": "med_DB_PASSWORD", "value": "your_password"},
      {"name": "JWT_SECRET", "value": "your_jwt_secret"}
    ]
  }]
}
```

---

## 🔐 보안 권장사항

### 1. 비밀번호 생성

```bash
# 강력한 비밀번호 생성 (Linux/macOS)
openssl rand -base64 32

# JWT 시크릿 생성
openssl rand -base64 32
```

### 2. 환경 변수 파일 보호

```bash
# .env 파일 권한 설정
chmod 600 .env

# .gitignore에 추가
echo ".env" >> .gitignore
```

### 3. 프로덕션 환경

- 환경 변수를 시스템 레벨에서 설정
- AWS Secrets Manager 또는 환경 변수 관리 도구 사용
- 코드에 비밀번호 하드코딩 금지

---

## ✅ 환경 변수 확인

### 로컬 확인

```bash
# 환경 변수 확인
env | grep med_
env | grep JWT_SECRET
env | grep OPENAI_API_KEY
```

### 애플리케이션 내부 확인

```java
// 로그에서 확인 (민감한 정보는 마스킹)
log.info("DB Username: {}", System.getenv("med_DB_USERNAME"));
log.info("JWT Secret configured: {}", 
    System.getenv("JWT_SECRET") != null);
```

### 헬스체크를 통한 확인

```bash
# Actuator info 엔드포인트 (환경 변수는 노출되지 않음)
curl http://localhost:8080/actuator/info
```

---

## 🐛 문제 해결

### 환경 변수가 로드되지 않음

**원인**: 환경 변수가 설정되지 않았거나 잘못된 이름 사용

**해결**:
1. 환경 변수 이름 확인 (대소문자 구분)
2. `.env` 파일이 올바른 위치에 있는지 확인
3. 환경 변수 로드 순서 확인

### 민감한 정보 노출

**원인**: 로그에 환경 변수 출력

**해결**:
1. 프로덕션에서는 `spring.jpa.show-sql=false`
2. 로그 레벨을 INFO 이상으로 설정
3. 에러 메시지에 민감한 정보 포함하지 않기

---

## 📝 체크리스트

배포 전 다음 사항을 확인하세요:

- [ ] 모든 필수 환경 변수가 설정되어 있는가?
- [ ] JWT_SECRET이 충분히 강력한가? (최소 256비트)
- [ ] 데이터베이스 비밀번호가 안전한가?
- [ ] .env 파일이 .gitignore에 포함되어 있는가?
- [ ] 프로덕션 환경에서 환경 변수가 안전하게 관리되는가?
- [ ] 환경 변수 이름이 올바른가? (대소문자, 언더스코어)

