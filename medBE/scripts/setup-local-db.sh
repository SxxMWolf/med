#!/bin/bash

# 로컬 PostgreSQL 데이터베이스 설정 스크립트
# DB 이름: localMED_DB
# 사용자: sxxm
# 비밀번호: sxxmpass

set -e

DB_NAME="localMED_DB"
DB_USER="sxxm"
DB_PASSWORD="sxxmpass"

echo "=========================================="
echo "로컬 PostgreSQL 데이터베이스 설정 시작"
echo "=========================================="

# PostgreSQL이 실행 중인지 확인
if ! pg_isready -h localhost -p 5432 > /dev/null 2>&1; then
    echo "❌ PostgreSQL이 실행 중이지 않습니다."
    echo "PostgreSQL을 시작한 후 다시 실행해주세요."
    exit 1
fi

echo "✅ PostgreSQL이 실행 중입니다."

# 사용자 생성 (이미 존재하면 무시)
echo "사용자 '$DB_USER' 생성 중..."
psql -h localhost -U postgres -tc "SELECT 1 FROM pg_user WHERE usename = '$DB_USER'" | grep -q 1 || \
    psql -h localhost -U postgres -c "CREATE USER $DB_USER WITH PASSWORD '$DB_PASSWORD';" || \
    echo "⚠️  사용자 '$DB_USER'가 이미 존재하거나 생성에 실패했습니다."

echo "✅ 사용자 '$DB_USER' 설정 완료"

# 데이터베이스 생성 (이미 존재하면 무시)
# PostgreSQL은 따옴표 없이 생성하면 소문자로 변환되므로 따옴표로 감싸야 함
echo "데이터베이스 '$DB_NAME' 생성 중..."
psql -h localhost -U postgres -tc "SELECT 1 FROM pg_database WHERE datname = '$DB_NAME'" | grep -q 1 || \
    psql -h localhost -U postgres -c "CREATE DATABASE \"$DB_NAME\" OWNER $DB_USER;" || \
    echo "⚠️  데이터베이스 '$DB_NAME'가 이미 존재하거나 생성에 실패했습니다."

echo "✅ 데이터베이스 '$DB_NAME' 설정 완료"

# 권한 부여
echo "권한 부여 중..."
psql -h localhost -U postgres -d postgres -c "GRANT ALL PRIVILEGES ON DATABASE \"$DB_NAME\" TO $DB_USER;" || true
psql -h localhost -U postgres -d postgres -c "ALTER DATABASE \"$DB_NAME\" OWNER TO $DB_USER;" || true

echo "✅ 권한 부여 완료"

echo ""
echo "=========================================="
echo "로컬 PostgreSQL 데이터베이스 설정 완료"
echo "=========================================="
echo "DB 이름: $DB_NAME"
echo "사용자: $DB_USER"
echo "비밀번호: $DB_PASSWORD"
echo "연결 URL: jdbc:postgresql://localhost:5432/$DB_NAME"
echo "=========================================="

