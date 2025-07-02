-- 좋아요 테이블 생성 (이미 존재하면 생성하지 않음)
CREATE TABLE IF NOT EXISTS likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_id BIGINT NOT NULL,
    target_type VARCHAR(10) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY unique_user_target (user_id, target_id, target_type)
);

-- 인덱스 추가 (성능 향상) - 이미 존재하면 생성하지 않음
CREATE INDEX IF NOT EXISTS idx_likes_target_id_type ON likes(target_id, target_type);
CREATE INDEX IF NOT EXISTS idx_likes_user_id ON likes(user_id); 