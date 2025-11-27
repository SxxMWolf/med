// 인증 관련 타입
export interface RegisterRequest {
  username: string;
  password: string;
  email: string;
  nickname: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  user: UserInfo;
}

export interface UserInfo {
  id: number;
  username: string;
  email: string;
  nickname: string;
}

export interface UserResponse {
  id: number;
  username: string;
  email: string;
  nickname: string;
  createdAt: string;
  updatedAt: string;
}

// 알러지 관련 타입
export type AllergyType = 'MEDICATION' | 'FOOD';
export type FoodAllergyCategory = 
  | 'PEANUT' 
  | 'GLUTEN' 
  | 'LACTOSE' 
  | 'SHELLFISH' 
  | 'EGG' 
  | 'SOY' 
  | 'TREE_NUT' 
  | 'FISH' 
  | 'OTHER';

export interface UserAllergy {
  id: number;
  user: User;
  ingredientName: string;
  description?: string;
  severity: 'MILD' | 'MODERATE' | 'SEVERE';
  allergyType?: AllergyType; // 약물 알러지 또는 식품 알러지
  foodCategory?: FoodAllergyCategory; // 식품 알러지인 경우 카테고리
  createdAt: string;
  updatedAt: string;
}

export interface User {
  id: number;
  username: string;
  email: string;
  nickname: string;
  allergies: UserAllergy[];
  createdAt: string;
  updatedAt: string;
}

// 증상 분석 관련 타입
export interface SymptomAnalysisRequest {
  userId: number;
  symptomText: string;
}

export interface RecommendedMedication {
  name: string;
  reason: string;
  dosage?: string;
}

export interface NotRecommendedMedication {
  name: string;
  reason: string;
  allergicIngredients: string[];
  foodAllergyRisk?: FoodAllergyRisk; // 식품 알러지 기반 위험 분석
  matchedFoodAllergens?: string[]; // 사용자 식품 알러지와 매칭된 성분 리스트
  foodOriginExcipientsDetected?: string[]; // 식품 유래 의약품 부형제 목록
}

export interface FoodAllergyRisk {
  hasRisk: boolean;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  matchedIngredients: string[];
  explanation: string;
}

export interface SymptomAnalysisResponse {
  recommendedMedications: RecommendedMedication[];
  notRecommendedMedications: NotRecommendedMedication[];
  precautions: string[];
  foodAllergyWarnings?: string[]; // 식품 알러지 관련 경고사항
}

// 부작용 분석 관련 타입
export interface GroupRequest {
  type: 'food' | 'drug';
  items: string[];
}

export interface SideEffectAnalysisRequest {
  userId?: number; // 선택적 (비로그인 사용자 지원)
  groups: GroupRequest[]; // 사용자가 정의한 그룹 목록
  description?: string;
}

export interface SensitiveIngredient {
  ingredientName: string;
  reason: string;
  severity: string;
  isFoodOrigin?: boolean; // 식품 유래 성분 여부
  foodAllergyMatch?: boolean; // 식품 알러지와 매칭 여부
}

export interface CommonSideEffectIngredient {
  ingredientName: string;
  sideEffectDescription: string;
  frequency: string;
}

export interface SideEffectAnalysisResponse {
  commonIngredients: string[];
  userSensitiveIngredients: SensitiveIngredient[];
  commonSideEffectIngredients: CommonSideEffectIngredient[];
  summary: string;
  foodAllergyAnalysis?: {
    detectedFoodOriginIngredients: string[];
    matchedAllergens: string[];
    riskAssessment: string;
  };
}

// OCR 분석 관련 타입
export interface OcrAnalysisRequest {
  userId: number;
  imageData: string;
  base64?: boolean;
}

export interface IngredientRisk {
  ingredientName: string;
  content: string;
  allergyRisk: string;
  riskLevel: string;
  reason: string;
  isFoodOrigin?: boolean; // 식품 유래 성분 여부
  foodAllergyMatch?: boolean; // 식품 알러지와 매칭 여부
}

export interface IngredientAnalysis {
  safetyLevel: string;
  ingredientRisks: IngredientRisk[];
  expectedSideEffects: string[];
  overallAssessment: string;
  recommendations: string[];
  foodAllergyRisk?: FoodAllergyRisk; // 식품 알러지 기반 위험 분석
  matchedFoodAllergens?: string[]; // 사용자 식품 알러지와 매칭된 성분 리스트
  foodOriginExcipientsDetected?: string[]; // 식품 유래 의약품 부형제 목록
}

export interface OcrAnalysisResponse {
  ocrText: string;
  extractedIngredients: string[];
  analysis: IngredientAnalysis;
}

// 약물 정보 타입
export interface MedicationInfo {
  name: string;
  ingredients: string[];
  excipients?: string[]; // 부형제 목록
  description?: string;
  manufacturer?: string;
}

// 게시글 관련 타입
export interface PostCreateRequest {
  title: string;
  content: string;
  category?: string;
}

export interface PostUpdateRequest {
  title: string;
  content: string;
  category?: string;
}

export interface PostResponse {
  id: number;
  authorId: number;
  authorNickname: string;
  title: string;
  content: string;
  category?: string;
  likeCount: number;
  isLiked: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Pageable {
  page: number;
  size: number;
  sort?: string[];
}

export interface PagePostResponse {
  content: PostResponse[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface LikeResponse {
  likeCount: number;
  isLiked: boolean;
}

// 댓글 관련 타입
export interface CommentCreateRequest {
  postId: number;
  content: string;
}

export interface CommentUpdateRequest {
  content: string;
}

export interface CommentResponse {
  id: number;
  postId: number;
  authorId: number;
  authorNickname: string;
  content: string;
  likeCount: number;
  isLiked: boolean;
  createdAt: string;
}

export interface PageCommentResponse {
  content: CommentResponse[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

// 이미지 업로드 관련 타입
export interface ImageUploadResponse {
  imageUrl: string;
}

// 인증 추가 타입
export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface ChangeNicknameRequest {
  nickname: string;
}

export interface MessageResponse {
  message: string;
}

export interface FindUsernameRequest {
  email: string;
}

export interface FindPasswordRequest {
  username: string;
  email: string;
}

