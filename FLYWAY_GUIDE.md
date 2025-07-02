# Flyway 사용 가이드

## 🚨 주의사항

### 절대 하지 말아야 할 것들
1. **기존 마이그레이션 파일 수정 금지** - 체크섬이 변경되어 검증 오류 발생
2. **마이그레이션 파일 삭제 금지** - 데이터베이스와 파일 불일치 발생
3. **마이그레이션 순서 변경 금지** - 의존성 문제 발생

### 올바른 방법
1. **새로운 변경사항**: 새로운 마이그레이션 파일 생성 (V9, V10, ...)
2. **컬럼 추가**: `ALTER TABLE table_name ADD COLUMN IF NOT EXISTS column_name type;`
3. **인덱스 생성**: 별도 마이그레이션 파일로 분리

## 🔧 문제 해결

### Flyway 검증 오류 발생 시

#### 1. 자동 복구 스크립트 사용
```bash
# 모든 실패한 마이그레이션 복구
./scripts/fix-flyway.sh

# 특정 버전 복구
./scripts/fix-flyway.sh 8
```

#### 2. 수동 복구
```sql
-- 실패한 마이그레이션 확인
SELECT version, description, success FROM flyway_schema_history WHERE success = 0;

-- 성공으로 변경
UPDATE flyway_schema_history SET success = 1 WHERE version = '8';
```

#### 3. 체크섬 불일치 해결
```sql
-- 현재 파일의 체크섬으로 업데이트
UPDATE flyway_schema_history SET checksum = NEW_CHECKSUM WHERE version = '8';
```

## 📁 마이그레이션 파일 구조

```
src/main/resources/db/migration/
├── V1__add_auth_level.sql
├── V2__create_base_tables.sql
├── V3__create_order_tables.sql
├── V4__create_delivery_tables.sql
├── V5__create_snap_tables.sql
├── V6__update_snap_posts_user_relation.sql
├── V7__create_likes_table.sql
├── V8__create_comments_and_reports_tables.sql
└── V9__template_example.sql
```

## 🛠️ 개발 환경 설정

### application.yml 설정
```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    validate-on-migrate: false  # 개발 환경에서는 검증 비활성화
    out-of-order: false
    ignore-migration-patterns: "*:missing"
    clean-disabled: true
    locations: classpath:db/migration
```

### 프로덕션 환경 설정
```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    validate-on-migrate: true   # 프로덕션에서는 검증 활성화
    out-of-order: false
    clean-disabled: true
    locations: classpath:db/migration
```

## 📋 마이그레이션 파일 작성 규칙

### 1. 파일명 규칙
```
V{version}__{description}.sql
예: V9__add_user_profile_fields.sql
```

### 2. 안전한 SQL 작성
```sql
-- 테이블 생성
CREATE TABLE IF NOT EXISTS table_name (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 컬럼 추가
ALTER TABLE table_name ADD COLUMN IF NOT EXISTS new_column VARCHAR(100);

-- 인덱스 생성 (별도 파일로 분리)
CREATE INDEX idx_table_name_column ON table_name(column_name);
```

### 3. 롤백 고려
```sql
-- 롤백이 필요한 경우 주석으로 표시
-- ROLLBACK: DROP TABLE IF EXISTS table_name;
```

## 🚀 새로운 마이그레이션 생성 시 체크리스트

- [ ] 파일명이 올바른 형식인가? (V{version}__{description}.sql)
- [ ] IF NOT EXISTS를 사용했는가?
- [ ] 인덱스는 별도 파일로 분리했는가?
- [ ] 기존 파일을 수정하지 않았는가?
- [ ] 테스트 데이터베이스에서 먼저 테스트했는가?

## 📞 문제 발생 시

1. **로그 확인**: `org.flywaydb: DEBUG` 레벨로 로그 확인
2. **스크립트 실행**: `./scripts/fix-flyway.sh` 실행
3. **수동 복구**: 위의 SQL 명령어로 수동 복구
4. **백업 확인**: 문제 발생 전 데이터베이스 백업 확인 