from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List, Optional
from app.services.sideeffect_service import SideEffectService
from app.models.sideeffect_analysis import SideEffectAnalysisResponse

router = APIRouter()
sideeffect_service = SideEffectService()

class SideEffectAnalysisRequest(BaseModel):
    medication_names: List[str]
    medication_ingredients: List[List[str]]  # 각 약물별 성분 리스트
    allergy_ingredients: Optional[List[str]] = []
    description: Optional[str] = None

@router.post("/sideeffects", response_model=SideEffectAnalysisResponse)
async def analyze_side_effects(request: SideEffectAnalysisRequest):
    """
    사용자가 보고한 부작용 리스트를 받아 공통 성분 및 위험 패턴을 추출합니다.
    """
    try:
        result = await sideeffect_service.analyze_side_effects(
            medication_names=request.medication_names,
            medication_ingredients=request.medication_ingredients,
            allergy_ingredients=request.allergy_ingredients or [],
            description=request.description
        )
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"부작용 분석 중 오류 발생: {str(e)}")

