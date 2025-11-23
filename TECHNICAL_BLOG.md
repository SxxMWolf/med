# 의약품 안전성 분석 플랫폼 개발기: 단계별 구현 가이드

## 📋 프로젝트 개요

의약품 성분 분석, 부작용 예측, 증상 기반 약물 추천 기능을 제공하는 웹 애플리케이션입니다. OCR, AI(GPT), 마이크로서비스 아키텍처를 활용하여 구현했습니다.

---

## 🏗️ Step 1: 프로젝트 아키텍처 설계 및 초기 설정

### 1.1 마이크로서비스 아키텍처 도입

**목표**: AI/ML 로직과 비즈니스 로직의 분리

**구현 내용**:
- **Java Spring Boot (medBE)**: 메인 API 서버, 인증, 데이터베이스 관리
- **Python FastAPI (medPY)**: AI/ML 처리 (GPT 프롬프트 최적화, NLP 처리)

**주요 기술 스택**:
```gradle
// Spring Boot 3.3.5
- Spring Data JPA (PostgreSQL)
- Spring Security + JWT
- WebClient (비동기 HTTP 통신)
- Swagger/OpenAPI (API 문서화)
```

```python
# Python FastAPI
- FastAPI 0.115.0
- OpenAI GPT API
- Pydantic (데이터 검증)
```

### 1.2 데이터베이스 설계

**PostgreSQL 스키마 설계**:
- `users`: 사용자 정보
- `user_allergies`: 사용자 알러지 정보
- `posts`, `comments`: 커뮤니티 게시글/댓글
- `post_likes`, `comment_likes`: 좋아요 기능
- `ocr_ingredients`: OCR 분석 결과 저장
- `side_effect_reports`: 부작용 보고서

**주요 설계 포인트**:
- Foreign Key CASCADE 삭제
- 인덱싱 최적화 (username, email, created_at)
- `updated_at` 자동 업데이트 트리거

---

## 🔐 Step 2: 사용자 인증 및 보안 구현

### 2.1 JWT 기반 인증 시스템

**구현 파일**: 
- `JwtConfig.java`: JWT 토큰 생성/검증
- `JwtAuthenticationFilter.java`: 요청 필터링
- `SecurityConfig.java`: Spring Security 설정

**주요 기능**:
```java
// JWT 토큰 생성
- HS256 알고리즘 사용
- 토큰 만료 시간: 24시간
- Secret Key: 환경변수로 관리

// 인증 필터
- Authorization 헤더에서 Bearer 토큰 추출
- OPTIONS 요청 (CORS preflight) 처리
- 상세한 로깅 (디버깅 용이)
```

### 2.2 회원가입 및 로그인 API

**엔드포인트**:
- `POST /api/auth/register`: 회원가입
- `POST /api/auth/login`: 로그인 (JWT 토큰 발급)
- `GET /api/auth/me`: 현재 사용자 정보 조회

**보안 기능**:
- BCrypt 패스워드 암호화
- 이메일 중복 검증
- JWT 토큰에 username 저장 (비민감 정보만)

### 2.3 CORS 및 보안 설정

**CorsConfig.java**:
```java
// 특정 origin만 허용 (local development)
- localhost:3000, localhost:3001
- Credentials: true (쿠키/인증 정보 포함)
```

---

## 📸 Step 3: OCR 기반 의약품 성분 분석 구현

### 3.1 Google Vision API 연동

**VisionService.java**:
- 이미지에서 텍스트 추출
- Base64 또는 파일 경로/URL 지원
- 에러 핸들링 및 로깅

### 3.2 마이크로서비스 통신

**Java → Python 통신**:
```java
// PythonApiService.java
- WebClient를 사용한 비동기 HTTP 통신
- 타임아웃 설정 (30초)
- 상세한 에러 로깅
- 연결 실패 시 명확한 에러 메시지
```

**Python 서비스 역할**:
1. **OCR 텍스트 정리**: GPT를 활용한 가독성 향상
2. **성분 추출**: 정규화된 텍스트에서 성분명 추출
3. **성분 분석**: 알러지 성분 비교 및 위험도 평가

### 3.3 GPT 프롬프트 최적화

**2단계 프롬프트 전략**:
1. **1단계**: OCR 텍스트 → 정리된 텍스트 (GPT)
2. **2단계**: 정리된 텍스트 → 성분 목록 추출 (GPT)

**폴백 메커니즘**:
- GPT API 실패 시 기본 정규식 파싱 사용
- 안정성과 정확성의 균형

---

## 📸 Step 3 상세: OCR 기반 의약품 성분 분석 구현 가이드

### 3.1 전체 아키텍처

**OCR 분석 플로우**:
```
사용자 이미지 업로드
    ↓
Google Vision API (텍스트 추출)
    ↓
Python 서비스 (텍스트 정규화 + 성분 추출)
    ↓
Python 서비스 (알러지 기반 안전성 분석)
    ↓
분석 결과 저장 및 반환
```

**패키지 구조**:
```
com.SxxM.med.ocr/
├── controller/ (AnalysisController에 통합)
├── service/
│   ├── VisionService.java          # Google Vision API 연동
│   └── OcrAnalysisService.java    # OCR 분석 오케스트레이션
├── entity/
│   └── OcrIngredient.java          # 분석 결과 저장
├── repository/
│   └── OcrIngredientRepository.java
└── dto/
    ├── OcrAnalysisRequest.java
    └── OcrAnalysisResponse.java
```

### 3.2 Google Vision API 연동

**VisionService 구현**:
```java
@Service
@RequiredArgsConstructor
public class VisionService {
    @Value("${google.vision.credentials.path:}")
    private String credentialsPath;
    
    public String extractTextFromImage(String imageData, boolean isBase64) {
        ImageAnnotatorClient vision = getClient();
        
        ByteString imageBytes;
        if (isBase64) {
            // Base64 디코딩
            byte[] decodedBytes = Base64.getDecoder().decode(imageData);
            imageBytes = ByteString.copyFrom(decodedBytes);
        } else {
            // URL 또는 파일 경로 처리
            if (imageData.startsWith("http://") || imageData.startsWith("https://")) {
                // HTTP URL에서 이미지 다운로드
                imageBytes = downloadImageFromUrl(imageData);
            } else {
                // 로컬 파일 경로
                imageBytes = ByteString.copyFrom(Files.readAllBytes(Paths.get(imageData)));
            }
        }
        
        // Vision API 호출
        Image img = Image.newBuilder().setContent(imageBytes).build();
        Feature feat = Feature.newBuilder()
            .setType(Feature.Type.TEXT_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
            .addFeatures(feat)
            .setImage(img)
            .build();
        
        BatchAnnotateImagesResponse response = vision.batchAnnotateImages(List.of(request));
        String extractedText = response.getResponses(0).getFullTextAnnotation().getText();
        
        return extractedText;
    }
}
```

**주요 기능**:
1. **다양한 입력 형식 지원**:
   - Base64 인코딩된 이미지
   - HTTP/HTTPS URL
   - 로컬 파일 경로

2. **에러 핸들링**:
   ```java
   catch (ResourceExhaustedException e) {
       // 할당량 초과 처리
   } catch (PermissionDeniedException e) {
       // 권한 오류 처리
   }
   ```

3. **인증 설정**:
   ```java
   private ImageAnnotatorClient getClient() throws IOException {
       if (credentialsPath != null && !credentialsPath.isEmpty()) {
           System.setProperty("GOOGLE_APPLICATION_CREDENTIALS", credentialsPath);
       }
       return ImageAnnotatorClient.create();
   }
   ```

### 3.3 OCR 분석 서비스 구현

