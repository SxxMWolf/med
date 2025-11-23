# MedPY - Python FastAPI Analysis Service

의약품 분석을 위한 Python 마이크로서비스입니다.

## 설치

```bash
pip install -r requirements.txt
```

## 환경변수 설정

`.env` 파일을 생성하고 다음 변수를 설정하세요:

```bash
cp .env.example .env
```

필수 환경변수:
- `OPENAI_API_KEY`: OpenAI API 키
- `OPENAI_API_URL`: OpenAI API URL (기본값: https://api.openai.com/v1/chat/completions)
- `GPT_MODEL`: 사용할 GPT 모델 (기본값: gpt-4o-mini)

## 실행

```bash
uvicorn app.main:app --reload --port 8000
```

또는 환경변수와 함께:

```bash
uvicorn app.main:app --reload --port 8000 --env-file .env
```

## API 엔드포인트

### 1. 성분 분석
`POST /analyze/ingredients`

### 2. 부작용 분석
`POST /analyze/sideeffects`

### 3. OCR 정규화
`POST /ocr/normalize`

## 문서

FastAPI 자동 생성 문서:
- Swagger UI: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc

