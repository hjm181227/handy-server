-- 마이그레이션 파일 템플릿
-- 파일명: V{version}__{description}.sql
-- 예시: V9__add_user_profile_fields.sql

-- 주의사항:
-- 1. 한 번 생성된 마이그레이션 파일은 절대 수정하지 마세요
-- 2. 새로운 변경사항이 필요하면 새로운 마이그레이션 파일을 생성하세요
-- 3. CREATE INDEX는 별도의 마이그레이션 파일로 분리하세요
-- 4. IF NOT EXISTS를 사용하여 안전하게 테이블을 생성하세요

-- 테이블 생성 예시
CREATE TABLE IF NOT EXISTS example_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 컬럼 추가 예시 (새로운 마이그레이션 파일에서)
-- ALTER TABLE example_table ADD COLUMN IF NOT EXISTS new_column VARCHAR(100);

-- 인덱스 생성은 별도 마이그레이션 파일로 분리
-- CREATE INDEX idx_example_name ON example_table(name); 