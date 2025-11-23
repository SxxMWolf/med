-- ============================================
-- 스키마 삭제 스크립트 (개발/테스트용)
-- 주의: 모든 데이터가 삭제됩니다!
-- ============================================

-- 트리거 삭제
DROP TRIGGER IF EXISTS update_users_updated_at ON users;
DROP TRIGGER IF EXISTS update_user_allergies_updated_at ON user_allergies;
DROP TRIGGER IF EXISTS update_side_effect_reports_updated_at ON side_effect_reports;
DROP TRIGGER IF EXISTS update_ocr_ingredients_updated_at ON ocr_ingredients;
DROP TRIGGER IF EXISTS update_posts_updated_at ON posts;

-- 함수 삭제
DROP FUNCTION IF EXISTS update_updated_at_column();

-- 테이블 삭제 (외래키 제약조건 때문에 순서 중요)
DROP TABLE IF EXISTS comments CASCADE;
DROP TABLE IF EXISTS posts CASCADE;
DROP TABLE IF EXISTS ocr_ingredient_list CASCADE;
DROP TABLE IF EXISTS ocr_ingredients CASCADE;
DROP TABLE IF EXISTS side_effect_medications CASCADE;
DROP TABLE IF EXISTS side_effect_reports CASCADE;
DROP TABLE IF EXISTS user_allergies CASCADE;
DROP TABLE IF EXISTS users CASCADE;

