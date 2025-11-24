# 배포 가이드

의약품 분석 플랫폼 배포를 위한 상세 가이드입니다.

## 📋 목차

1. [필수 요구사항](#필수-요구사항)
2. [환경 변수 설정](#환경-변수-설정)
3. [로컬 빌드 및 실행](#로컬-빌드-및-실행)
4. [Docker를 사용한 배포](#docker를-사용한-배포)
5. [AWS 배포](#aws-배포)
6. [배포 후 확인](#배포-후-확인)
7. [문제 해결](#문제-해결)

---

## 필수 요구사항

### 시스템 요구사항
- **Java**: 17 이상
- **Gradle**: 8.5 이상 (또는 Gradle Wrapper 사용)
- **PostgreSQL**: 12 이상
- **Docker**: 20.10 이상 (Docker 배포 시)
- **Python**: 3.11 이상 (Python 서비스용)

### 외부 서비스
- **OpenAI API Key**: GPT 분석용
- **Google Cloud Vision API**: OCR 기능용
- **이메일 서비스**: Gmail SMTP (선택적)

---

## 환경 변수 설정

### 필수 환경 변수

```bash
# 데이터베이스
med_DB_USERNAME=your_username
med_DB_PASSWORD=your_password
DB_URL=jdbc:postgresql://your-db-host:5432/postgres

# JWT
JWT_SECRET=your_jwt_secret_key_minimum_256_bits

# OpenAI GPT
OPENAI_API_KEY=your_openai_api_key
GPT_API_URL=https://api.openai.com/v1/chat/completions
GPT_MODEL=gpt-4o-mini

# Google Vision API
GOOGLE_APPLICATION_CREDENTIALS=/path/to/google-credentials.json

# Python 서비스
PYTHON_API_URL=http://localhost:8000
```

### 선택적 환경 변수

```bash
# 의약품 DB API
MFDS_API_URL=https://api.mfds.go.kr
MFDS_API_KEY=your_mfds_api_key

# 이메일
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password

# 콘텐츠 검증
CONTENT_VALIDATION_ENABLED=false

# 서버 포트
SERVER_PORT=8080
```

### 환경 변수 파일 생성

`.env.example` 파일을 참고하여 `.env` 파일을 생성하거나, 배포 환경에서 환경 변수를 설정하세요.

---

## 로컬 빌드 및 실행

### 1. 프로젝트 클론 및 이동

```bash
cd medBE
```

### 2. 환경 변수 설정

```bash
# .env 파일 생성 또는 환경 변수 설정
export med_DB_USERNAME=your_username
export med_DB_PASSWORD=your_password
export JWT_SECRET=your_jwt_secret
export OPENAI_API_KEY=your_openai_key
# ... 기타 환경 변수
```

### 3. 빌드

```bash
# JAR 파일 빌드
./gradlew clean build -x test

# 빌드된 JAR 파일 위치
# build/libs/med-0.0.1-SNAPSHOT.jar
```

### 4. 실행

```bash
# 프로덕션 프로파일로 실행
java -jar build/libs/med-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod

# 또는 Gradle로 실행
./gradlew bootRun --args='--spring.profiles.active=prod'
```

### 5. 헬스체크 확인

```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/actuator/health
```

---

## Docker를 사용한 배포

### 1. Docker 이미지 빌드

```bash
docker build -t med-backend:latest .
```

### 2. 환경 변수 파일 준비

`.env` 파일을 생성하거나 환경 변수를 설정하세요.

### 3. Docker Compose로 실행

```bash
# docker-compose.yml 파일이 있는 디렉토리에서
docker-compose up -d

# 로그 확인
docker-compose logs -f med-be

# 중지
docker-compose down
```

### 4. 단일 컨테이너 실행

```bash
docker run -d \
  --name med-backend \
  -p 8080:8080 \
  -e med_DB_USERNAME=your_username \
  -e med_DB_PASSWORD=your_password \
  -e JWT_SECRET=your_jwt_secret \
  -e OPENAI_API_KEY=your_openai_key \
  -e GOOGLE_APPLICATION_CREDENTIALS=/app/google-credentials.json \
  -v $(pwd)/google-credentials.json:/app/google-credentials.json:ro \
  -v $(pwd)/logs:/var/log/med \
  med-backend:latest
```

---

## AWS 배포

### 옵션 1: AWS Elastic Beanstalk

#### 1. EB CLI 설치

```bash
pip install awsebcli
```

#### 2. EB 초기화

```bash
eb init -p java med-backend
```

#### 3. 환경 변수 설정

```bash
eb setenv \
  med_DB_USERNAME=your_username \
  med_DB_PASSWORD=your_password \
  JWT_SECRET=your_jwt_secret \
  OPENAI_API_KEY=your_openai_key \
  SPRING_PROFILES_ACTIVE=prod
```

#### 4. 배포

```bash
# JAR 파일 빌드
./gradlew clean build -x test

# 배포
eb deploy
```

### 옵션 2: AWS EC2 + Docker

#### 1. EC2 인스턴스 준비

```bash
# Ubuntu 22.04 LTS 권장
# Docker 설치
sudo apt update
sudo apt install docker.io docker-compose -y
sudo systemctl start docker
sudo systemctl enable docker
```

#### 2. 프로젝트 배포

```bash
# Git에서 클론
git clone <your-repo-url>
cd med/medBE

# 환경 변수 설정
nano .env  # 또는 환경 변수 파일 생성

# Docker Compose로 실행
sudo docker-compose up -d
```

#### 3. Nginx 리버스 프록시 설정 (선택적)

```nginx
# /etc/nginx/sites-available/med-backend
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 옵션 3: AWS ECS (Elastic Container Service)

#### 1. ECR에 이미지 푸시

```bash
# ECR 로그인
aws ecr get-login-password --region ap-northeast-2 | \
  docker login --username AWS --password-stdin <account-id>.dkr.ecr.ap-northeast-2.amazonaws.com

# 이미지 태그
docker tag med-backend:latest <account-id>.dkr.ecr.ap-northeast-2.amazonaws.com/med-backend:latest

# 이미지 푸시
docker push <account-id>.dkr.ecr.ap-northeast-2.amazonaws.com/med-backend:latest
```

#### 2. ECS 태스크 정의 생성

환경 변수를 포함한 태스크 정의 JSON 파일을 생성하세요.

#### 3. ECS 서비스 배포

AWS 콘솔 또는 CLI를 통해 ECS 서비스를 생성하고 배포하세요.

---

## 배포 후 확인

### 1. 헬스체크

```bash
# 기본 헬스체크
curl http://your-server:8080/api/health

# Actuator 헬스체크
curl http://your-server:8080/actuator/health

# 예상 응답
{
  "status": "UP",
  "timestamp": "2024-01-01T00:00:00",
  "service": "med-backend"
}
```

### 2. API 테스트

```bash
# 회원가입 테스트
curl -X POST http://your-server:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "test123",
    "email": "test@example.com",
    "nickname": "테스트"
  }'

# 로그인 테스트
curl -X POST http://your-server:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "test123"
  }'
```

### 3. Swagger UI 확인

```
http://your-server:8080/swagger-ui.html
```

### 4. 로그 확인

```bash
# Docker 로그
docker logs med-backend

# 파일 로그
tail -f /var/log/med/application.log
```

---

## 문제 해결

### 데이터베이스 연결 실패

**증상**: `Connection refused` 또는 `Authentication failed`

**해결 방법**:
1. 데이터베이스 호스트 및 포트 확인
2. 사용자명 및 비밀번호 확인
3. 보안 그룹 설정 확인 (AWS RDS)
4. 네트워크 연결 확인

```bash
# 연결 테스트
psql -h your-db-host -U your_username -d postgres
```

### JWT 토큰 오류

**증상**: `JWT signature does not match` 또는 `Invalid token`

**해결 방법**:
1. `JWT_SECRET` 환경 변수 확인
2. 토큰 만료 시간 확인
3. 시크릿 키 길이 확인 (최소 256비트 권장)

### GPT API 오류

**증상**: `GPT API 호출 중 오류 발생`

**해결 방법**:
1. `OPENAI_API_KEY` 환경 변수 확인
2. API 할당량 확인
3. 네트워크 연결 확인
4. API 엔드포인트 URL 확인

### Python 서비스 연결 실패

**증상**: `Python API 서비스에 연결할 수 없습니다`

**해결 방법**:
1. Python 서비스가 실행 중인지 확인
2. `PYTHON_API_URL` 환경 변수 확인
3. 네트워크 연결 확인 (Docker 네트워크)
4. Python 서비스 로그 확인

```bash
# Python 서비스 헬스체크
curl http://localhost:8000/health
```

### Google Vision API 오류

**증상**: `Vision API 권한이 없습니다`

**해결 방법**:
1. `GOOGLE_APPLICATION_CREDENTIALS` 환경 변수 확인
2. 인증 파일 경로 확인
3. 서비스 계정 권한 확인
4. API 활성화 확인 (Google Cloud Console)

### 메모리 부족

**증상**: `OutOfMemoryError`

**해결 방법**:
1. JVM 힙 메모리 증가

```bash
java -Xmx512m -Xms256m -jar app.jar
```

2. Docker 메모리 제한 증가

```yaml
# docker-compose.yml
services:
  med-be:
    deploy:
      resources:
        limits:
          memory: 1G
```

### 포트 충돌

**증상**: `Port 8080 is already in use`

**해결 방법**:
1. 다른 포트 사용

```bash
SERVER_PORT=8081 java -jar app.jar
```

2. 기존 프로세스 종료

```bash
# 포트 사용 중인 프로세스 확인
lsof -i :8080

# 프로세스 종료
kill -9 <PID>
```

---

## 보안 체크리스트

배포 전 다음 사항을 확인하세요:

- [ ] 모든 환경 변수가 설정되어 있는가?
- [ ] JWT_SECRET이 충분히 강력한가? (최소 256비트)
- [ ] 데이터베이스 비밀번호가 안전한가?
- [ ] 프로덕션에서 `spring.jpa.hibernate.ddl-auto=validate`로 설정되어 있는가?
- [ ] `spring.jpa.show-sql=false`로 설정되어 있는가?
- [ ] 에러 메시지에 민감한 정보가 노출되지 않는가?
- [ ] CORS 설정이 적절한가?
- [ ] Swagger UI가 프로덕션에서 비활성화되어 있는가? (선택적)
- [ ] 로그 파일에 민감한 정보가 기록되지 않는가?
- [ ] Google Vision API 인증 파일이 안전하게 관리되는가?

---

## 모니터링 및 로깅

### 로그 위치

- **파일 로그**: `/var/log/med/application.log`
- **Docker 로그**: `docker logs med-backend`
- **Actuator**: `http://your-server:8080/actuator`

### 로그 레벨

프로덕션 환경에서는 다음 로그 레벨을 권장합니다:
- `root`: INFO
- `com.SxxM.med`: INFO
- `org.springframework.web`: WARN
- `org.hibernate`: WARN

### 모니터링 도구 (향후 개선)

- **Prometheus**: 메트릭 수집
- **Grafana**: 대시보드 시각화
- **ELK Stack**: 로그 분석
- **CloudWatch**: AWS 통합 모니터링

---

## 자동 배포 (CI/CD)

### GitHub Actions 예시

`.github/workflows/deploy.yml` 파일을 생성하여 자동 배포를 설정할 수 있습니다.

```yaml
name: Deploy to Production

on:
  push:
    branches: [ main ]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Build with Gradle
        run: ./gradlew clean build -x test
      
      - name: Build Docker image
        run: docker build -t med-backend:${{ github.sha }} .
      
      - name: Deploy to AWS
        # AWS 배포 스크립트 실행
        run: |
          # 배포 로직
```

---

## 롤백 전략

배포 실패 시 롤백 방법:

### Docker

```bash
# 이전 이미지로 롤백
docker tag med-backend:previous med-backend:latest
docker-compose up -d
```

### AWS Elastic Beanstalk

```bash
eb deploy --version previous-version
```

### 수동 롤백

1. 이전 버전의 JAR 파일로 교체
2. 애플리케이션 재시작
3. 헬스체크 확인

---

## 성능 최적화

### JVM 튜닝

```bash
java -jar \
  -Xmx512m \
  -Xms256m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  app.jar
```

### 데이터베이스 연결 풀

`application-prod.properties`에서 HikariCP 설정을 조정하세요.

### 캐싱 (향후 개선)

- Redis 도입 고려
- GPT API 응답 캐싱
- 약물 정보 캐싱

---

## 다음 단계

배포가 완료된 후:

1. **모니터링 설정**: 로그 및 메트릭 수집
2. **백업 전략**: 데이터베이스 정기 백업
3. **스케일링**: 트래픽에 따른 자동 스케일링 설정
4. **CDN 설정**: 정적 리소스 최적화
5. **SSL/TLS**: HTTPS 설정
6. **로드 밸런서**: 다중 인스턴스 배포

---

## 지원

문제가 발생하면:
1. 로그 파일 확인
2. 헬스체크 엔드포인트 확인
3. 환경 변수 확인
4. 네트워크 연결 확인

추가 도움이 필요하면 이슈를 등록하세요.