**OcrAnalysisService 전체 플로우**:
```java
@Service
@Transactional
public class OcrAnalysisService {
    
    public OcrAnalysisResponse analyzeOcrImage(OcrAnalysisRequest request) {
        // 1. 사용자 정보 및 알러지 조회
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        List<String> allergyIngredients = userAllergyRepository
            .findByUserId(request.getUserId())
            .stream()
            .map(UserAllergy::getIngredientName)
            .collect(Collectors.toList());
        
        // 2. OCR 텍스트 추출
        String ocrText = visionService.extractTextFromImage(
            request.getImageData(), 
            request.isBase64()
        );
        
        // 3. Python 서비스: 텍스트 정규화 및 성분 추출
        Map<String, Object> normalizeResult = pythonApiService.normalizeOcrText(ocrText);
        List<String> extractedIngredients = (List<String>) 
            normalizeResult.get("normalized_ingredients");
        String cleanedText = normalizeResult.get("cleaned_text").toString();
        
        // 4. Python 서비스: 성분 분석 (알러지 비교 포함)
        Map<String, Object> analysisResult = pythonApiService.analyzeIngredients(
            extractedIngredients, 
            allergyIngredients
        );
        
        // 5. 응답 변환 및 저장
        OcrAnalysisResponse response = convertToOcrAnalysisResponse(
            analysisResult, ocrText, cleanedText, extractedIngredients
        );
        
        // 6. DB 저장 (선택적, 실패해도 응답은 반환)
        saveAnalysisResult(user, request, ocrText, extractedIngredients, response);
        
        return response;
    }
}
```

**주요 설계 포인트**:
1. **트랜잭션 관리**: `@Transactional`로 데이터 일관성 보장
2. **에러 복구**: DB 저장 실패해도 분석 결과는 반환
3. **로깅**: 각 단계별 상세 로깅으로 디버깅 용이

### 3.4 Python 서비스 통신

**텍스트 정규화 API 호출**:
```java
public Map<String, Object> normalizeOcrText(String ocrText) {
    Map<String, Object> request = Map.of("ocr_text", ocrText);
    
    Map<String, Object> response = getWebClient()
        .post()
        .uri("/ocr/normalize")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .retrieve()
        .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), 
            clientResponse -> {
                log.error("Python API 호출 실패: 상태코드={}", 
                    clientResponse.statusCode());
                return clientResponse.bodyToMono(String.class)
                    .flatMap(body -> Mono.error(new RuntimeException(
                        "Python API 호출 실패: " + clientResponse.statusCode())));
            })
        .bodyToMono(Map.class)
        .timeout(Duration.ofSeconds(30))
        .block();
    
    // 응답 검증
    if (response == null || !response.containsKey("normalized_ingredients")) {
        throw new RuntimeException("Python API 응답이 올바르지 않습니다");
    }
    
    return response;
}
```

**성분 분석 API 호출**:
```java
public Map<String, Object> analyzeIngredients(
        List<String> ingredients, 
        List<String> allergyIngredients) {
    Map<String, Object> request = Map.of(
        "ingredients", ingredients,
        "allergy_ingredients", allergyIngredients
    );
    
    Map<String, Object> response = getWebClient()
        .post()
        .uri("/analyze/ingredients")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .retrieve()
        .bodyToMono(Map.class)
        .timeout(Duration.ofSeconds(60))
        .block();
    
    return response;
}
```

**에러 처리 전략**:
1. **타임아웃 처리**: 30초(정규화), 60초(분석)
2. **연결 실패**: 명확한 에러 메시지 제공
3. **응답 검증**: 필수 필드 존재 여부 확인

### 3.5 응답 변환 및 파싱

**Python 응답 → Java DTO 변환**:
```java
private OcrAnalysisResponse convertToOcrAnalysisResponse(
        Map<String, Object> analysisResult,
        String ocrText,
        String cleanedText,
        List<String> extractedIngredients) {
    
    OcrAnalysisResponse response = new OcrAnalysisResponse();
    response.setOcrText(ocrText);
    response.setCleanedText(cleanedText);
    response.setExtractedIngredients(extractedIngredients);
    
    // 안전성 분석 결과 파싱
    IngredientAnalysis analysis = new IngredientAnalysis();
    
    if (analysisResult.containsKey("safety_level")) {
        analysis.setSafetyLevel(analysisResult.get("safety_level").toString());
    }
    
    if (analysisResult.containsKey("ingredient_risks")) {
        List<Map<String, Object>> risks = (List<Map<String, Object>>) 
            analysisResult.get("ingredient_risks");
        List<IngredientRisk> ingredientRisks = risks.stream()
            .map(this::convertToIngredientRisk)
            .collect(Collectors.toList());
        analysis.setIngredientRisks(ingredientRisks);
    }
    
    // 예상 부작용, 종합 평가, 권장사항 파싱...
    
    response.setAnalysis(analysis);
    return response;
}
```

**안전한 타입 변환**:
```java
private IngredientRisk convertToIngredientRisk(Map<String, Object> riskMap) {
    IngredientRisk risk = new IngredientRisk();
    
    // null-safe 변환
    if (riskMap.containsKey("ingredient_name")) {
        Object value = riskMap.get("ingredient_name");
        risk.setIngredientName(value != null ? value.toString() : null);
    }
    // ... 다른 필드들도 동일하게 처리
    
    return risk;
}
```

**핵심 포인트**:
- 타입 안전성: `@SuppressWarnings("unchecked")` 최소화
- Null 안전성: 모든 값에 대해 null 체크
- 예외 처리: 파싱 실패 시 빈 리스트 반환

### 3.6 데이터베이스 저장

**OcrIngredient 엔티티**:
```java
@Entity
@Table(name = "ocr_ingredients")
public class OcrIngredient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "image_url", length = 1000)
    private String imageUrl;
    
    @Column(name = "ocr_text", columnDefinition = "TEXT")
    private String ocrText;
    
    @ElementCollection
    @CollectionTable(name = "ocr_ingredient_list", 
        joinColumns = @JoinColumn(name = "ocr_id"))
    @Column(name = "ingredient_name")
    private List<String> ingredientList;
    
    @Column(name = "analysis_result", columnDefinition = "TEXT")
    private String analysisResult;  // JSON 문자열로 저장
}
```

**저장 로직**:
```java
try {
    OcrIngredient ocrIngredient = OcrIngredient.builder()
        .user(user)
        .imageUrl(request.isBase64() ? "base64_data" : request.getImageData())
        .ocrText(ocrText)
        .ingredientList(extractedIngredients)
        .analysisResult(objectMapper.writeValueAsString(response))
        .build();
    
    ocrIngredientRepository.save(ocrIngredient);
    log.info("OCR 분석 결과 DB 저장 완료");
} catch (Exception e) {
    log.warn("OCR 분석 결과 DB 저장 실패 (응답은 반환)", e);
    // DB 저장 실패해도 응답은 반환 (서비스 가용성 우선)
}
```

**설계 철학**:
- **가용성 우선**: DB 저장 실패해도 분석 결과는 반환
- **JSON 저장**: 분석 결과를 JSON 문자열로 저장하여 유연성 확보
- **ElementCollection**: 성분 목록을 별도 테이블로 관리

### 3.7 API 엔드포인트 구현

**AnalysisController**:
```java
@PostMapping("/ocr")
@Operation(summary = "OCR 분석", 
    description = "의약품 성분표 이미지를 OCR로 분석하여 성분 리스트 및 안전성을 평가합니다.")
@SecurityRequirement(name = "BearerAuth")
public ResponseEntity<OcrAnalysisResponse> analyzeOcr(
        Authentication authentication,
        @Valid @RequestBody OcrAnalysisRequest request
) {
    try {
        // JWT에서 사용자 정보 추출
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        // 요청의 userId를 JWT에서 추출한 userId로 덮어쓰기 (보안)
        request.setUserId(user.getId());
        
        log.info("OCR 분석 시작: userId={}, username={}", user.getId(), username);
        OcrAnalysisResponse response = ocrAnalysisService.analyzeOcrImage(request);
        log.info("OCR 분석 완료: userId={}", user.getId());
        
        return ResponseEntity.ok(response);
    } catch (RuntimeException e) {
        log.error("OCR 분석 요청 처리 중 오류 발생: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    } catch (Exception e) {
        log.error("OCR 분석 요청 처리 중 예상치 못한 오류 발생", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
```

