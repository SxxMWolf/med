#!/bin/bash

# ============================================
# 의약품 분석 플랫폼 배포 스크립트
# ============================================

set -e  # 에러 발생 시 스크립트 중단

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 로그 함수
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 환경 변수 확인
check_env_variables() {
    log_info "환경 변수 확인 중..."
    
    local required_vars=(
        "med_DB_USERNAME"
        "med_DB_PASSWORD"
        "JWT_SECRET"
        "OPENAI_API_KEY"
    )
    
    local missing_vars=()
    
    for var in "${required_vars[@]}"; do
        if [ -z "${!var}" ]; then
            missing_vars+=("$var")
        fi
    done
    
    if [ ${#missing_vars[@]} -ne 0 ]; then
        log_error "다음 환경 변수가 설정되지 않았습니다:"
        for var in "${missing_vars[@]}"; do
            echo "  - $var"
        done
        exit 1
    fi
    
    log_info "모든 필수 환경 변수가 설정되어 있습니다."
}

# 빌드
build() {
    log_info "애플리케이션 빌드 중..."
    ./gradlew clean build -x test
    log_info "빌드 완료"
}

# Docker 이미지 빌드
build_docker() {
    log_info "Docker 이미지 빌드 중..."
    docker build -t med-backend:latest .
    log_info "Docker 이미지 빌드 완료"
}

# Docker Compose로 배포
deploy_docker_compose() {
    log_info "Docker Compose로 배포 중..."
    docker-compose up -d
    log_info "배포 완료"
    
    # 헬스체크 대기
    log_info "헬스체크 대기 중..."
    sleep 10
    
    # 헬스체크 확인
    if curl -f http://localhost:8080/api/health > /dev/null 2>&1; then
        log_info "헬스체크 성공"
    else
        log_warn "헬스체크 실패 (서비스가 아직 시작 중일 수 있습니다)"
    fi
}

# JAR 파일 직접 실행
deploy_jar() {
    log_info "JAR 파일로 배포 중..."
    
    local jar_file="build/libs/med-0.0.1-SNAPSHOT.jar"
    
    if [ ! -f "$jar_file" ]; then
        log_error "JAR 파일을 찾을 수 없습니다: $jar_file"
        log_info "먼저 빌드를 실행하세요: ./deploy.sh build"
        exit 1
    fi
    
    # 기존 프로세스 종료 (있는 경우)
    if pgrep -f "med-0.0.1-SNAPSHOT.jar" > /dev/null; then
        log_warn "기존 프로세스 종료 중..."
        pkill -f "med-0.0.1-SNAPSHOT.jar"
        sleep 2
    fi
    
    # 백그라운드로 실행
    nohup java -jar \
        -Dspring.profiles.active=prod \
        "$jar_file" > /var/log/med/application.log 2>&1 &
    
    local pid=$!
    log_info "애플리케이션이 시작되었습니다. PID: $pid"
    
    # 헬스체크 대기
    log_info "헬스체크 대기 중..."
    sleep 10
    
    # 헬스체크 확인
    if curl -f http://localhost:8080/api/health > /dev/null 2>&1; then
        log_info "헬스체크 성공"
    else
        log_warn "헬스체크 실패 (서비스가 아직 시작 중일 수 있습니다)"
    fi
}

# 로그 확인
show_logs() {
    log_info "로그 확인 중..."
    
    if command -v docker-compose &> /dev/null && docker-compose ps | grep -q med-be; then
        docker-compose logs -f med-be
    elif [ -f "/var/log/med/application.log" ]; then
        tail -f /var/log/med/application.log
    else
        log_warn "로그 파일을 찾을 수 없습니다"
    fi
}

# 헬스체크
health_check() {
    log_info "헬스체크 실행 중..."
    
    local health_url="http://localhost:8080/api/health"
    local actuator_url="http://localhost:8080/actuator/health"
    
    if curl -f "$health_url" > /dev/null 2>&1; then
        log_info "기본 헬스체크: OK"
        curl -s "$health_url" | jq '.' 2>/dev/null || curl -s "$health_url"
    else
        log_error "기본 헬스체크: FAILED"
    fi
    
    if curl -f "$actuator_url" > /dev/null 2>&1; then
        log_info "Actuator 헬스체크: OK"
        curl -s "$actuator_url" | jq '.' 2>/dev/null || curl -s "$actuator_url"
    else
        log_warn "Actuator 헬스체크: 사용 불가 (Actuator가 설정되지 않았을 수 있습니다)"
    fi
}

# 중지
stop() {
    log_info "애플리케이션 중지 중..."
    
    if command -v docker-compose &> /dev/null && docker-compose ps | grep -q med-be; then
        docker-compose down
        log_info "Docker Compose로 중지 완료"
    elif pgrep -f "med-0.0.1-SNAPSHOT.jar" > /dev/null; then
        pkill -f "med-0.0.1-SNAPSHOT.jar"
        log_info "JAR 프로세스 종료 완료"
    else
        log_warn "실행 중인 애플리케이션을 찾을 수 없습니다"
    fi
}

# 사용법 출력
usage() {
    echo "사용법: $0 [명령어]"
    echo ""
    echo "명령어:"
    echo "  check-env      환경 변수 확인"
    echo "  build          애플리케이션 빌드"
    echo "  build-docker   Docker 이미지 빌드"
    echo "  deploy-jar     JAR 파일로 배포"
    echo "  deploy-docker  Docker Compose로 배포"
    echo "  logs           로그 확인"
    echo "  health         헬스체크"
    echo "  stop           애플리케이션 중지"
    echo "  all            빌드 및 배포 (Docker Compose)"
    echo ""
    echo "예시:"
    echo "  $0 check-env"
    echo "  $0 build"
    echo "  $0 deploy-docker"
    echo "  $0 all"
}

# 메인 로직
main() {
    case "${1:-}" in
        check-env)
            check_env_variables
            ;;
        build)
            build
            ;;
        build-docker)
            build_docker
            ;;
        deploy-jar)
            check_env_variables
            deploy_jar
            ;;
        deploy-docker)
            check_env_variables
            build_docker
            deploy_docker_compose
            ;;
        logs)
            show_logs
            ;;
        health)
            health_check
            ;;
        stop)
            stop
            ;;
        all)
            check_env_variables
            build_docker
            deploy_docker_compose
            ;;
        *)
            usage
            exit 1
            ;;
    esac
}

main "$@"

