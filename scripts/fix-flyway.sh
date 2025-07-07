#!/bin/bash

# Flyway 검증 오류 복구 스크립트
# 사용법: ./fix-flyway.sh [version]

DB_NAME="handy"
DB_USER="root"
DB_PASSWORD=""

echo "🔧 Flyway 검증 오류 복구 스크립트"
echo "=================================="

# 현재 Flyway 상태 확인
echo "📊 현재 Flyway 상태:"
mysql -u $DB_USER -p$DB_PASSWORD $DB_NAME -e "
SELECT version, description, checksum, success 
FROM flyway_schema_history 
ORDER BY installed_rank;
"

echo ""
echo "🔍 실패한 마이그레이션 확인:"
mysql -u $DB_USER -p$DB_PASSWORD $DB_NAME -e "
SELECT version, description, checksum, success 
FROM flyway_schema_history 
WHERE success = 0;
"

# 특정 버전이 지정된 경우
if [ ! -z "$1" ]; then
    VERSION=$1
    echo ""
    echo "🎯 버전 $VERSION 복구 중..."
    
    # 해당 버전의 테이블이 존재하는지 확인
    TABLE_EXISTS=$(mysql -u $DB_USER -p$DB_PASSWORD $DB_NAME -e "
    SELECT COUNT(*) 
    FROM flyway_schema_history 
    WHERE version = '$VERSION' AND success = 0;
    " -s -N)
    
    if [ "$TABLE_EXISTS" -gt 0 ]; then
        # 성공 상태로 변경
        mysql -u $DB_USER -p$DB_PASSWORD $DB_NAME -e "
        UPDATE flyway_schema_history 
        SET success = 1 
        WHERE version = '$VERSION';
        "
        echo "✅ 버전 $VERSION 복구 완료"
    else
        echo "❌ 버전 $VERSION을 찾을 수 없거나 이미 성공 상태입니다"
    fi
else
    echo ""
    echo "🔧 모든 실패한 마이그레이션 복구 중..."
    
    # 모든 실패한 마이그레이션을 성공으로 변경
    mysql -u $DB_USER -p$DB_PASSWORD $DB_NAME -e "
    UPDATE flyway_schema_history 
    SET success = 1 
    WHERE success = 0;
    "
    
    AFFECTED_ROWS=$(mysql -u $DB_USER -p$DB_PASSWORD $DB_NAME -e "
    SELECT ROW_COUNT();
    " -s -N)
    
    echo "✅ $AFFECTED_ROWS 개의 마이그레이션 복구 완료"
fi

echo ""
echo "📊 복구 후 Flyway 상태:"
mysql -u $DB_USER -p$DB_PASSWORD $DB_NAME -e "
SELECT version, description, success 
FROM flyway_schema_history 
ORDER BY installed_rank;
"

echo ""
echo "🎉 Flyway 복구 완료!" 