**보안 고려사항**:
1. **JWT 인증 필수**: `@SecurityRequirement(name = "BearerAuth")`
2. **사용자 ID 덮어쓰기**: 요청의 userId를 JWT에서 추출한 값으로 덮어쓰기
3. **에러 메시지**: 민감한 정보 노출 방지

### 3.8 DTO 설계

**OcrAnalysisRequest**:
```java
public class OcrAnalysisRequest {
    private Long userId;  // JWT에서 자동 설정
    
    @NotBlank(message = "이미지 URL 또는 Base64 데이터는 필수입니다")
    private String imageData;  // URL 또는 Base64
    
    private boolean isBase64;
}
```

**OcrAnalysisResponse**:
```java
@Builder
public class OcrAnalysisResponse {
    private String ocrText;                    // 원본 OCR 텍스트
    private String cleanedText;                // 정리된 텍스트
    private List<String> extractedIngredients; // 추출된 성분 목록
    private IngredientAnalysis analysis;       // 안전성 분석 결과
    
    @Builder
    public static class IngredientAnalysis {
        private String safetyLevel;            // SAFE, CAUTION, DANGEROUS
        private List<IngredientRisk> ingredientRisks;
        private List<String> expectedSideEffects;
        private String overallAssessment;
        private List<String> recommendations;
    }
    
    @Builder
    public static class IngredientRisk {
        private String ingredientName;
        private String content;
        private String allergyRisk;
        private String riskLevel;
        private String reason;
    }
}
```

**설계 원칙**:
- **계층적 구조**: 중첩 클래스로 관련 데이터 그룹화
- **Builder 패턴**: 복잡한 객체 생성 간소화
- **명확한 필드명**: API 응답 구조 직관적

### 3.9 에러 처리 전략

**계층별 예외 처리**:
```java
// VisionService
catch (ResourceExhaustedException e) {
    throw new RuntimeException("Vision API 할당량이 초과되었습니다");
} catch (PermissionDeniedException e) {
    throw new RuntimeException("Vision API 권한이 없습니다");
}

// PythonApiService
catch (WebClientException e) {
    throw new RuntimeException("Python API 서비스에 연결할 수 없습니다");
} catch (TimeoutException e) {
    throw new RuntimeException("Python API 호출이 시간 초과되었습니다");
}

// OcrAnalysisService
catch (Exception e) {
    log.error("OCR 분석 중 오류 발생", e);
    throw new RuntimeException("OCR 분석 중 오류가 발생했습니다");
}
```

**로깅 전략**:
- 각 단계별 상세 로깅
- 에러 발생 시 스택 트레이스 포함
- 사용자 ID, 이미지 정보 등 컨텍스트 포함

### 3.10 성능 최적화

**1. 비동기 처리 고려사항**:
- 현재는 동기 처리 (`block()`)
- 향후 WebFlux로 전환 가능

**2. 타임아웃 설정**:
- 텍스트 정규화: 30초
- 성분 분석: 60초 (더 복잡한 처리)

**3. DB 저장 최적화**:
- 선택적 저장 (실패해도 응답 반환)
- JSON 직렬화 최적화

### 3.11 데이터베이스 스키마

**OCR 성분 테이블**:
```sql
CREATE TABLE ocr_ingredients (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    image_url VARCHAR(1000),
    ocr_text TEXT,
    analysis_result TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ocr_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE
);

-- 성분 목록 테이블 (ElementCollection)
CREATE TABLE ocr_ingredient_list (
    ocr_id BIGINT NOT NULL,
    ingredient_name VARCHAR(255) NOT NULL,
    CONSTRAINT fk_ingredient_list_ocr FOREIGN KEY (ocr_id) 
        REFERENCES ocr_ingredients(id) ON DELETE CASCADE,
    PRIMARY KEY (ocr_id, ingredient_name)
);

-- 인덱스
CREATE INDEX idx_ocr_ingredients_user_id ON ocr_ingredients(user_id);
```

### 3.12 구현 시 고려사항

**1. 이미지 형식 지원**:
- Base64 인코딩
- HTTP/HTTPS URL
- 로컬 파일 경로

**2. 확장 가능성**:
- Python 서비스와의 느슨한 결합
- 새로운 분석 로직 추가 용이
- 마이크로서비스 아키텍처

**3. 사용자 경험**:
- 상세한 에러 메시지
- 분석 결과의 구조화된 정보
- 알러지 기반 맞춤 분석

---

## 🎯 OCR 기능 핵심 포인트

### 1. 마이크로서비스 아키텍처
- **Java (Spring Boot)**: OCR API, 사용자 관리, 데이터 저장
- **Python (FastAPI)**: 텍스트 정규화, 성분 추출, AI 분석
- **Google Vision API**: 이미지에서 텍스트 추출

### 2. 3단계 분석 프로세스
1. **텍스트 추출**: Google Vision API
2. **텍스트 정규화**: Python 서비스 (GPT 활용)
3. **안전성 분석**: Python 서비스 (알러지 비교)

### 3. 사용자 맞춤 분석
- 사용자 알러지 정보 기반 위험도 평가
- 개인화된 권장사항 제공
- 안전성 레벨 분류 (SAFE, CAUTION, DANGEROUS)

### 4. 안정성과 가용성
- DB 저장 실패해도 분석 결과 반환
- 상세한 에러 핸들링 및 로깅
- 타임아웃 설정으로 무한 대기 방지

### 5. 확장 가능한 설계
- DTO 패턴으로 API 버전 관리 용이
- Python 서비스와의 느슨한 결합
- 새로운 분석 로직 추가 용이

---

## 🧠 Step 4: AI 기반 의약품 분석 구현

### 4.1 증상 분석 기능

**SymptomAnalysisService.java**:
- 사용자 증상 입력
- 알러지 성분 기반 약물 필터링
- GPT를 통한 약물 추천 및 주의사항 제공

**응답 구조**:
```json
{
  "recommendedMedications": [...],
  "notRecommendedMedications": [...],
  "precautions": [...]
}
```

### 4.2 부작용 분석 기능

**SideEffectAnalysisService.java**:
- 복용 중인 약물들의 부작용 분석
- 공통 성분 추출
- 알러지 성분 매칭
- Python 서비스를 통한 고급 분석

**Python 서비스 (sideeffect_service.py)**:
- 공통 성분 자동 추출
- GPT를 통한 위험 패턴 분석
- 사용자 민감 성분 식별

---

## 🧠 Step 4 상세: AI 기반 약 성분 분석, 추천, 위험 성분 추출 구현 가이드

### 4.1 전체 아키텍처

**AI 분석 시스템 플로우**:
```
사용자 입력 (증상/약물 목록)
    ↓
사용자 알러지 정보 조회
    ↓
약물 정보 조회 (MedicationDbService)
    ↓
GPT API / Python 서비스 (AI 분석)
    ↓
위험 성분 추출 및 안전성 평가
    ↓
개인화된 추천 및 주의사항 제공
```

**주요 구성 요소**:
1. **증상 기반 약물 추천**: GPT를 활용한 증상 분석 및 약물 추천
2. **부작용 분석**: 복용 중인 약물들의 상호작용 및 위험 성분 분석
3. **알러지 기반 필터링**: 사용자 알러지 정보를 활용한 안전성 평가
4. **위험 성분 추출**: 공통 성분, 알러지 성분, 부작용 성분 식별

### 4.2 GPT 서비스 구현

