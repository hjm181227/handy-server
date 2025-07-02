-- SnapPost 테이블의 user_id를 외래키로 변경
-- 기존 데이터가 있다면 먼저 백업하고, 외래키 제약조건 추가

-- 1. 외래키 제약조건 추가
ALTER TABLE snap_posts 
ADD CONSTRAINT fk_snap_posts_user 
FOREIGN KEY (user_id) REFERENCES users(id);

-- 2. 인덱스 추가 (성능 향상)
CREATE INDEX idx_snap_posts_user_id ON snap_posts(user_id);
CREATE INDEX idx_snap_posts_created_at ON snap_posts(created_at);
CREATE INDEX idx_snap_posts_is_active ON snap_posts(is_active); 