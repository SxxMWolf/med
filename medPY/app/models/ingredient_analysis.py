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

