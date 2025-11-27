from pydantic import BaseModel
from typing import List, Optional

class IngredientRisk(BaseModel):
    ingredient_name: str
    content: Optional[str] = None
    allergy_risk: Optional[str] = None
    risk_level: str  # LOW, MEDIUM, HIGH
    reason: Optional[str] = None

class IngredientAnalysisResponse(BaseModel):
    safety_level: str  # SAFE, CAUTION, DANGEROUS
    ingredient_risks: List[IngredientRisk]
    expected_side_effects: List[str]
    overall_assessment: str
    recommendations: List[str]
    # 식품 알러지 관련 필드 (선택적)
    food_allergy_risk: Optional[str] = None  # LOW, MEDIUM, HIGH
    matched_food_allergens: Optional[List[str]] = None  # 매칭된 식품 알러지 성분
    food_origin_excipients_detected: Optional[List[str]] = None  # 식품 유래 부형제 목록