**GptService 설계**:
```java
@Service
@RequiredArgsConstructor
public class GptService {
    @Value("${gpt.api.key}")
    private String apiKey;
    
    @Value("${gpt.model:gpt-4}")
    private String model;
    
    // 구조화된 JSON 응답을 위한 메서드
    public <T> T analyzeWithGpt(String prompt, Class<T> responseClass) {
        Map<String, Object> requestBody = Map.of(
            "model", model,
            "messages", List.of(
                Map.of("role", "system", 
                    "content", "You are a medical assistant. Always respond in valid JSON format only."),
                Map.of("role", "user", "content", prompt)
            ),
            "temperature", 0.3,
            "response_format", Map.of("type", "json_object")
        );
        
        // WebClient를 통한 비동기 호출
        String response = getWebClient()
            .post()
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .block();
        
        // JSON 파싱 및 타입 변환
        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
        String content = extractContentFromResponse(responseMap);
        
        return objectMapper.readValue(content, responseClass);
    }
}
```

**주요 설계 포인트**:
1. **제네릭 메서드**: 다양한 응답 타입 지원
2. **JSON 형식 강제**: `response_format: json_object`로 구조화된 응답 보장
3. **Temperature 설정**: 0.3으로 일관된 응답 생성
4. **에러 핸들링**: 상세한 로깅 및 예외 처리

### 4.3 증상 기반 약물 추천 시스템

**SymptomAnalysisService 구현**:
```java
@Service
@Transactional
public class SymptomAnalysisService {
    private final UserRepository userRepository;
    private final UserAllergyRepository userAllergyRepository;
    private final GptService gptService;
    
    public SymptomAnalysisResponse analyzeSymptom(SymptomAnalysisRequest request) {
        // 1. 사용자 존재 여부 확인
        userRepository.findById(request.getUserId())
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        // 2. 사용자 알러지 정보 조회
        List<String> allergyIngredients = userAllergyRepository
            .findByUserId(request.getUserId())
            .stream()
            .map(UserAllergy::getIngredientName)
            .collect(Collectors.toList());
        
        // 3. GPT 프롬프트 생성
        String prompt = buildSymptomAnalysisPrompt(
            request.getSymptomText(), 
            allergyIngredients
        );
        
        // 4. GPT 분석 요청
        SymptomAnalysisResponse response = gptService.analyzeWithGpt(
            prompt, 
            SymptomAnalysisResponse.class
        );
        
        return response;
    }
}
```

**프롬프트 엔지니어링**:
```java
private String buildSymptomAnalysisPrompt(String symptomText, List<String> allergyIngredients) {
    StringBuilder prompt = new StringBuilder();
    
    // 증상 정보
    prompt.append("사용자가 다음과 같은 증상을 호소하고 있습니다:\n\n");
    prompt.append("증상: ").append(symptomText).append("\n\n");
    
    // 알러지 정보 (있는 경우)
    if (!allergyIngredients.isEmpty()) {
        prompt.append("사용자의 알러지 성분 목록:\n");
        allergyIngredients.forEach(ingredient -> 
            prompt.append("- ").append(ingredient).append("\n")
        );
        prompt.append("\n");
    }
    
    // 응답 형식 명시
    prompt.append("""
        다음 정보를 포함하여 JSON 형식으로 응답해주세요:
        1. 추천 약물 목록 (recommendedMedications): 각 약물의 이름, 추천 이유, 복용법
        2. 피해야 할 약물 목록 (notRecommendedMedications): 알러지 성분이 포함된 약물, 피해야 하는 이유, 포함된 알러지 성분
        3. 주의 사항 (precautions): 복용 시 주의해야 할 사항들
        
        JSON 형식:
        {
          "recommendedMedications": [
            {
              "name": "약물명",
              "reason": "추천 이유",
              "dosage": "복용법"
            }
          ],
          "notRecommendedMedications": [
            {
              "name": "약물명",
              "reason": "피해야 하는 이유",
              "allergicIngredients": ["알러지 성분1", "알러지 성분2"]
            }
          ],
          "precautions": ["주의사항1", "주의사항2"]
        }
        """);
    
    return prompt.toString();
}
```

**프롬프트 설계 원칙**:
1. **명확한 컨텍스트 제공**: 증상과 알러지 정보를 구체적으로 전달
2. **구조화된 출력 요구**: JSON 형식과 필드 구조 명시
3. **의료 보조 역할 강조**: System 메시지로 역할 명확화
4. **안전성 우선**: 알러지 성분 기반 필터링 강조

**응답 DTO 구조**:
```java
@Builder
public class SymptomAnalysisResponse {
    private List<RecommendedMedication> recommendedMedications;
    private List<NotRecommendedMedication> notRecommendedMedications;
    private List<String> precautions;
    
    @Builder
    public static class RecommendedMedication {
        private String name;        // 약물명
        private String reason;      // 추천 이유
        private String dosage;      // 복용법
    }
    
    @Builder
    public static class NotRecommendedMedication {
        private String name;                    // 약물명
        private String reason;                  // 피해야 하는 이유
        private List<String> allergicIngredients; // 포함된 알러지 성분
    }
}
```

### 4.4 부작용 분석 시스템

**SideEffectAnalysisService 구현**:
```java
@Service
@Transactional
public class SideEffectAnalysisService {
    private final UserRepository userRepository;
    private final UserAllergyRepository userAllergyRepository;
    private final MedicationDbService medicationDbService;
    private final PythonApiService pythonApiService;
    private final SideEffectReportRepository sideEffectReportRepository;
    
    public SideEffectAnalysisResponse analyzeSideEffect(SideEffectAnalysisRequest request) {
        // 1. 사용자 정보 및 알러지 조회
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        List<String> allergyIngredients = userAllergyRepository
            .findByUserId(request.getUserId())
            .stream()
            .map(UserAllergy::getIngredientName)
            .collect(Collectors.toList());
        
        // 2. 각 약물의 성분 정보 조회
        List<MedicationInfo> medicationInfos = medicationDbService
            .getMedicationInfoList(request.getMedicationNames());
        
        // 3. 약물별 성분 리스트 준비
        List<List<String>> medicationIngredients = medicationInfos.stream()
            .map(MedicationInfo::getIngredients)
            .collect(Collectors.toList());
        
        // 4. Python 서비스를 통한 부작용 분석
        Map<String, Object> analysisResult = pythonApiService.analyzeSideEffects(
            request.getMedicationNames(),
            medicationIngredients,
            allergyIngredients,
            request.getDescription()
        );
        
        // 5. 응답 변환
        SideEffectAnalysisResponse response = convertToSideEffectAnalysisResponse(analysisResult);
        
        // 6. 분석 결과 저장
        saveAnalysisResult(user, request, response);
        
        return response;
    }
}
```

**Python 서비스 통신**:
```java
public Map<String, Object> analyzeSideEffects(
        List<String> medicationNames,
        List<List<String>> medicationIngredients,
        List<String> allergyIngredients,
        String description) {
    
    Map<String, Object> request = Map.of(
        "medication_names", medicationNames,
        "medication_ingredients", medicationIngredients,
        "allergy_ingredients", allergyIngredients != null ? allergyIngredients : List.of(),
        "description", description != null ? description : ""
    );
    
    Map<String, Object> response = getWebClient()
        .post()
        .uri("/analyze/sideeffects")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .retrieve()
        .bodyToMono(Map.class)
        .timeout(Duration.ofSeconds(60))
        .block();
    
    return response;
}
```

**Python 서비스 분석 내용**:
1. **공통 성분 추출**: 여러 약물에 공통으로 포함된 성분 식별
2. **알러지 성분 매칭**: 사용자 알러지 성분과 약물 성분 비교
3. **부작용 성분 분석**: GPT를 통한 위험 패턴 분석
4. **종합 평가**: 전체적인 안전성 평가 및 권장사항 제공

### 4.5 위험 성분 추출 로직

