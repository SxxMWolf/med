from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List, Optional, Dict
from app.services.ingredient_service import IngredientService
from app.services.gpt_service import GptService
from app.models.ingredient_analysis import IngredientAnalysisResponse
import logging

router = APIRouter()
ingredient_service = IngredientService()
gpt_service = GptService()

class IngredientAnalysisRequest(BaseModel):
    ingredients: List[str]
    allergy_ingredients: Optional[List[str]] = []  # 하위 호환성을 위한 기존 필드
    medication_allergies: Optional[List[str]] = []  # 약물 알러지 (신규)
    food_allergies: Optional[List[str]] = []  # 식품 알러지 (신규)

@router.post("/ingredients", response_model=IngredientAnalysisResponse)
async def analyze_ingredients(request: IngredientAnalysisRequest):
    """
    성분 리스트를 받아 위험 성분 혹은 알러지 성분을 분석합니다.
    
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
        result = await ingredient_service.analyze_ingredients(
            ingredients=request.ingredients,
            medication_allergies=medication_allergies,
            food_allergies=food_allergies
        )
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"성분 분석 중 오류 발생: {str(e)}")

class FoodIngredientInferenceRequest(BaseModel):
    food_names: List[str]

class FoodIngredientInferenceResponse(BaseModel):
    food_ingredients: Dict[str, List[str]]  # {식품명: [성분1, 성분2, ...]}

def _build_food_ingredient_prompt(food_name: str) -> str:
    """식품 성분 추론을 위한 GPT 프롬프트 생성"""
    return f"""다음 식품의 주요 성분(원재료, 첨가물 포함)을 JSON 형식으로 나열해주세요.

식품명: {food_name}

응답 형식:
{{
    "food_name": "{food_name}",
    "ingredients": ["성분1", "성분2", "성분3", ...]
}}

성분은 가능한 한 구체적으로 나열해주세요. 예를 들어:
- "두유"의 경우: ["대두", "물", "설탕", "칼슘", "비타민D", "레시틴"]
- "우유"의 경우: ["우유단백질", "유당", "지방", "칼슘", "비타민A", "비타민D"]

JSON 형식으로만 응답해주세요."""

def _extract_ingredients_from_response(response: dict, food_name: str) -> List[str]:
    """GPT 응답에서 성분 리스트 추출"""
    if isinstance(response, dict) and "ingredients" in response:
        ingredients = response["ingredients"]
        if isinstance(ingredients, list):
            return ingredients
        logging.warning(f"식품 {food_name}의 성분이 리스트 형식이 아닙니다: {ingredients}")
    else:
        logging.warning(f"식품 {food_name}의 GPT 응답 형식이 올바르지 않습니다: {response}")
    return []

@router.post("/food-ingredients", response_model=FoodIngredientInferenceResponse)
async def infer_food_ingredients(request: FoodIngredientInferenceRequest):
    """
    식품명 목록을 받아 GPT를 통해 각 식품의 주요 성분을 추론합니다.
    
    예: "두유" -> ["대두", "물", "설탕", "칼슘", "비타민D"]
    """
    food_ingredients = {}
    
    for food_name in request.food_names:
        try:
            prompt = _build_food_ingredient_prompt(food_name)
            response = await gpt_service.analyze_with_gpt(prompt)
            food_ingredients[food_name] = _extract_ingredients_from_response(response, food_name)
        except Exception as e:
            logging.error(f"식품 {food_name}의 성분 추론 중 오류 발생: {e}")
            food_ingredients[food_name] = []  # 오류 발생 시 빈 리스트 반환
    
    return FoodIngredientInferenceResponse(food_ingredients=food_ingredients)

