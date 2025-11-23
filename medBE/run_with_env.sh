#!/bin/bash

# 환경변수 설정
export DB_PASSWORD='$Mem0t10na1'
export DB_USERNAME=sxxm

# 다른 환경변수들도 설정 (있는 경우)
if [ -n "$JWT_SECRET" ]; then
    export JWT_SECRET="$JWT_SECRET"
fi

echo "=== 환경변수 설정 ==="
echo "DB_USERNAME: $DB_USERNAME"
echo "DB_PASSWORD: 설정됨 (길이: ${#DB_PASSWORD} 문자)"
echo ""

echo "=== Med 애플리케이션 시작 ==="
echo "Swagger UI: http://localhost:8080/swagger-ui.html"
echo ""
echo "종료하려면 Ctrl+C를 누르세요."
echo ""

# 환경변수를 함께 전달하여 실행
./gradlew bootRun

