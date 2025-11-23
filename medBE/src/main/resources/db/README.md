# 데이터베이스 스키마 설정 가이드

## 데이터베이스 생성

PostgreSQL에서 데이터베이스를 생성합니다:

```sql
CREATE DATABASE med_db;
```

## 스키마 실행

`schema.sql` 파일을 실행하여 모든 테이블, 인덱스, 트리거를 생성합니다:

```bash
psql -U postgres -d med_db -f src/main/resources/db/schema.sql
```

또는 psql에서 직접 실행:

```sql
\i src/main/resources/db/schema.sql
```

## 테이블 구조

### 주요 테이블

1. **users** - 사용자 정보
   - id, username, password, email, nickname
   - created_at, updated_at

2. **user_allergies** - 사용자 알러지 정보
   - id, user_id, ingredient_name, description, severity
   - created_at, updated_at

3. **side_effect_reports** - 부작용 보고서
   - id, user_id, description, analysis_result
   - created_at, updated_at

4. **side_effect_medications** - 부작용 약물 목록
   - report_id, medication_name

5. **ocr_ingredients** - OCR 성분 분석 결과
   - id, user_id, image_url, ocr_text, analysis_result
   - created_at, updated_at

6. **ocr_ingredient_list** - OCR 성분 목록
   - ocr_id, ingredient_name

7. **posts** - 게시글
   - id, author_id, title, content, category
   - created_at, updated_at

8. **comments** - 댓글
   - id, post_id, author_id, content
   - created_at

## 인덱스

성능 최적화를 위해 다음 인덱스가 생성됩니다:
- 사용자 조회: username, email
- 관계 조회: user_id, author_id, post_id
- 정렬: created_at (DESC)
- 필터링: category

## 트리거

`updated_at` 컬럼이 자동으로 업데이트되도록 트리거가 설정되어 있습니다.

## 주의사항

1. **비밀번호 암호화**: 사용자 비밀번호는 bcrypt로 암호화되어 저장됩니다.
2. **외래키 제약조건**: CASCADE 삭제가 설정되어 있어 사용자 삭제 시 관련 데이터가 함께 삭제됩니다.
3. **초기 데이터**: schema.sql에 주석 처리된 테스트 계정이 있습니다. 필요시 활성화하여 사용하세요.

## 데이터베이스 연결 설정

`application.properties`에서 데이터베이스 연결 정보를 확인하세요:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/med_db
spring.datasource.username=postgres
spring.datasource.password=postgres
```

