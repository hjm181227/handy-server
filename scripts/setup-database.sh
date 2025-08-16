#!/bin/bash

# EC2 인스턴스 데이터베이스 설정 스크립트
# 사용법: ./scripts/setup-database.sh [instance-id] [environment]

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

INSTANCE_ID=${1:-}
ENVIRONMENT=${2:-prod}

if [ -z "$INSTANCE_ID" ]; then
    log_error "EC2 인스턴스 ID가 필요합니다."
    log_info "사용법: $0 [instance-id] [environment]"
    exit 1
fi

# 환경별 설정
case $ENVIRONMENT in
    "dev")
        DB_NAME="handy_dev"
        DB_USER="handy_dev_user"
        DB_PASSWORD="handy_dev_1234!"
        ;;
    "prod")
        DB_NAME="handy_prod"
        DB_USER="handy_prod_user"
        DB_PASSWORD="handy_prod_1234!"
        ;;
    *)
        log_error "지원하지 않는 환경입니다: $ENVIRONMENT"
        log_info "사용법: $0 [instance-id] [dev|prod]"
        exit 1
        ;;
esac

log_info "데이터베이스 설정 시작: $ENVIRONMENT 환경, 인스턴스: $INSTANCE_ID"

# EC2 인스턴스에 접속하여 데이터베이스 설정
ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -i ~/.ssh/handy-app-server.pem ec2-user@15.165.22.118 << EOF
    set -e
    
    log_info() {
        echo -e "\033[0;32m[INFO]\033[0m \$1"
    }
    
    log_warn() {
        echo -e "\033[1;33m[WARN]\033[0m \$1"
    }
    
    log_error() {
        echo -e "\033[0;31m[ERROR]\033[0m \$1"
    }
    
    # 1. MySQL 서비스 상태 확인
    log_info "MySQL 서비스 상태 확인 중..."
    if ! sudo systemctl is-active --quiet mysql; then
        log_error "MySQL이 실행되지 않고 있습니다."
        exit 1
    fi
    
    # 2. MySQL root 비밀번호 확인
    log_info "MySQL root 비밀번호 확인 중..."
    if ! mysql -u root -p'root1234!' -e "SELECT 1;" 2>/dev/null; then
        log_info "MySQL root 비밀번호를 설정합니다."
        sudo mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED BY 'root1234!';"
        sudo mysql -e "FLUSH PRIVILEGES;"
    else
        log_info "MySQL root 비밀번호가 이미 설정되어 있습니다."
    fi
    
    # 3. 데이터베이스 생성
    log_info "데이터베이스 생성 중: $DB_NAME"
    mysql -u root -p'root1234!' -e "CREATE DATABASE IF NOT EXISTS $DB_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    
    # 4. 사용자 생성 및 권한 부여
    log_info "데이터베이스 사용자 생성 중: $DB_USER"
    mysql -u root -p'root1234!' -e "CREATE USER IF NOT EXISTS '$DB_USER'@'localhost' IDENTIFIED BY '$DB_PASSWORD';"
    mysql -u root -p'root1234!' -e "CREATE USER IF NOT EXISTS '$DB_USER'@'%' IDENTIFIED BY '$DB_PASSWORD';"
    mysql -u root -p'root1234!' -e "GRANT ALL PRIVILEGES ON $DB_NAME.* TO '$DB_USER'@'localhost';"
    mysql -u root -p'root1234!' -e "GRANT ALL PRIVILEGES ON $DB_NAME.* TO '$DB_USER'@'%';"
    mysql -u root -p'root1234!' -e "FLUSH PRIVILEGES;"
    
    # 5. 데이터베이스 연결 테스트
    log_info "데이터베이스 연결 테스트 중..."
    if mysql -u $DB_USER -p$DB_PASSWORD -h localhost $DB_NAME -e "SELECT 1;" 2>/dev/null; then
        log_info "데이터베이스 연결 성공!"
    else
        log_error "데이터베이스 연결 실패"
        exit 1
    fi
    
    # 6. MySQL 설정 최적화 (Amazon Linux 2023용)
    log_info "MySQL 설정 최적화 중..."
    sudo mkdir -p /etc/my.cnf.d
    sudo tee /etc/my.cnf.d/custom.cnf << 'MYSQL_EOF'
[mysqld]
# 기본 문자셋 설정
character-set-server = utf8mb4
collation-server = utf8mb4_unicode_ci

# 연결 설정
max_connections = 200
max_connect_errors = 100000

# 버퍼 설정
innodb_buffer_pool_size = 256M
innodb_log_file_size = 64M
innodb_flush_log_at_trx_commit = 2

# 쿼리 캐시 설정
query_cache_type = 1
query_cache_size = 32M

# 로그 설정
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow.log
long_query_time = 2

# 보안 설정
local_infile = 0
MYSQL_EOF
    
    # 7. MySQL 재시작
    log_info "MySQL 재시작 중..."
    sudo systemctl restart mysql
    
    # 8. 환경 변수 파일 업데이트
    log_info "환경 변수 파일 업데이트 중..."
    sudo tee /etc/profile.d/handy-server.sh << 'ENV_EOF'
