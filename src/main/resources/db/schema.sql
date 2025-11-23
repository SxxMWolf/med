-- ============================================
-- 의약품 분석 및 커뮤니티 시스템 데이터베이스 스키마
-- PostgreSQL
-- ============================================

-- 사용자 테이블
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    nickname VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 사용자 알러지 테이블
CREATE TABLE IF NOT EXISTS user_allergies (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    ingredient_name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    severity VARCHAR(20) CHECK (severity IN ('MILD', 'MODERATE', 'SEVERE')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_allergy_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 부작용 보고서 테이블
CREATE TABLE IF NOT EXISTS side_effect_reports (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    description VARCHAR(2000),
    analysis_result TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_side_effect_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 부작용 약물 목록 테이블 (ElementCollection)
CREATE TABLE IF NOT EXISTS side_effect_medications (
    report_id BIGINT NOT NULL,
    medication_name VARCHAR(255) NOT NULL,
    CONSTRAINT fk_medication_report FOREIGN KEY (report_id) REFERENCES side_effect_reports(id) ON DELETE CASCADE,
    PRIMARY KEY (report_id, medication_name)
);

-- OCR 성분 테이블
CREATE TABLE IF NOT EXISTS ocr_ingredients (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    image_url VARCHAR(1000),
    ocr_text TEXT,
    analysis_result TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ocr_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- OCR 성분 목록 테이블 (ElementCollection)
CREATE TABLE IF NOT EXISTS ocr_ingredient_list (
    ocr_id BIGINT NOT NULL,
    ingredient_name VARCHAR(255) NOT NULL,
    CONSTRAINT fk_ingredient_list_ocr FOREIGN KEY (ocr_id) REFERENCES ocr_ingredients(id) ON DELETE CASCADE,
    PRIMARY KEY (ocr_id, ingredient_name)
);

-- 게시글 테이블
CREATE TABLE IF NOT EXISTS posts (
    id BIGSERIAL PRIMARY KEY,
    author_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    category VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_post_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 댓글 테이블
CREATE TABLE IF NOT EXISTS comments (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comment_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================
-- 인덱스 생성
-- ============================================

-- 사용자 테이블 인덱스
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- 사용자 알러지 테이블 인덱스
CREATE INDEX IF NOT EXISTS idx_user_allergies_user_id ON user_allergies(user_id);

-- 부작용 보고서 테이블 인덱스
CREATE INDEX IF NOT EXISTS idx_side_effect_reports_user_id ON side_effect_reports(user_id);

-- OCR 성분 테이블 인덱스
CREATE INDEX IF NOT EXISTS idx_ocr_ingredients_user_id ON ocr_ingredients(user_id);

-- 게시글 테이블 인덱스
CREATE INDEX IF NOT EXISTS idx_posts_author_id ON posts(author_id);
CREATE INDEX IF NOT EXISTS idx_posts_category ON posts(category);
CREATE INDEX IF NOT EXISTS idx_posts_created_at ON posts(created_at DESC);

-- 댓글 테이블 인덱스
CREATE INDEX IF NOT EXISTS idx_comments_post_id ON comments(post_id);
CREATE INDEX IF NOT EXISTS idx_comments_author_id ON comments(author_id);
CREATE INDEX IF NOT EXISTS idx_comments_created_at ON comments(created_at DESC);

-- ============================================
-- 트리거 생성 (updated_at 자동 업데이트)
-- ============================================

-- updated_at 자동 업데이트 함수
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- users 테이블 트리거
DROP TRIGGER IF EXISTS update_users_updated_at ON users;
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- user_allergies 테이블 트리거
DROP TRIGGER IF EXISTS update_user_allergies_updated_at ON user_allergies;
CREATE TRIGGER update_user_allergies_updated_at
    BEFORE UPDATE ON user_allergies
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- side_effect_reports 테이블 트리거
DROP TRIGGER IF EXISTS update_side_effect_reports_updated_at ON side_effect_reports;
CREATE TRIGGER update_side_effect_reports_updated_at
    BEFORE UPDATE ON side_effect_reports
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ocr_ingredients 테이블 트리거
DROP TRIGGER IF EXISTS update_ocr_ingredients_updated_at ON ocr_ingredients;
CREATE TRIGGER update_ocr_ingredients_updated_at
    BEFORE UPDATE ON ocr_ingredients
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- posts 테이블 트리거
DROP TRIGGER IF EXISTS update_posts_updated_at ON posts;
CREATE TRIGGER update_posts_updated_at
    BEFORE UPDATE ON posts
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- 초기 데이터 (선택적)
-- ============================================

-- 테스트용 관리자 계정 (비밀번호: admin123 - 실제 사용 시 변경 필요)
-- INSERT INTO users (username, password, email, nickname) 
-- VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin@example.com', '관리자');

