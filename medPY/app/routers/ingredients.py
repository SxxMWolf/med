from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List, Optional
from app.services.ingredient_service import IngredientService
from app.models.ingredient_analysis import IngredientAnalysisResponse

router = APIRouter()
ingredient_service = IngredientService()

class IngredientAnalysisRequest(BaseModel):
    ingredients: List[str]
    allergy_ingredients: Optional[List[str]] = []

@router.post("/ingredients", response_model=IngredientAnalysisResponse)
async def analyze_ingredients(request: IngredientAnalysisRequest):
    """
    성분 리스트를 받아 위험 성분 혹은 알러지 성분을 분석합니다.
    """
    try:
        result = await ingredient_service.analyze_ingredients(
            ingredients=request.ingredients,
            allergy_ingredients=request.allergy_ingredients or []
        )
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"성분 분석 중 오류 발생: {str(e)}")