**응답 변환 및 위험 성분 분류**:
```java
private SideEffectAnalysisResponse convertToSideEffectAnalysisResponse(
        Map<String, Object> analysisResult) {
    
    SideEffectAnalysisResponse response = new SideEffectAnalysisResponse();
    
    // 1. 공통 성분 (여러 약물에 공통으로 포함된 성분)
    if (analysisResult.containsKey("common_ingredients")) {
        List<String> commonIngredients = (List<String>) 
            analysisResult.get("common_ingredients");
        response.setCommonIngredients(commonIngredients);
    }
    
    // 2. 사용자 민감 성분 (알러지 성분 매칭)
    if (analysisResult.containsKey("user_sensitive_ingredients")) {
        List<Map<String, Object>> sensitiveList = (List<Map<String, Object>>) 
            analysisResult.get("user_sensitive_ingredients");
        List<SensitiveIngredient> sensitiveIngredients = sensitiveList.stream()
            .map(this::convertToSensitiveIngredient)
            .collect(Collectors.toList());
        response.setUserSensitiveIngredients(sensitiveIngredients);
    }
    
    // 3. 부작용 성분 (GPT 분석 결과)
    if (analysisResult.containsKey("common_side_effect_ingredients")) {
        List<Map<String, Object>> sideEffectList = (List<Map<String, Object>>) 
            analysisResult.get("common_side_effect_ingredients");
        List<CommonSideEffectIngredient> sideEffectIngredients = sideEffectList.stream()
            .map(this::convertToCommonSideEffectIngredient)
            .collect(Collectors.toList());
        response.setCommonSideEffectIngredients(sideEffectIngredients);
    }
    
    // 4. 종합 평가
    if (analysisResult.containsKey("summary")) {
        response.setSummary(analysisResult.get("summary").toString());
    }
    
    return response;
}
```

**위험 성분 분류**:
1. **공통 성분 (common_ingredients)**:
   - 여러 약물에 공통으로 포함된 성분
   - 중복 복용 시 과다 복용 위험

2. **사용자 민감 성분 (user_sensitive_ingredients)**:
   - 사용자 알러지 성분과 매칭된 성분
   - 심각도(severity) 및 이유(reason) 포함

3. **부작용 성분 (common_side_effect_ingredients)**:
   - GPT 분석을 통한 부작용 위험 성분
   - 부작용 설명 및 빈도(frequency) 포함

**SensitiveIngredient 변환**:
```java
private SensitiveIngredient convertToSensitiveIngredient(Map<String, Object> map) {
    SensitiveIngredient ingredient = new SensitiveIngredient();
    
    if (map.containsKey("ingredient_name")) {
        ingredient.setIngredientName(map.get("ingredient_name").toString());
    }
    if (map.containsKey("reason")) {
        ingredient.setReason(map.get("reason").toString());
    }
    if (map.containsKey("severity")) {
        ingredient.setSeverity(map.get("severity").toString()); // MILD, MODERATE, SEVERE
    }
    
    return ingredient;
}
```

### 4.6 약물 정보 조회 시스템

**MedicationDbService 구현**:
```java
@Service
public class MedicationDbService {
    @Value("${medication.db.api.url:}")
    private String apiUrl;
    
    @Value("${medication.db.api.key:}")
    private String apiKey;
    
    public MedicationInfo getMedicationInfo(String medicationName) {
        try {
            if (apiUrl == null || apiUrl.isEmpty()) {
                // 외부 API가 없는 경우 내부 DB 조회
                return getMedicationInfoFromInternalDb(medicationName);
            }
            
            // 외부 의약품 DB API 호출
            WebClient webClient = webClientBuilder.baseUrl(apiUrl).build();
            String response = webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                    .path("/medication")
                    .queryParam("name", medicationName)
                    .queryParam("apiKey", apiKey)
                    .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            if (response == null) {
                return getMedicationInfoFromInternalDb(medicationName);
            }
            
            JsonNode jsonNode = objectMapper.readTree(response);
            return parseMedicationInfo(jsonNode);
            
        } catch (Exception e) {
            log.error("외부 의약품 DB 조회 중 오류 발생: {}", medicationName, e);
            // 폴백: 내부 DB 조회
            return getMedicationInfoFromInternalDb(medicationName);
        }
    }
    
    public List<MedicationInfo> getMedicationInfoList(List<String> medicationNames) {
        return medicationNames.stream()
            .map(this::getMedicationInfo)
            .collect(Collectors.toList());
    }
}
```

**MedicationInfo DTO**:
```java
@Builder
public class MedicationInfo {
    private String name;              // 약물명
    private List<String> ingredients; // 성분 목록
    private String description;        // 설명
    private String manufacturer;      // 제조사
}
```

**폴백 전략**:
- 외부 API 실패 시 내부 DB 조회
- 내부 DB에도 없으면 빈 성분 목록 반환
- 서비스 가용성 우선

### 4.7 API 엔드포인트 구현

**AnalysisController**:
```java
@RestController
@RequestMapping("/api/analysis")
@Tag(name = "Analysis", description = "의약품 분석 API")
public class AnalysisController {
    
    // 증상 기반 약물 추천
    @PostMapping("/symptom")
    @Operation(summary = "증상 분석", 
        description = "사용자의 증상을 분석하여 추천 약물 및 주의사항을 제공합니다.")
    public ResponseEntity<SymptomAnalysisResponse> analyzeSymptom(
            @Valid @RequestBody SymptomAnalysisRequest request
    ) {
        try {
            SymptomAnalysisResponse response = symptomAnalysisService.analyzeSymptom(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("증상 분석 요청 처리 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // 부작용 분석
    @PostMapping("/side-effect")
    @Operation(summary = "부작용 분석", 
        description = "복용 중인 약물들의 부작용을 분석하여 공통 성분 및 위험 패턴을 추출합니다.")
    public ResponseEntity<SideEffectAnalysisResponse> analyzeSideEffect(
            @Valid @RequestBody SideEffectAnalysisRequest request
    ) {
        try {
            SideEffectAnalysisResponse response = sideEffectAnalysisService.analyzeSideEffect(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("부작용 분석 요청 처리 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
```

### 4.8 부작용 보고서 저장

**SideEffectReport 엔티티**:
```java
@Entity
@Table(name = "side_effect_reports")
@Builder
public class SideEffectReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ElementCollection
    @CollectionTable(name = "side_effect_medications", 
        joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "medication_name")
    private List<String> medicationNames;
    
    @Column(length = 2000)
    private String description;
    
    @Column(name = "analysis_result", columnDefinition = "TEXT")
    private String analysisResult;  // JSON 문자열로 저장
}
```

**저장 로직**:
```java
SideEffectReport report = SideEffectReport.builder()
    .user(user)
    .medicationNames(request.getMedicationNames())
    .description(request.getDescription())
    .analysisResult(objectMapper.writeValueAsString(response))
    .build();

sideEffectReportRepository.save(report);
```

**저장 목적**:
- 사용자 이력 관리
- 향후 분석 데이터 축적
- 개인화된 추천 개선

### 4.9 프롬프트 엔지니어링 전략

**1. System 메시지 설정**:
```java
Map.of("role", "system", 
    "content", "You are a medical assistant. Always respond in valid JSON format only.")
```
- 역할 명확화
- 출력 형식 강제

**2. Temperature 설정**:
```java
"temperature", 0.3
```
- 낮은 값으로 일관된 응답 생성
- 의료 정보의 정확성 우선

**3. JSON 형식 강제**:
```java
"response_format", Map.of("type", "json_object")
```
- 구조화된 응답 보장
- 파싱 오류 최소화

**4. 컨텍스트 제공**:
- 사용자 증상 상세 설명
- 알러지 정보 포함
- 응답 형식 명시

### 4.10 에러 처리 및 로깅

**계층별 예외 처리**:
```java
// GptService
catch (Exception e) {
    log.error("GPT API 호출 중 오류 발생", e);
    throw new RuntimeException("GPT 분석 중 오류가 발생했습니다: " + e.getMessage(), e);
}

// SymptomAnalysisService
catch (Exception e) {
    log.error("증상 분석 중 오류 발생", e);
    throw new RuntimeException("증상 분석 중 오류가 발생했습니다: " + e.getMessage(), e);
}

// Controller
catch (Exception e) {
    log.error("증상 분석 요청 처리 중 오류 발생", e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
}
```

