#!/bin/bash

echo "=== Python FastAPI 서비스 시작 ==="
echo ""

# 현재 디렉토리로 이동
cd "$(dirname "$0")"

# 가상환경 활성화 확인
if [ -d "venv" ]; then
    echo "✅ 가상환경 발견: venv/"
    source venv/bin/activate
    echo "✅ 가상환경 활성화 완료"
else
    echo "⚠️  가상환경이 없습니다. venv를 생성합니다..."
    python3 -m venv venv
    source venv/bin/activate
    echo "✅ 가상환경 생성 및 활성화 완료"
fi

# requirements.txt 확인
if [ -f "requirements.txt" ]; then
    echo ""
    echo "📦 패키지 설치 확인 중..."
    pip install -q -r requirements.txt
    echo "✅ 패키지 설치 완료"
fi

# .env 파일 확인
if [ ! -f ".env" ]; then
    echo ""
    echo "⚠️  .env 파일이 없습니다."
    echo "💡 OPENAI_API_KEY 등의 환경변수를 설정하세요."
    echo ""
fi

echo ""
echo "🚀 Python FastAPI 서비스 시작..."
echo "📝 Swagger UI: http://localhost:8000/docs"
echo "📝 Health Check: http://localhost:8000/health"
echo ""
echo "종료하려면 Ctrl+C를 누르세요."
echo ""

# 서비스 실행
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

