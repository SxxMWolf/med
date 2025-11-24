#!/bin/bash

# SSL 인증서 설정 스크립트
# 사용법: ./setup-ssl.sh [도메인] [이메일]

set -e

DOMAIN="${1:-}"
EMAIL="${2:-your-email@example.com}"

if [ -z "$DOMAIN" ]; then
    echo "⚠️  도메인이 제공되지 않았습니다."
    echo "사용법: ./setup-ssl.sh <도메인> [이메일]"
    echo ""
    echo "도메인이 없는 경우 자체 서명 인증서를 생성합니다."
    read -p "자체 서명 인증서를 생성하시겠습니까? (y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
    
    # 자체 서명 인증서 생성 (개발/테스트용)
    echo "🔐 자체 서명 인증서 생성 중..."
    mkdir -p nginx/ssl
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
        -keyout nginx/ssl/privkey.pem \
        -out nginx/ssl/fullchain.pem \
        -subj "/C=KR/ST=Seoul/L=Seoul/O=Med/CN=localhost"
    
    echo "✅ 자체 서명 인증서가 생성되었습니다."
    echo "⚠️  브라우저에서 보안 경고가 표시될 수 있습니다 (개발/테스트용)."
    exit 0
fi

echo "🔐 Let's Encrypt SSL 인증서 설정 중..."
echo "도메인: $DOMAIN"
echo "이메일: $EMAIL"

# Certbot 설치 확인
if ! command -v certbot &> /dev/null; then
    echo "📦 Certbot 설치 중..."
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        if command -v apt-get &> /dev/null; then
            sudo apt-get update
            sudo apt-get install -y certbot
        elif command -v yum &> /dev/null; then
            sudo yum install -y certbot
        else
            echo "❌ 패키지 매니저를 찾을 수 없습니다. 수동으로 Certbot을 설치해주세요."
            exit 1
        fi
    else
        echo "❌ 이 스크립트는 Linux에서만 실행할 수 있습니다."
        exit 1
    fi
fi

# SSL 인증서 디렉토리 생성
mkdir -p nginx/ssl
mkdir -p /var/www/certbot

# Let's Encrypt 인증서 발급
echo "📜 Let's Encrypt 인증서 발급 중..."
sudo certbot certonly --standalone \
    --non-interactive \
    --agree-tos \
    --email "$EMAIL" \
    -d "$DOMAIN" \
    --preferred-challenges http

# 인증서 파일 복사
if [ -f "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" ]; then
    sudo cp /etc/letsencrypt/live/$DOMAIN/fullchain.pem nginx/ssl/fullchain.pem
    sudo cp /etc/letsencrypt/live/$DOMAIN/privkey.pem nginx/ssl/privkey.pem
    sudo chmod 644 nginx/ssl/fullchain.pem
    sudo chmod 600 nginx/ssl/privkey.pem
    
    echo "✅ SSL 인증서가 성공적으로 설정되었습니다!"
    echo "📁 인증서 위치: nginx/ssl/"
    echo ""
    echo "🔄 인증서 자동 갱신 설정:"
    echo "sudo certbot renew --dry-run"
    echo ""
    echo "📝 crontab에 자동 갱신 추가:"
    echo "0 0 * * * certbot renew --quiet && docker-compose restart nginx"
else
    echo "❌ 인증서 발급에 실패했습니다."
    exit 1
fi