**로깅 전략**:
- 각 단계별 상세 로깅
- 사용자 ID, 약물명 등 컨텍스트 포함
- 에러 발생 시 스택 트레이스 포함

### 4.11 성능 최적화

**1. 약물 정보 일괄 조회**:
```java
public List<MedicationInfo> getMedicationInfoList(List<String> medicationNames) {
    return medicationNames.stream()
        .map(this::getMedicationInfo)
        .collect(Collectors.toList());
}
```
- 병렬 처리 가능 (향후 개선)
- 현재는 순차 처리

**2. 타임아웃 설정**:
- Python 서비스: 60초 (복잡한 분석)
- GPT API: 기본 타임아웃

**3. 캐싱 전략 (향후 개선)**:
- 약물 정보 캐싱
- GPT 응답 캐싱 (동일 증상)

### 4.12 데이터베이스 스키마

**부작용 보고서 테이블**:
```sql
CREATE TABLE side_effect_reports (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    description VARCHAR(2000),
    analysis_result TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_side_effect_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE
);

-- 약물 목록 테이블 (ElementCollection)
CREATE TABLE side_effect_medications (
    report_id BIGINT NOT NULL,
    medication_name VARCHAR(255) NOT NULL,
    CONSTRAINT fk_medication_report FOREIGN KEY (report_id) 
        REFERENCES side_effect_reports(id) ON DELETE CASCADE,
    PRIMARY KEY (report_id, medication_name)
);

-- 인덱스
CREATE INDEX idx_side_effect_reports_user_id ON side_effect_reports(user_id);
```

### 4.13 구현 시 고려사항

**1. 의료 정보의 정확성**:
- GPT는 보조 도구일 뿐, 최종 진단이 아님
- 사용자에게 의사 상담 권장 메시지 포함

**2. 개인정보 보호**:
- 알러지 정보는 사용자별로 분리 관리
- 분석 결과는 해당 사용자만 접근 가능

**3. 확장 가능성**:
- 새로운 분석 로직 추가 용이
- Python 서비스와의 느슨한 결합
- 마이크로서비스 아키텍처

**4. 안정성**:
- 폴백 메커니즘 (외부 API 실패 시)
- 상세한 에러 핸들링
- 트랜잭션 관리

---

## 🎯 AI 기반 약 성분 분석 핵심 포인트

### 1. 개인화된 분석
- **사용자 알러지 정보 활용**: 개인별 맞춤 안전성 평가
- **증상 기반 추천**: GPT를 통한 지능형 약물 추천
- **위험 성분 식별**: 공통 성분, 알러지 성분, 부작용 성분 분류

### 2. AI 통합 전략
- **GPT API**: 증상 분석, 약물 추천, 주의사항 제공
- **Python 서비스**: 복잡한 성분 분석, 패턴 인식
- **하이브리드 접근**: AI와 규칙 기반 로직 결합

### 3. 위험 성분 추출
- **공통 성분**: 중복 복용 위험 식별
- **알러지 성분**: 사용자별 민감 성분 매칭
- **부작용 성분**: GPT 기반 위험 패턴 분석

### 4. 프롬프트 엔지니어링
- **구조화된 출력**: JSON 형식 강제
- **명확한 컨텍스트**: 증상, 알러지 정보 상세 제공
- **일관된 응답**: Temperature 0.3 설정

### 5. 데이터 관리
- **분석 결과 저장**: 사용자 이력 관리
- **약물 정보 조회**: 외부 API + 내부 DB 폴백
- **트랜잭션 관리**: 데이터 일관성 보장

---

## 👥 Step 5: 커뮤니티 기능 구현

### 5.1 게시글 및 댓글 시스템

**주요 엔드포인트**:
- `GET /api/posts`: 게시글 목록 (페이지네이션)
- `POST /api/posts`: 게시글 작성 (JWT 인증)
- `GET /api/posts/{postId}?withComments=true`: 게시글 상세 + 댓글
- `GET /api/comments/post/{postId}?page=0&size=20`: 댓글 목록 (페이지네이션)

**페이지네이션**:
```java
// Spring Data JPA Pageable 활용
- 기본 페이지 크기: 20
- 정렬: createdAt DESC
- 카테고리별 필터링 지원
```

### 5.2 좋아요 기능

**구현 내용**:
- 게시글 좋아요: `POST /api/posts/{postId}/like`
- 댓글 좋아요: `POST /api/comments/{commentId}/like`
- 중복 좋아요 방지 (DB 제약조건)
- 실시간 좋아요 수 반환

**데이터베이스 설계**:
```sql
-- 복합 Primary Key로 중복 방지
CREATE TABLE post_likes (
    user_id BIGINT,
    post_id BIGINT,
    PRIMARY KEY (user_id, post_id)
);
```

### 5.3 콘텐츠 검증

**ContentValidationService.java**:
- 부적절한 콘텐츠 필터링 (확장 가능한 구조)
- GPT 기반 검증 (선택적)

---

## 📝 Step 5 상세: 커뮤니티 기능 구현 가이드

### 5.1 아키텍처 설계

**패키지 구조**:
```
com.SxxM.med.community/
├── controller/
│   ├── PostController.java
│   └── CommentController.java
├── service/
│   ├── PostService.java
│   ├── CommentService.java
│   ├── LikeService.java
│   └── ContentValidationService.java
├── entity/
│   ├── Post.java
│   ├── Comment.java
│   ├── PostLike.java
│   └── CommentLike.java
├── repository/
│   ├── PostRepository.java
│   ├── CommentRepository.java
│   ├── PostLikeRepository.java
│   └── CommentLikeRepository.java
└── dto/
    ├── PostCreateRequest.java
    ├── PostResponse.java
    ├── PostDetailResponse.java
    └── ...
```

**기능별 분리 원칙**:
- 게시글(Post)과 댓글(Comment)은 독립적인 엔티티로 관리
- 좋아요 기능은 별도 서비스로 분리하여 재사용성 향상
- DTO 패턴으로 엔티티와 API 응답 분리

### 5.2 데이터베이스 설계

**엔티티 관계**:
```java
// Post 엔티티
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "author_id", nullable = false)
private User author;

// Comment 엔티티
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "post_id", nullable = false)
private Post post;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "author_id", nullable = false)
private User author;
```

**좋아요 테이블 설계**:
```sql
-- 게시글 좋아요 (중복 방지)
CREATE TABLE post_likes (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_post_like UNIQUE (post_id, user_id),
    CONSTRAINT fk_post_like_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_post_like_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 인덱스 최적화
CREATE INDEX idx_post_likes_post_id ON post_likes(post_id);
CREATE INDEX idx_post_likes_user_id ON post_likes(user_id);
```

**주요 설계 포인트**:
1. **LAZY 로딩**: `FetchType.LAZY`로 N+1 문제 방지
2. **CASCADE 삭제**: 게시글 삭제 시 관련 댓글/좋아요 자동 삭제
3. **UNIQUE 제약**: `(post_id, user_id)` 복합 키로 중복 좋아요 방지
4. **인덱싱**: 자주 조회되는 컬럼에 인덱스 생성

### 5.3 게시글 CRUD 구현