# Database Configuration
DB_HOST=localhost
DB_PORT=3306
DB_NAME=$DB_NAME
DB_USERNAME=$DB_USER
DB_PASSWORD=$DB_PASSWORD

# JWT Configuration
JWT_SECRET=your-super-secret-jwt-key-should-be-at-least-32-characters-long

# AWS Configuration
AWS_ACCESS_KEY=your_aws_access_key
AWS_SECRET_KEY=your_aws_secret_key
AWS_REGION=ap-northeast-2
AWS_S3_BUCKET=your-s3-bucket-name

# Application Configuration
SPRING_PROFILES_ACTIVE=$ENVIRONMENT
ENV_EOF
    
    # 9. 데이터베이스 백업 스크립트 생성
    log_info "데이터베이스 백업 스크립트 생성 중..."
    sudo tee /home/ec2-user/backup-database.sh << 'BACKUP_EOF'
#!/bin/bash

# 데이터베이스 백업 스크립트
BACKUP_DIR="/home/ec2-user/backups"
DATE=\$(date +%Y%m%d_%H%M%S)
DB_NAME="$DB_NAME"
DB_USER="$DB_USER"
DB_PASSWORD="$DB_PASSWORD"

# 백업 디렉토리 생성
mkdir -p \$BACKUP_DIR

# 데이터베이스 백업
mysqldump -u \$DB_USER -p\$DB_PASSWORD \$DB_NAME > \$BACKUP_DIR/\${DB_NAME}_\${DATE}.sql

# 7일 이상 된 백업 파일 삭제
find \$BACKUP_DIR -name "*.sql" -mtime +7 -delete

echo "백업 완료: \$BACKUP_DIR/\${DB_NAME}_\${DATE}.sql"
BACKUP_EOF
    
    sudo chmod +x /home/ec2-user/backup-database.sh
    
    # 10. 자동 백업 cron 작업 설정
    log_info "자동 백업 cron 작업 설정 중..."
    # cronie 패키지 설치 (Amazon Linux 2023용)
    sudo dnf install -y cronie
    sudo systemctl start crond
    sudo systemctl enable crond
    (crontab -l 2>/dev/null; echo "0 2 * * * /home/ec2-user/backup-database.sh") | crontab -
    
    # 11. 데이터베이스 모니터링 스크립트 생성
    log_info "데이터베이스 모니터링 스크립트 생성 중..."
    sudo tee /home/ec2-user/monitor-database.sh << 'MONITOR_EOF'
#!/bin/bash

# 데이터베이스 모니터링 스크립트
DB_NAME="$DB_NAME"
DB_USER="$DB_USER"
DB_PASSWORD="$DB_PASSWORD"

echo "=== 데이터베이스 상태 ==="
echo "MySQL 서비스 상태:"
sudo systemctl status mysql --no-pager

echo -e "\n=== 데이터베이스 연결 테스트 ==="
if mysql -u \$DB_USER -p\$DB_PASSWORD -h localhost \$DB_NAME -e "SELECT 1;" 2>/dev/null; then
    echo "✓ 데이터베이스 연결 성공"
else
    echo "✗ 데이터베이스 연결 실패"
fi

echo -e "\n=== 데이터베이스 정보 ==="
mysql -u \$DB_USER -p\$DB_PASSWORD -h localhost \$DB_NAME -e "
SHOW VARIABLES LIKE 'max_connections';
SHOW VARIABLES LIKE 'innodb_buffer_pool_size';
SHOW STATUS LIKE 'Threads_connected';
SHOW STATUS LIKE 'Uptime';
"

echo -e "\n=== 테이블 목록 ==="
mysql -u \$DB_USER -p\$DB_PASSWORD -h localhost \$DB_NAME -e "SHOW TABLES;"
MONITOR_EOF
    
    sudo chmod +x /home/ec2-user/monitor-database.sh
    
    # 12. 최종 확인
    log_info "데이터베이스 설정 완료!"
    log_info "생성된 데이터베이스: $DB_NAME"
    log_info "생성된 사용자: $DB_USER"
    log_info "백업 스크립트: /home/ec2-user/backup-database.sh"
    log_info "모니터링 스크립트: /home/ec2-user/monitor-database.sh"
    
        # 13. 데이터베이스 상태 출력
    echo -e "\n=== 최종 데이터베이스 상태 ==="
    mysql -u $DB_USER -p$DB_PASSWORD -h localhost $DB_NAME -e "
    SELECT 'Database Info' as info;
    SELECT DATABASE() as current_database;
    SELECT USER() as current_user_name;
    SELECT VERSION() as mysql_version;
    "
EOF

if [ $? -eq 0 ]; then
    log_info "데이터베이스 설정 완료!"
    log_info "다음 단계:"
    log_info "1. 애플리케이션 배포: ./scripts/deploy.sh $ENVIRONMENT $INSTANCE_ID"
    log_info "2. 데이터베이스 모니터링: ssh -i ~/.ssh/handy-app-server.pem ec2-user@15.165.22.118 '/home/ec2-user/monitor-database.sh'"
    log_info "3. 수동 백업: ssh -i ~/.ssh/handy-app-server.pem ec2-user@15.165.22.118 '/home/ec2-user/backup-database.sh'"
else
    log_error "데이터베이스 설정 실패"
    exit 1
fi
