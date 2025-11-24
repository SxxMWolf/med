#!/bin/bash

# HTTPS 빠른 시작 스크립트
# 도메인이 있으면 Let's Encrypt, 없으면 자체 서명 인증서 사용

set -e

echo "🚀 HTTPS 설정 빠른 시작"
echo "========================"
echo ""

# 도메인 확인
read -p "도메인이 있습니까? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    read -p "도메인을 입력하세요 (예: api.yourdomain.com): " DOMAIN
    read -p "이메일을 입력하세요: " EMAIL
    
    if [ -z "$DOMAIN" ] || [ -z "$EMAIL" ]; then
        echo "❌ 도메인과 이메일을 모두 입력해주세요."
        exit 1
    fi
    
    echo ""
    echo "🔐 Let's Encrypt 인증서 발급 중..."
    ./scripts/setup-ssl.sh "$DOMAIN" "$EMAIL"
    
    # Nginx 설정 파일 업데이트
    sed -i.bak "s/server_name _;/server_name $DOMAIN;/g" nginx/nginx.conf
    echo "✅ Nginx 설정이 업데이트되었습니다."
else
    echo ""
    echo "🔐 자체 서명 인증서 생성 중..."
    ./scripts/setup-ssl.sh
    
    echo ""
    echo "⚠️  자체 서명 인증서는 브라우저에서 보안 경고가 표시됩니다."
    echo "프로덕션 환경에서는 도메인을 사용하는 것을 권장합니다."
fi

echo ""
echo "🐳 Docker Compose 재시작 중..."
cd ..
docker-compose down
docker-compose up -d --build

echo ""
echo "✅ HTTPS 설정이 완료되었습니다!"
echo ""
echo "📋 다음 단계:"
echo "1. 서비스 상태 확인: docker-compose ps"
echo "2. 로그 확인: docker-compose logs nginx"
echo "3. HTTPS 테스트: curl -k https://your-domain/api/health"
echo "4. 프론트엔드 환경변수 업데이트: VITE_API_BASE_URL=https://your-domain"
echo ""

