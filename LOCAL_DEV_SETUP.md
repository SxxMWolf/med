# 로컬 개발 환경 설정 가이드

이 문서는 로컬 개발 환경으로 전환된 설정에 대한 가이드입니다.

## 📋 변경 사항 요약

배포 관련 설정들을 주석 처리하고 로컬 개발 환경용 설정으로 전환했습니다. 모든 배포 설정은 주석 또는 별도 파일로 보존되어 있어 나중에 다시 배포할 수 있습니다.

## 🔧 변경된 파일 목록

### 백엔드 설정

1. **`medBE/src/main/resources/application.properties`**
   - ✅ AWS RDS 데이터베이스 사용 (로컬 개발 환경에서도 RDS 사용)
   - ✅ 로컬 PostgreSQL 설정은 주석으로 보존 (필요 시 활성화 가능)

2. **`medBE/src/main/java/com/sxxm/med/config/CorsConfig.java`**
   - ✅ Vercel 도메인 허용 제거
   - ✅ 로컬 개발 환경만 허용 (`localhost`, `127.0.0.1`)
   - ✅ 배포 설정은 주석으로 보존

### 프론트엔드 설정

3. **`medFE/vercel.json`**
   - ✅ 배포용 API 프록시 설정 제거
   - ✅ 로컬 개발에서는 Vite 프록시 사용

4. **`medFE/vercel.json.deploy`** (새로 생성)
   - 📝 배포용 설정이 별도 파일로 보존됨
   - 배포 시 이 파일을 `vercel.json`로 복사하여 사용

5. **`medFE/src/api/client.ts`**
   - ✅ `API_BASE_URL`을 빈 문자열로 설정 (Vite 프록시 사용)
   - ✅ 프로덕션 URL 참조 주석 처리

6. **`medFE/src/api/index.ts`**
   - ✅ `API_BASE_URL`을 빈 문자열로 설정 (Vite 프록시 사용)
   - ✅ 프로덕션 URL 참조 주석 처리

7. **`medFE/vite.config.ts`**
   - ✅ 이미 로컬 프록시 설정이 올바르게 구성되어 있음
   - `/api` 요청이 `http://localhost:8080`으로 자동 프록시됨

## 🚀 로컬 개발 환경 실행 방법

### 필수 사항

1. **AWS RDS 데이터베이스 접근**
   - AWS RDS 인스턴스가 실행 중이어야 합니다
   - 로컬 환경에서 RDS로의 네트워크 접근이 가능해야 합니다
   - 환경 변수로 RDS 사용자명/비밀번호 설정 필요:
     ```bash
     export med_DB_USERNAME=your_rds_username
     export med_DB_PASSWORD=your_rds_password
     ```
   - ⚠️ 참고: 로컬 PostgreSQL을 사용하려면 `application.properties`에서 로컬 DB 설정 주석을 해제하고 RDS 설정을 주석 처리하세요.

2. **Python 서비스** (의약품 분석용)
   ```bash
   cd medPY
   # 가상 환경 설정
   python -m venv venv
   source venv/bin/activate  # Windows: venv\Scripts\activate
   
   # 의존성 설치
   pip install -r requirements.txt
   
   # 서비스 실행
   uvicorn app.main:app --reload --port 8000
   ```

### 백엔드 실행

```bash
cd medBE

# 환경 변수 설정 (필수)
export med_DB_USERNAME=your_rds_username
export med_DB_PASSWORD=your_rds_password
export JWT_SECRET=your_jwt_secret_key_minimum_256_bits
export OPENAI_API_KEY=your_openai_api_key
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/google-credentials.json

# Gradle로 실행
./gradlew bootRun

# 또는 빌드 후 실행
./gradlew build
java -jar build/libs/med-0.0.1-SNAPSHOT.jar
```

백엔드는 `http://localhost:8080`에서 실행됩니다.

### 프론트엔드 실행

```bash
cd medFE

# 의존성 설치
npm install

# 개발 서버 실행
npm run dev
```

프론트엔드는 `http://localhost:3000`에서 실행됩니다.

Vite 프록시 설정으로 인해 `/api` 요청은 자동으로 백엔드(`localhost:8080`)로 프록시됩니다.

## 🔄 배포 환경으로 다시 전환하는 방법

### 1. 백엔드 설정

`medBE/src/main/resources/application.properties`에서:
- 배포 설정 주석을 해제하고 로컬 설정을 주석 처리

### 2. 프론트엔드 설정

- `medFE/vercel.json.deploy`를 `vercel.json`로 복사
- `medFE/src/api/client.ts`와 `medFE/src/api/index.ts`에서 배포 설정 주석 해제 및 로컬 설정 주석 처리

### 3. CORS 설정

`medBE/src/main/java/com/sxxm/med/config/CorsConfig.java`에서:
- 배포 환경 CORS 설정 주석을 해제하고 로컬 설정을 주석 처리

## 📝 환경 변수

로컬 개발 시 필요한 주요 환경 변수:

```bash
# 데이터베이스 (AWS RDS - 필수)
med_DB_USERNAME=your_rds_username
med_DB_PASSWORD=your_rds_password

# JWT
JWT_SECRET=your_jwt_secret_key_minimum_256_bits

# OpenAI GPT
OPENAI_API_KEY=your_openai_api_key
GPT_API_URL=https://api.openai.com/v1/chat/completions
GPT_MODEL=gpt-4o-mini

# Google Vision API
GOOGLE_APPLICATION_CREDENTIALS=/path/to/google-credentials.json

# Python API (기본값: localhost:8000)
PYTHON_API_URL=http://localhost:8000
```

## ✅ 확인 사항

로컬 개발 환경이 정상적으로 작동하는지 확인:

1. **백엔드 헬스체크**
   ```bash
   curl http://localhost:8080/api/health
   ```

2. **프론트엔드 접속**
   - 브라우저에서 `http://localhost:3000` 접속

3. **API 프록시 확인**
   - 브라우저 개발자 도구에서 네트워크 탭 확인
   - `/api` 요청이 `localhost:3000`에서 발생하고 자동으로 `localhost:8080`으로 프록시되는지 확인

## 🐛 문제 해결

### CORS 에러가 발생하는 경우
- `CorsConfig.java`에서 로컬 도메인이 올바르게 허용되어 있는지 확인
- 프론트엔드가 `localhost:3000`에서 실행 중인지 확인

### 데이터베이스 연결 실패
- AWS RDS 인스턴스가 실행 중인지 확인 (AWS 콘솔에서 확인)
- 로컬 환경에서 RDS로의 네트워크 접근이 가능한지 확인 (Security Group 설정 확인)
- 환경 변수 `med_DB_USERNAME`, `med_DB_PASSWORD`가 올바르게 설정되어 있는지 확인
- `application.properties`의 데이터베이스 URL이 올바른지 확인
- ⚠️ 참고: 로컬 PostgreSQL을 사용하려면 `application.properties`에서 로컬 DB 설정을 활성화하세요

### API 프록시가 작동하지 않는 경우
- Vite 개발 서버가 실행 중인지 확인
- `vite.config.ts`의 프록시 설정 확인
- 브라우저 콘솔에서 에러 메시지 확인

