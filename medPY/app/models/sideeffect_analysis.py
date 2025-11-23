from pydantic import BaseModel
from typing import List

class SensitiveIngredient(BaseModel):
    ingredient_name: str
    reason: str
    severity: str  # MILD, MODERATE, SEVERE

class CommonSideEffectIngredient(BaseModel):
    ingredient_name: str
    side_effect_description: str
    frequency: str

class SideEffectAnalysisResponse(BaseModel):
    common_ingredients: List[str]
    user_sensitive_ingredients: List[SensitiveIngredient]
    common_side_effect_ingredients: List[CommonSideEffectIngredient]
    summary: str

