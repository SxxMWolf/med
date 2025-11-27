from typing import List, Optional
import logging
from app.models.ingredient_analysis import IngredientAnalysisResponse, IngredientRisk
from app.services.gpt_service import GptService
from app.services.allergy_mapper import FoodAllergenMapper

class IngredientService:
    def __init__(self):
        self.gpt_service = GptService()
    
    async def analyze_ingredients(
        self,
        ingredients: List[str],
        medication_allergies: Optional[List[str]] = None,
        food_allergies: Optional[List[str]] = None
    ) -> IngredientAnalysisResponse:
        """
        성분 리스트를 분석하여 위험도와 알러지 위험을 평가합니다.
        
        Args:
            ingredients: 약물 성분 목록 (주성분 + 부형제)
            medication_allergies: 약물 알러지 목록 (라우터에서 하위 호환성 처리 완료)
            food_allergies: 식품 알러지 목록 (라우터에서 하위 호환성 처리 완료)
        """
        medication_allergies = medication_allergies or []
        food_allergies = food_allergies or []
        
        try:
            food_allergy_risk = FoodAllergenMapper.check_food_allergy_risk(
                food_allergies, 
                ingredients
            )
        except Exception as e:
            # 에러 발생 시 안전한 기본값 반환
            logging.warning(f"식품 알러지 위험도 평가 실패: {e}")
            food_allergy_risk = {
                "has_risk": False,
                "risk_level": "LOW",
                "matched_allergens": {},
                "explanation": "식품 알러지 평가 중 오류가 발생했습니다."
            }
        
        # GPT를 사용한 분석
        try:
            prompt = self._build_ingredient_analysis_prompt(
                ingredients, 
                medication_allergies, 
                food_allergies,
                food_allergy_risk
            )
            response = await self.gpt_service.analyze_with_gpt(prompt)
        except Exception as e:
            # GPT 응답 파싱 실패 시 기본 응답 생성
            logging.error(f"GPT 분석 실패: {e}")
            raise
        
        # 식품 알러지 위험 정보를 응답에 추가
        if food_allergy_risk.get("has_risk"):
            if "food_allergy_risk" not in response:
                response["food_allergy_risk"] = food_allergy_risk["risk_level"]
            if "matched_food_allergens" not in response:
                matched_allergens = []
                for allergy, matched_ings in food_allergy_risk.get("matched_allergens", {}).items():
                    matched_allergens.extend(matched_ings)
                response["matched_food_allergens"] = list(set(matched_allergens))
            if "food_origin_excipients_detected" not in response:
                excipients = []
                for matched_ings in food_allergy_risk.get("matched_allergens", {}).values():
                    excipients.extend(matched_ings)
                response["food_origin_excipients_detected"] = list(set(excipients))
        
        try:
            return IngredientAnalysisResponse(**response)
        except Exception as e:
            # Pydantic 검증 실패 시 에러 처리
            logging.error(f"응답 모델 검증 실패: {e}, response: {response}")
            raise
    
    def _build_ingredient_analysis_prompt(
        self,
        ingredients: List[str],
        medication_allergies: List[str],
        food_allergies: List[str],
        food_allergy_risk: dict
    ) -> str:
        prompt = f"""의약품 성분표에서 추출한 성분 목록:
{chr(10).join(f'- {ing}' for ing in ingredients)}

"""
        
        # 약물 알러지 정보
        if medication_allergies:
            prompt += f"""사용자의 약물 알러지 성분 목록:
{chr(10).join(f'- {ing}' for ing in medication_allergies)}

중요: 약물 알러지는 약물의 주성분(active ingredient)에 포함 여부를 확인하세요.

"""
        
        # 식품 알러지 정보
        if food_allergies:
            prompt += f"""사용자의 식품 알러지 목록:
{chr(10).join(f'- {ing}' for ing in food_allergies)}

중요: 식품 알러지는 약물의 부형제(excipient)에 포함 여부를 확인하세요.
식품 알러지 → 의약품 부형제 위험성 연결 규칙:
- 땅콩 알러지 → 땅콩유, 땅콩기름이 포함된 약물 피하기
- 계란 알러지 → 난백, 계란알부민이 포함된 약물 피하기
- 우유/유당 알러지 → 유당(락토스), 카제인이 포함된 약물 피하기
- 대두 알러지 → 콩유, 대두유, 레시틴이 포함된 약물 피하기
- 글루텐 알러지 → 밀전분, 글루텐이 포함된 약물 피하기
- 젤라틴 알러지 → 젤라틴이 포함된 약물 피하기

"""
            
            if food_allergy_risk.get("has_risk"):
                prompt += f"""⚠️ 식품 알러지 위험 감지:
위험도: {food_allergy_risk.get('risk_level', 'UNKNOWN')}
매칭된 알러지 성분: {', '.join(food_allergy_risk.get('matched_allergens', {}).keys())}
매칭된 부형제: {', '.join([ing for ings in food_allergy_risk.get('matched_allergens', {}).values() for ing in ings])}
설명: {food_allergy_risk.get('explanation', '')}

"""
        
        prompt += """다음 정보를 포함하여 JSON 형식으로 응답해주세요:
1. 각 성분의 위험도 분석 (ingredient_risks): 성분명, 함량 정보(가능한 경우), 알러지 위험도, 위험 수준, 이유
   - 약물 알러지: 주성분에 포함된 알러지 성분 체크
   - 식품 알러지: 부형제에 포함된 알러지 유발 성분 체크
2. 예상 부작용 (expected_side_effects): 이 약물을 복용할 때 예상되는 부작용 목록
3. 전체 안전성 평가 (overall_assessment): 이 약물의 전체적인 안전성에 대한 평가
   - 약물 알러지와 식품 알러지를 모두 고려한 평가
4. 복용 안전성 수준 (safety_level): SAFE, CAUTION, DANGEROUS 중 하나
5. 권장 사항 (recommendations): 복용 전 주의사항 및 권장사항
   - 식품 알러지 관련 주의사항 포함
6. 식품 알러지 기반 위험 분석 (food_allergy_risk): LOW, MEDIUM, HIGH (식품 알러지가 있는 경우)
7. 매칭된 식품 알러지 성분 (matched_food_allergens): 사용자 식품 알러지와 매칭된 부형제 목록 (식품 알러지가 있는 경우)
8. 식품 유래 부형제 감지 (food_origin_excipients_detected): 식품 유래 의약품 부형제 목록 (식품 알러지가 있는 경우)

JSON 형식:
{
  "safety_level": "SAFE|CAUTION|DANGEROUS",
  "ingredient_risks": [
    {
      "ingredient_name": "성분명",
      "content": "함량 정보",
      "allergy_risk": "위험도 설명",
      "risk_level": "LOW|MEDIUM|HIGH",
      "reason": "이유"
    }
  ],
  "expected_side_effects": ["부작용1", "부작용2"],
  "overall_assessment": "전체 평가",
  "recommendations": ["권장사항1", "권장사항2"],
  "food_allergy_risk": "LOW|MEDIUM|HIGH",
  "matched_food_allergens": ["매칭된 부형제1", "매칭된 부형제2"],
  "food_origin_excipients_detected": ["젤라틴", "유당", "레시틴"]
}"""
        
        return prompt

