#!/bin/bash

# 로컬 배포 자동화 스크립트
# 사용법: ./deploy-local.sh [stage|prod]

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

# .env 파일 로드
if [ -f .env ]; then
    log_info ".env 파일 로드 중..."
    set -a
    source .env
    set +a
fi

# 환경 변수 설정
ENVIRONMENT=${1:-prod}
EC2_HOST="15.165.22.118"
EC2_USER="ec2-user"
KEY_PATH="~/.ssh/handy-app-server.pem"
REMOTE_DIR="/home/ec2-user/handy-server"
JAR_NAME="app-server-0.0.1-SNAPSHOT.jar"

# 환경별 설정
case $ENVIRONMENT in
    "stage")
        PROFILE="dev"
        DB_NAME="handy_stage"
        DB_USER="handy_stage_user"
        DB_PASSWORD="handy_stage_1234!"
        SERVICE_NAME="handy-server-stage"
        SERVICE_PORT="8081"
        ;;
    "prod")
        PROFILE="prod"
        DB_NAME="handy_prod"
        DB_USER="handy_prod_user"
        DB_PASSWORD="handy_prod_1234!"
        SERVICE_NAME="handy-server-prod"
        SERVICE_PORT="8080"
        ;;
    *)
        log_error "지원하지 않는 환경입니다: $ENVIRONMENT"
        log_info "사용법: $0 [stage|prod]"
        log_info "  stage : 개발/테스트 환경 (포트 8081, handy_stage 데이터베이스)"
        log_info "  prod  : 프로덕션 환경 (포트 8080, handy_prod 데이터베이스)"
        exit 1
        ;;
esac

log_info "🚀 로컬 배포 시작: $ENVIRONMENT 환경"
log_info "📋 환경 정보:"
log_info "  - 프로필: $PROFILE"
log_info "  - 데이터베이스: $DB_NAME"
log_info "  - 서비스: $SERVICE_NAME"
log_info "  - 포트: $SERVICE_PORT"

# 1. 사전 검사
log_step "1. 사전 검사 실행"

# SSH 키 파일 확인
if [ ! -f ~/.ssh/handy-app-server.pem ]; then
    log_error "SSH 키 파일을 찾을 수 없습니다: ~/.ssh/handy-app-server.pem"
    exit 1
fi

# EC2 연결 테스트
log_info "EC2 인스턴스 연결 테스트 중..."
if ! ssh -o ConnectTimeout=10 -o StrictHostKeyChecking=no -i ~/.ssh/handy-app-server.pem $EC2_USER@$EC2_HOST "echo 'Connection test successful'" > /dev/null 2>&1; then
    log_error "EC2 인스턴스에 연결할 수 없습니다: $EC2_HOST"
    exit 1
fi
log_info "EC2 연결 성공"

# 2. 프로젝트 빌드
log_step "2. 프로젝트 빌드"
log_info "테스트 및 빌드 실행 중..."

./gradlew clean test build

if [ $? -ne 0 ]; then
    log_error "빌드 또는 테스트 실패"
    exit 1
fi

# JAR 파일 확인
JAR_PATH="build/libs/$JAR_NAME"
if [ ! -f "$JAR_PATH" ]; then
    log_error "JAR 파일을 찾을 수 없습니다: $JAR_PATH"
    exit 1
fi

JAR_SIZE=$(du -h "$JAR_PATH" | cut -f1)
log_info "빌드 완료 - JAR 파일 크기: $JAR_SIZE"

# 3. 데이터베이스 백업 (프로덕션만)
if [ "$ENVIRONMENT" = "prod" ]; then
    log_step "3. 프로덕션 데이터베이스 백업"
    log_info "기존 데이터베이스 백업 중..."
    
    ssh -i ~/.ssh/handy-app-server.pem $EC2_USER@$EC2_HOST << EOF
        # 백업 디렉토리 생성
        mkdir -p /home/ec2-user/backups
        
        # 데이터베이스 백업 (데이터베이스가 존재하는 경우에만)
        if mysql -u $DB_USER -p$DB_PASSWORD -h localhost $DB_NAME -e "SELECT 1;" 2>/dev/null; then
            BACKUP_DATE=\$(date +%Y%m%d_%H%M%S)
            BACKUP_FILE="/home/ec2-user/backups/${DB_NAME}_backup_\${BACKUP_DATE}.sql"
            mysqldump -u $DB_USER -p$DB_PASSWORD $DB_NAME > \$BACKUP_FILE
            echo "백업 완료: \$BACKUP_FILE"
        else
            echo "백업할 데이터베이스가 없거나 접근할 수 없습니다."
        fi
EOF
    log_info "데이터베이스 백업 완료"
else
    log_step "3. 백업 단계 건너뜀 (스테이지 환경)"
fi

# 4. 파일 전송
log_step "4. JAR 파일 전송"
log_info "JAR 파일을 EC2로 전송 중..."

scp -i ~/.ssh/handy-app-server.pem "$JAR_PATH" $EC2_USER@$EC2_HOST:$REMOTE_DIR/

if [ $? -ne 0 ]; then
    log_error "파일 전송 실패"
    exit 1
fi
log_info "파일 전송 완료"

