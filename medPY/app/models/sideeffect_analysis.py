from pydantic import BaseModel
from typing import List, Optional

class SensitiveIngredient(BaseModel):
    ingredient_name: str
    reason: str
    severity: str  # MILD, MODERATE, SEVERE
    is_food_origin: Optional[bool] = None  # 식품 유래 성분 여부
    food_allergy_match: Optional[bool] = None  # 식품 알러지와 매칭 여부

class CommonSideEffectIngredient(BaseModel):
    ingredient_name: str
    side_effect_description: str
    frequency: str

class FoodAllergyAnalysis(BaseModel):
    detected_food_origin_ingredients: List[str]  # 검출된 식품 유래 성분
    matched_allergens: List[str]  # 매칭된 식품 알러지
    risk_assessment: str  # 위험도 평가

class SideEffectAnalysisResponse(BaseModel):
    common_ingredients: List[str]
    user_sensitive_ingredients: List[SensitiveIngredient]
    common_side_effect_ingredients: List[CommonSideEffectIngredient]
    summary: str
    # 식품 알러지 관련 필드 (선택적)
    food_allergy_risk: Optional[str] = None  # LOW, MEDIUM, HIGH
    matched_food_allergens: Optional[List[str]] = None  # 매칭된 식품 알러지 성분
    food_origin_excipients_detected: Optional[List[str]] = None  # 식품 유래 부형제 목록
    food_allergy_analysis: Optional[FoodAllergyAnalysis] = None  # 식품 알러지 분석 결과

