from typing import List, Optional
import logging
from app.models.sideeffect_analysis import (
    SideEffectAnalysisResponse,
    SensitiveIngredient,
    CommonSideEffectIngredient
)
from app.services.gpt_service import GptService
from app.services.allergy_mapper import FoodAllergenMapper

class SideEffectService:
    def __init__(self):
        self.gpt_service = GptService()
    
    async def analyze_side_effects(
        self,
        medication_names: List[str],
        medication_ingredients: List[List[str]],
        medication_allergies: Optional[List[str]] = None,
        food_allergies: Optional[List[str]] = None,
        description: Optional[str] = None
    ) -> SideEffectAnalysisResponse:
        """
        부작용 보고를 분석하여 공통 성분과 위험 패턴을 추출합니다.
        
        Args:
            medication_names: 그룹 이름 목록 (예: ["두유", "A, B, C, D", "E, F, G"])
            medication_ingredients: 각 그룹별 성분 리스트 (그룹 내 약물들의 합집합)
                - 각 그룹의 성분은 이미 합집합으로 처리되어 전달됨
                - 모든 그룹의 교집합을 계산하여 공통 성분 추출
            medication_allergies: 약물 알러지 목록 (라우터에서 하위 호환성 처리 완료)
            food_allergies: 식품 알러지 목록 (라우터에서 하위 호환성 처리 완료)
            description: 부작용 설명
        """
        medication_allergies = medication_allergies or []
        food_allergies = food_allergies or []
        
        # 공통 성분 추출 (모든 그룹의 교집합)
        common_ingredients = self._extract_common_ingredients(medication_ingredients)
        
        # 모든 그룹의 성분 통합 (식품 알러지 체크용)
        all_ingredients = []
        for ingredients in medication_ingredients:
            all_ingredients.extend(ingredients)
        all_ingredients = list(set(all_ingredients))  # 중복 제거
        
        try:
            food_allergy_risk = FoodAllergenMapper.check_food_allergy_risk(
                food_allergies,
                all_ingredients
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
            prompt = self._build_side_effect_analysis_prompt(
                medication_names,
                medication_ingredients,
                common_ingredients,
                medication_allergies,
                food_allergies,
                food_allergy_risk,
                description
            )
            response = await self.gpt_service.analyze_with_gpt(prompt)
        except Exception as e:
            # GPT 응답 파싱 실패 시 에러 전파
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
            
            # food_allergy_analysis 객체 생성
            detected_ingredients = []
            matched_allergens_list = []
            for allergy, matched_ings in food_allergy_risk.get("matched_allergens", {}).items():
                matched_allergens_list.append(allergy)
                detected_ingredients.extend(matched_ings)
            
            response["food_allergy_analysis"] = {
                "detected_food_origin_ingredients": list(set(detected_ingredients)),
                "matched_allergens": matched_allergens_list,
                "risk_assessment": food_allergy_risk.get("explanation", "")
            }
        
        try:
            return SideEffectAnalysisResponse(**response)
        except Exception as e:
            # Pydantic 검증 실패 시 에러 처리
            logging.error(f"응답 모델 검증 실패: {e}, response: {response}")
            raise
    
    def _extract_common_ingredients(self, medication_ingredients: List[List[str]]) -> List[str]:
        """
        여러 그룹의 공통 성분을 추출합니다 (그룹 간 교집합).
        
        각 그룹의 성분은 이미 합집합으로 처리되어 전달되므로,
        모든 그룹의 교집합을 계산하여 공통 성분을 추출합니다.
        
        Args:
            medication_ingredients: 각 그룹별 성분 리스트 (그룹 내 약물들의 합집합)
        
        Returns:
            모든 그룹에 공통으로 포함된 성분 목록
        """
        if not medication_ingredients:
            return []
        
        if len(medication_ingredients) == 1:
            return medication_ingredients[0]
        
        # 첫 번째 그룹의 성분을 기준으로 공통 성분 찾기
        common_ingredients = set(medication_ingredients[0])
        
        # 나머지 그룹들과 교집합 계산
        for ingredients in medication_ingredients[1:]:
            common_ingredients &= set(ingredients)
        
        return list(common_ingredients)
    
    def _build_side_effect_analysis_prompt(
        self,
        medication_names: List[str],
        medication_ingredients: List[List[str]],
        common_ingredients: List[str],
        medication_allergies: List[str],
        food_allergies: List[str],
        food_allergy_risk: dict,
        description: Optional[str] = None
    ) -> str:
        prompt = f"""사용자가 다음 약물 그룹들을 복용한 후 부작용을 경험했습니다:

{chr(10).join(f'- 그룹: {name}' for name in medication_names)}

"""
        
        if description:
            prompt += f"부작용 설명: {description}\n\n"
        
        prompt += "각 그룹의 성분 정보 (그룹 내 약물들의 합집합):\n"
        for name, ingredients in zip(medication_names, medication_ingredients):
            prompt += f"- 그룹 ({name}): {', '.join(ingredients)}\n"
        prompt += "\n"
        
        if common_ingredients:
            prompt += f"모든 그룹에 공통으로 포함된 성분 (그룹 간 교집합): {', '.join(common_ingredients)}\n\n"
        
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
        
        prompt += """다음 정보를 포함하여 JSON 형식으로 응답해주세요. **모든 텍스트는 반드시 한글로 작성해주세요.**

1. 공통 성분 (common_ingredients): 모든 그룹에 공통으로 포함된 성분 목록 (그룹 간 교집합으로 도출된 공통 성분)
2. 사용자 민감 가능 성분 (user_sensitive_ingredients): 사용자의 약물 알러지 또는 식품 알러지 성분과 일치하거나 유사한 성분, 이유, 심각도
   - 약물 알러지: 주성분에 포함된 알러지 성분 체크
   - 식품 알러지: 부형제에 포함된 알러지 유발 성분 체크
   - 심각도(severity) 판단 기준:
     * SEVERE: 생명을 위협할 수 있는 심각한 알러지 반응 (아나필락시스, 셀리악병, 심각한 식품 알러지 등)
     * MODERATE: 중등도 알러지 반응 (발진, 두드러기, 호흡곤란 등)
     * MILD: 경미한 알러지 반응 (가벼운 발진, 소화불량 등)
3. 많은 사람에게 부작용이 일어나는 성분 (common_side_effect_ingredients): 일반적으로 부작용을 일으키는 것으로 알려진 성분, 부작용 설명, 발생 빈도
   - **중요**: side_effect_description과 frequency는 반드시 한글로 작성해주세요.
   - side_effect_description 예시: "알레르기 반응, 위장 장애", "졸음, 구강 건조", "위장 불편감, 두통" (영어로 작성하지 마세요)
   - frequency는 "흔함", "보통", "드묾" 등의 한글로 작성해주세요. (Common → 흔함, Uncommon → 보통, Rare → 드묾)
4. 요약 (summary): 전체 분석 요약 (한글로 작성)
   - 약물 알러지와 식품 알러지를 모두 고려한 요약
5. 식품 알러지 기반 위험 분석 (food_allergy_risk): LOW, MEDIUM, HIGH (식품 알러지가 있는 경우)
6. 매칭된 식품 알러지 성분 (matched_food_allergens): 사용자 식품 알러지와 매칭된 부형제 목록 (식품 알러지가 있는 경우)
7. 식품 유래 부형제 감지 (food_origin_excipients_detected): 식품 유래 의약품 부형제 목록 (식품 알러지가 있는 경우)

JSON 형식 (모든 텍스트는 한글로 작성):
{
  "common_ingredients": ["성분1", "성분2"],
  "user_sensitive_ingredients": [
    {
      "ingredient_name": "성분명",
      "reason": "민감한 이유 (한글로 작성)",
      "severity": "MILD|MODERATE|SEVERE"
    }
  ],
  "common_side_effect_ingredients": [
    {
      "ingredient_name": "성분명",
      "side_effect_description": "부작용 설명 (한글로 작성, 예: 알레르기 반응, 위장 장애 등)",
      "frequency": "빈도 (한글로 작성, 예: 흔함, 보통, 드묾)"
    }
  ],
  "summary": "전체 분석 요약 (한글로 작성)",
  "food_allergy_risk": "LOW|MEDIUM|HIGH",
  "matched_food_allergens": ["매칭된 부형제1", "매칭된 부형제2"],
  "food_origin_excipients_detected": ["젤라틴", "유당", "레시틴"]
}

**중요**: 모든 설명, 이유, 부작용 설명, 빈도, 요약은 반드시 한글로 작성해주세요. 영어로 작성하지 마세요."""
        
        return prompt