# 5. 원격 배포
log_step "5. EC2 인스턴스 배포"
log_info "원격 서버에 배포 중..."

ssh -i ~/.ssh/handy-app-server.pem $EC2_USER@$EC2_HOST << EOF
    set -e
    
    log_info() {
        echo -e "\033[0;32m[INFO]\033[0m \$1"
    }
    
    log_error() {
        echo -e "\033[0;31m[ERROR]\033[0m \$1"
    }
    
    # 원격 디렉토리 생성
    mkdir -p $REMOTE_DIR
    cd $REMOTE_DIR
    
    # 기존 서비스 중지
    log_info "기존 서비스 중지 중..."
    sudo systemctl stop $SERVICE_NAME 2>/dev/null || true
    
    # systemd 서비스 파일 생성/업데이트
    log_info "systemd 서비스 파일 생성 중..."
    sudo tee /etc/systemd/system/$SERVICE_NAME.service > /dev/null << 'SERVICE_EOF'
[Unit]
Description=Handy Server ($ENVIRONMENT)
After=network.target mysql.service
Wants=mysql.service

[Service]
Type=simple
User=ec2-user
WorkingDirectory=$REMOTE_DIR
ExecStart=/usr/bin/java -jar -Dspring.profiles.active=$PROFILE -Dserver.port=$SERVICE_PORT $JAR_NAME
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

# 환경 변수
Environment="DB_HOST=localhost"
Environment="DB_PORT=3306"
Environment="DB_NAME=$DB_NAME"
Environment="DB_USERNAME=$DB_USER"
Environment="DB_PASSWORD=$DB_PASSWORD"
Environment="AWS_ACCESS_KEY=$AWS_ACCESS_KEY"
Environment="AWS_SECRET_KEY=$AWS_SECRET_KEY"
Environment="AWS_REGION=$AWS_REGION"
Environment="AWS_S3_BUCKET=$AWS_S3_BUCKET"
Environment="JWT_SECRET=$JWT_SECRET"
Environment="SPRING_JPA_HIBERNATE_DDL_AUTO=create"
Environment="JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC"

[Install]
WantedBy=multi-user.target
SERVICE_EOF
    
    # systemd 데몬 재로드
    log_info "systemd 데몬 재로드 중..."
    sudo systemctl daemon-reload
    sudo systemctl enable $SERVICE_NAME
    
    # 서비스 시작
    log_info "서비스 시작 중..."
    sudo systemctl start $SERVICE_NAME
    
    # 서비스 상태 확인
    sleep 3
    if sudo systemctl is-active --quiet $SERVICE_NAME; then
        log_info "서비스가 성공적으로 시작되었습니다."
    else
        log_error "서비스 시작 실패"
        sudo systemctl status $SERVICE_NAME --no-pager
        exit 1
    fi
EOF

if [ $? -ne 0 ]; then
    log_error "원격 배포 실패"
    exit 1
fi

# 6. 헬스체크
log_step "6. 서비스 헬스체크"
log_info "서비스 상태 확인 중..."

for i in {1..60}; do
    if ssh -i ~/.ssh/handy-app-server.pem $EC2_USER@$EC2_HOST "curl -f http://localhost:$SERVICE_PORT/actuator/health" > /dev/null 2>&1; then
        log_info "✅ 서비스가 정상적으로 실행 중입니다!"
        break
    fi
    if [ $i -eq 60 ]; then
        log_error "❌ 서비스 헬스체크 타임아웃"
        log_info "서비스 로그 확인:"
        ssh -i ~/.ssh/handy-app-server.pem $EC2_USER@$EC2_HOST "sudo journalctl -u $SERVICE_NAME --no-pager -n 20"
        exit 1
    fi
    echo -n "."
    sleep 2
done

# 7. 배포 완료 정보
log_step "7. 배포 완료"
log_info "🎉 배포가 성공적으로 완료되었습니다!"
log_info ""
log_info "📋 배포 정보:"
log_info "  - 환경: $ENVIRONMENT"
log_info "  - 서비스: $SERVICE_NAME"
log_info "  - URL: http://$EC2_HOST:$SERVICE_PORT"
log_info "  - Health Check: http://$EC2_HOST:$SERVICE_PORT/actuator/health"
log_info ""
log_info "🔧 유용한 명령어들:"
log_info "  - 서비스 상태 확인: ssh -i ~/.ssh/handy-app-server.pem $EC2_USER@$EC2_HOST 'sudo systemctl status $SERVICE_NAME'"
log_info "  - 서비스 로그 확인: ssh -i ~/.ssh/handy-app-server.pem $EC2_USER@$EC2_HOST 'sudo journalctl -u $SERVICE_NAME -f'"
log_info "  - 서비스 재시작: ssh -i ~/.ssh/handy-app-server.pem $EC2_USER@$EC2_HOST 'sudo systemctl restart $SERVICE_NAME'"

# 현재 실행 중인 서비스들 표시
log_info ""
log_info "🔍 현재 실행 중인 Handy 서비스들:"
ssh -i ~/.ssh/handy-app-server.pem $EC2_USER@$EC2_HOST "sudo systemctl list-units --type=service --state=running | grep handy || echo '  실행 중인 Handy 서비스가 없습니다.'"