**게시글 작성**:
```java
@PostMapping
@SecurityRequirement(name = "BearerAuth")
public ResponseEntity<PostResponse> createPost(
        Authentication authentication,
        @Valid @RequestBody PostCreateRequest request
) {
    String username = authentication.getName();
    PostResponse response = postService.createPost(username, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

**주요 로직**:
1. JWT에서 사용자 정보 추출
2. 콘텐츠 검증 (ContentValidationService)
3. 카테고리 기본값 설정 ("자유게시판")
4. 작성 시간 자동 설정 (`@PrePersist`)

**게시글 목록 조회 (페이지네이션)**:
```java
public Page<PostResponse> getAllPosts(Pageable pageable, String category, Long userId) {
    Page<Post> posts;
    if (category != null && !category.isEmpty()) {
        posts = postRepository.findByCategory(category, pageable);
    } else {
        posts = postRepository.findAll(pageable);
    }
    return posts.map(post -> toResponse(post, userId));
}
```

**응답 DTO 변환**:
```java
private PostResponse toResponse(Post post, Long userId) {
    Long likeCount = postLikeRepository.countByPostId(post.getId());
    Boolean isLiked = userId != null && 
        postLikeRepository.existsByPostIdAndUserId(post.getId(), userId);
    
    return PostResponse.builder()
        .id(post.getId())
        .authorId(post.getAuthor().getId())
        .authorNickname(post.getAuthor().getNickname())
        .title(post.getTitle())
        .content(post.getContent())
        .category(post.getCategory())
        .likeCount(likeCount)
        .isLiked(isLiked)  // 현재 사용자의 좋아요 여부
        .createdAt(post.getCreatedAt())
        .updatedAt(post.getUpdatedAt())
        .build();
}
```

**핵심 포인트**:
- 사용자별 좋아요 상태 포함 (`isLiked`)
- 실시간 좋아요 수 계산
- 인증되지 않은 사용자도 조회 가능 (userId가 null일 수 있음)

### 5.4 댓글 시스템 구현

**댓글 작성**:
```java
public CommentResponse createComment(String username, CommentCreateRequest request) {
    User author = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
    
    Post post = postRepository.findById(request.getPostId())
            .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다"));
    
    // 콘텐츠 검증
    if (!contentValidationService.validateContent(request.getContent())) {
        throw new RuntimeException("부적절한 내용이 포함되어 있습니다");
    }
    
    Comment comment = Comment.builder()
            .post(post)
            .author(author)
            .content(request.getContent())
            .build();
    
    Comment saved = commentRepository.save(comment);
    return toResponse(saved, null);
}
```

**댓글 페이지네이션**:
```java
public Page<CommentResponse> getCommentsByPostIdWithPagination(
        Long postId, int page, int size, Long userId) {
    Pageable pageable = PageRequest.of(page, size, 
        Sort.by(Sort.Direction.ASC, "createdAt"));
    Page<Comment> comments = commentRepository.findByPostId(postId, pageable);
    return comments.map(comment -> toResponse(comment, userId));
}
```

**정렬 전략**:
- 댓글은 작성 시간 오름차순 정렬 (최신 댓글이 아래)
- 페이지 크기 기본값: 20개

### 5.5 좋아요 기능 구현

**LikeService 설계**:
```java
@Service
@Transactional
public class LikeService {
    // 게시글/댓글 좋아요를 통합 관리
    public LikeResponse likePost(Long postId, Long userId) { ... }
    public LikeResponse likeComment(Long commentId, Long userId) { ... }
}
```

**중복 좋아요 방지**:
```java
public LikeResponse likePost(Long postId, Long userId) {
    // 이미 좋아요한 경우 무시
    if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
        return getPostLikeResponse(postId, userId);
    }
    
    PostLike postLike = PostLike.builder()
            .post(post)
            .user(user)
            .build();
    
    postLikeRepository.save(postLike);
    return getPostLikeResponse(postId, userId);
}
```

**좋아요 응답 구조**:
```java
@Builder
public class LikeResponse {
    private Long likeCount;      // 총 좋아요 수
    private Boolean isLiked;     // 현재 사용자의 좋아요 여부
}
```

**장점**:
- 한 번의 API 호출로 좋아요 수와 상태 모두 반환
- 프론트엔드에서 즉시 UI 업데이트 가능
- DB 제약조건으로 데이터 무결성 보장

### 5.6 권한 관리

**작성자 검증**:
```java
public PostResponse updatePost(Long postId, String username, PostUpdateRequest request) {
    Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다"));
    
    // 작성자 검증
    if (!post.getAuthor().getUsername().equals(username)) {
        throw new RuntimeException("게시글 수정 권한이 없습니다");
    }
    
    // 수정 로직...
}
```

**HTTP 상태 코드**:
- `403 FORBIDDEN`: 권한 없음
- `404 NOT_FOUND`: 리소스 없음
- `401 UNAUTHORIZED`: 인증 실패

### 5.7 콘텐츠 검증 시스템

**ContentValidationService 구현**:
```java
@Service
public class ContentValidationService {
    private final GptService gptService;
    
    @Value("${content.validation.enabled:false}")
    private boolean validationEnabled;
    
