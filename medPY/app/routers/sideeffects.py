from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List, Optional
from app.services.sideeffect_service import SideEffectService
from app.models.sideeffect_analysis import SideEffectAnalysisResponse

router = APIRouter()
sideeffect_service = SideEffectService()

class SideEffectAnalysisRequest(BaseModel):
    medication_names: List[str]  # 그룹 이름 목록 (예: ["두유", "A, B, C, D", "E, F, G"])
    medication_ingredients: List[List[str]]  # 각 그룹별 성분 리스트 (그룹 내 약물들의 합집합)
    allergy_ingredients: Optional[List[str]] = []  # 하위 호환성을 위한 기존 필드
    medication_allergies: Optional[List[str]] = []  # 약물 알러지 (신규)
    food_allergies: Optional[List[str]] = []  # 식품 알러지 (신규)
    description: Optional[str] = None

@router.post("/sideeffects", response_model=SideEffectAnalysisResponse)
async def analyze_side_effects(request: SideEffectAnalysisRequest):
    """
    사용자가 보고한 부작용 리스트를 받아 공통 성분 및 위험 패턴을 추출합니다.
    
    medication_names는 그룹 이름 목록이고, medication_ingredients는 각 그룹별 성분 리스트입니다.
    각 그룹의 성분은 이미 합집합으로 처리되어 전달됩니다.
    모든 그룹의 교집합을 계산하여 공통 성분을 추출합니다.
    
    약물 알러지와 식품 알러지를 분리해서 받을 수 있으며,
    하위 호환성을 위해 allergy_ingredients도 지원합니다.
    """
    try:
        medication_allergies = request.medication_allergies or []
        food_allergies = request.food_allergies or []
        
        # 하위 호환성: medication_allergies와 food_allergies가 모두 없고
        # allergy_ingredients가 제공된 경우에만 변환
        if not medication_allergies and not food_allergies:
            if request.allergy_ingredients:
                medication_allergies = request.allergy_ingredients
        result = await sideeffect_service.analyze_side_effects(
            medication_names=request.medication_names,
            medication_ingredients=request.medication_ingredients,
            medication_allergies=medication_allergies,
            food_allergies=food_allergies,
            description=request.description
        )
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"부작용 분석 중 오류 발생: {str(e)}")