    public boolean validateContent(String content) {
        if (!validationEnabled) {
            return true; // 검증 비활성화 시 항상 통과
        }
        
        try {
            String prompt = String.format("""
                다음 텍스트가 부적절한 내용(욕설, 스팸, 혐오 표현 등)을 포함하고 있는지 검사해주세요.
                부적절한 내용이 있으면 "REJECT", 적절한 내용이면 "APPROVE"만 응답해주세요.
                
                텍스트: %s
                """, content);
            
            String response = gptService.analyzeWithGptString(prompt);
            return !response.contains("REJECT");
        } catch (Exception e) {
            log.error("콘텐츠 검증 중 오류 발생", e);
            return true; // 검증 실패 시 기본적으로 통과 (서비스 중단 방지)
        }
    }
}
```

**설계 철학**:
1. **선택적 활성화**: `content.validation.enabled`로 기능 on/off
2. **폴백 전략**: GPT API 실패 시 기본적으로 통과 (서비스 가용성 우선)
3. **확장 가능**: 향후 키워드 필터링, 정규식 등 추가 가능

### 5.8 게시글 상세 조회 최적화

**withComments 옵션**:
```java
@GetMapping("/{postId}")
public ResponseEntity<?> getPost(
        Authentication authentication,
        @PathVariable Long postId,
        @RequestParam(required = false, defaultValue = "false") boolean withComments
) {
    Long userId = getUserId(authentication);
    
    if (withComments) {
        PostDetailResponse post = postService.getPostWithComments(postId, userId, true);
        return ResponseEntity.ok(post);
    } else {
        PostResponse post = postService.getPost(postId, userId);
        return ResponseEntity.ok(post);
    }
}
```

**장점**:
- 필요할 때만 댓글 조회 (성능 최적화)
- 프론트엔드에서 선택적 데이터 로딩 가능
- API 응답 크기 최소화

### 5.9 성능 최적화 전략

**1. LAZY 로딩 활용**:
```java
@ManyToOne(fetch = FetchType.LAZY)
private User author;
```
- 필요할 때만 연관 엔티티 로드
- N+1 문제는 `@EntityGraph` 또는 JOIN FETCH로 해결

**2. 인덱스 최적화**:
```sql
CREATE INDEX idx_posts_category ON posts(category);
CREATE INDEX idx_posts_created_at ON posts(created_at DESC);
CREATE INDEX idx_comments_post_id ON comments(post_id);
```

**3. 페이지네이션**:
- Spring Data JPA의 `Pageable` 활용
- 대용량 데이터 효율적 처리
- 프론트엔드에서 무한 스크롤 구현 가능

### 5.10 에러 처리 및 로깅

**계층별 예외 처리**:
```java
try {
    PostResponse response = postService.createPost(username, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
} catch (RuntimeException e) {
    log.error("게시글 작성 실패", e);
    if (e.getMessage().contains("권한")) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
} catch (Exception e) {
    log.error("게시글 작성 중 예상치 못한 오류 발생", e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
}
```

**로깅 전략**:
- `@Slf4j` 어노테이션 활용
- 에러 발생 시 스택 트레이스 포함
- 사용자 친화적 에러 메시지 반환

### 5.11 API 문서화

**Swagger 어노테이션**:
```java
@RestController
@RequestMapping("/api/posts")
@Tag(name = "Posts", description = "게시글 관리 API")
public class PostController {
    
    @PostMapping
    @Operation(summary = "게시글 작성", description = "새로운 게시글을 작성합니다.")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<PostResponse> createPost(...) { ... }
}
```

**주요 기능**:
- 인터랙티브 API 문서 자동 생성
- JWT 인증 테스트 가능
- 요청/응답 스키마 자동 생성

### 5.12 구현 시 고려사항

**1. 트랜잭션 관리**:
```java
@Service
@Transactional
public class PostService {
    // 모든 메서드가 트랜잭션 내에서 실행
    // 예외 발생 시 자동 롤백
}
```

**2. DTO 패턴**:
- 엔티티 직접 노출 방지
- API 버전 관리 용이
- 순환 참조 방지

**3. 보안**:
- JWT 기반 인증
- 작성자만 수정/삭제 가능
- 콘텐츠 검증으로 부적절한 내용 필터링

---

## 🎯 커뮤니티 기능 핵심 포인트

### 1. 확장 가능한 아키텍처
- 기능별 패키지 분리 (`community` 패키지)
- 서비스 레이어 분리로 유지보수성 향상
- DTO 패턴으로 엔티티와 API 응답 분리

### 2. 사용자 경험 최적화
- 실시간 좋아요 수 및 상태 반환
- 페이지네이션으로 대용량 데이터 효율적 처리
- 선택적 댓글 로딩으로 성능 최적화

### 3. 데이터 무결성
- DB 제약조건으로 중복 좋아요 방지
- CASCADE 삭제로 데이터 일관성 유지
- 트랜잭션으로 원자성 보장

### 4. 보안 및 검증
- JWT 기반 인증
- 작성자 권한 검증
- GPT 기반 콘텐츠 검증 (선택적)

### 5. 성능 최적화
- LAZY 로딩으로 불필요한 쿼리 방지
- 인덱스 최적화
- 페이지네이션으로 메모리 효율성 향상

---

## 🔄 Step 6: 서비스 간 통신 최적화

### 6.1 WebClient 설정

**비동기 HTTP 통신**:
- Reactor 기반 논블로킹 I/O
- 타임아웃 및 재시도 로직
- 에러 핸들링 강화

**에러 처리 전략**:
```java
// PythonApiService.java
- 4xx/5xx 에러 상세 로깅
- Connection timeout 구분
- 사용자 친화적 에러 메시지
```

### 6.2 환경 변수 관리

**application.properties**:
```properties
python.api.url=${PYTHON_API_URL:http://localhost:8000}
```

**빌드 설정 (build.gradle)**:
```gradle
tasks.named('bootRun') {
    environment = System.getenv()  // 환경변수 전달
}
```

---

## 📝 Step 7: API 문서화 및 테스트

### 7.1 Swagger/OpenAPI 통합

**OpenApiConfig.java**:
- JWT Bearer 인증 스키마 설정
- API 그룹화 및 태그 관리

**주요 기능**:
- 인터랙티브 API 문서 (`/swagger-ui.html`)
- 인증 토큰 테스트 가능
- 요청/응답 스키마 자동 생성

### 7.2 DTO 패턴 적용

**엔티티와 DTO 분리**:
- 순환 참조 방지
- API 응답 구조 명확화
- LocalDateTime 직렬화 이슈 해결

**예시**:
```java
// UserAllergyResponse.java
@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
private LocalDateTime createdAt;
```

---

## 🛠️ Step 8: 고급 기능 구현

### 8.1 이메일 서비스

**EmailService.java**:
- 아이디 찾기
- 임시 비밀번호 발급
- Gmail SMTP 연동

### 8.2 알러지 관리

**UserController.java**:
- `GET /api/users/{userId}/allergies`: 알러지 목록
- `POST /api/users/{userId}/allergies`: 알러지 추가
- `DELETE /api/users/{userId}/allergies/{allergyId}`: 알러지 삭제

### 8.3 약물 검색

**MedicationController.java**:
- `GET /api/medications/search?name={약물명}`: 약물 검색
- 외부 API 연동 또는 내부 데이터베이스 검색

---

## 🚀 Step 9: 성능 최적화 및 에러 처리

### 9.1 JSON 직렬화 최적화

**JacksonConfig.java**:
- LocalDateTime ISO-8601 형식 직렬화
- 중첩 깊이 제한 증가 (2000)
- 타임존 설정 (Asia/Seoul)

### 9.2 에러 핸들링

**전략**:
- 계층별 예외 처리 (Controller → Service → Repository)
- 명확한 HTTP 상태 코드 반환
- 상세한 로깅 (SLF4J)

### 9.3 로깅 전략

**주요 로깅 포인트**:
- JWT 토큰 생성/검증
- 외부 API 호출 (Python, Vision, GPT)
- DB 쿼리 (개발 환경)
- 에러 발생 시 스택 트레이스

---

## 📦 Step 10: 배포 준비

### 10.1 환경 변수 관리

**필수 환경 변수**:
```bash
# Database
med_DB_PASSWORD

# JWT
JWT_SECRET

# OpenAI
OPENAI_API_KEY

# Google Vision
GOOGLE_APPLICATION_CREDENTIALS

# Python Service
PYTHON_API_URL
```

### 10.2 서비스 실행 스크립트

**Python 서비스 (start.sh)**:
- 가상환경 자동 활성화
- 의존성 설치 확인
- 환경변수 로드
- Uvicorn 서버 실행 (--reload)

### 10.3 테스트 환경 분리

**application-test.properties**:
- H2 인메모리 데이터베이스 사용
- 외부 서비스 Mock 설정

---

## 🎯 핵심 기술 포인트

### 1. 마이크로서비스 아키텍처
- **장점**: 기술 스택 선택의 자유, 독립적 배포, 확장성
- **구현**: WebClient를 통한 HTTP 통신, 비동기 처리

### 2. AI 통합
- **GPT API**: 증상 분석, 부작용 분석, OCR 텍스트 정리
- **프롬프트 엔지니어링**: 구조화된 JSON 응답 보장
- **폴백 메커니즘**: AI 실패 시 기본 로직 사용

### 3. 보안
- **JWT**: Stateless 인증
- **BCrypt**: 비밀번호 암호화
- **CORS**: 명시적 Origin 허용

### 4. 사용자 경험
- **페이지네이션**: 대용량 데이터 효율적 처리
- **실시간 피드백**: 좋아요 수, 사용자 상태 반영
- **에러 메시지**: 명확하고 사용자 친화적

---

## 📊 기술 스택 요약

### Backend (Java)
- Spring Boot 3.3.5
- Spring Data JPA
- Spring Security + JWT
- PostgreSQL
- Google Vision API
- WebClient (Reactive)

### AI Service (Python)
- FastAPI 0.115.0
- OpenAI GPT API
- Pydantic

### Infrastructure
- AWS RDS (PostgreSQL)
- 환경변수 기반 설정

---

## 🔮 향후 개선 방향

1. **캐싱 전략**: Redis 도입으로 GPT API 호출 최소화
2. **이미지 저장소**: AWS S3 또는 Cloudflare R2로 마이그레이션
3. **모니터링**: Prometheus + Grafana 연동
4. **로깅**: ELK Stack 또는 CloudWatch
5. **부하 분산**: Load Balancer, Auto Scaling
6. **API Rate Limiting**: 사용자별 요청 제한

---

## 💡 배운 점

1. **마이크로서비스 통신**: WebClient를 통한 비동기 통신의 중요성
2. **에러 핸들링**: 명확한 에러 메시지와 로깅의 가치
3. **AI 통합**: 프롬프트 설계와 폴백 전략의 필요성
4. **보안**: JWT 인증과 CORS 설정의 세심한 관리
5. **API 설계**: RESTful 원칙과 DTO 패턴의 유지보수성

---

이 프로젝트는 현대적인 웹 애플리케이션의 핵심 기술들을 학습하고 실전에 적용한 결과물입니다. 특히 AI와 전통적인 백엔드 개발의 융합을 통해 더 나은 사용자 경험을 제공할 수 있었습니다.

