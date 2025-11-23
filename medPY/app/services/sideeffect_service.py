from typing import List, Optional
from app.models.sideeffect_analysis import (
    SideEffectAnalysisResponse,
    SensitiveIngredient,
    CommonSideEffectIngredient
)
from app.services.gpt_service import GptService

class SideEffectService:
    def __init__(self):
        self.gpt_service = GptService()
    
    async def analyze_side_effects(
        self,
        medication_names: List[str],
        medication_ingredients: List[List[str]],
        allergy_ingredients: List[str],
        description: Optional[str] = None
    ) -> SideEffectAnalysisResponse:
        """
        부작용 보고를 분석하여 공통 성분과 위험 패턴을 추출합니다.
        """
        # 공통 성분 추출
        common_ingredients = self._extract_common_ingredients(medication_ingredients)
        
        # GPT를 사용한 분석
        prompt = self._build_side_effect_analysis_prompt(
            medication_names,
            medication_ingredients,
            common_ingredients,
            allergy_ingredients,
            description
        )
        response = await self.gpt_service.analyze_with_gpt(prompt)
        
        return SideEffectAnalysisResponse(**response)
    
    def _extract_common_ingredients(self, medication_ingredients: List[List[str]]) -> List[str]:
        """
        여러 약물의 공통 성분을 추출합니다.
        """
        if not medication_ingredients:
            return []
        
        if len(medication_ingredients) == 1:
            return medication_ingredients[0]
        
        # 첫 번째 약물의 성분을 기준으로 공통 성분 찾기
        common_ingredients = set(medication_ingredients[0])
        
        for ingredients in medication_ingredients[1:]:
            common_ingredients &= set(ingredients)
        
        return list(common_ingredients)
    
    def _build_side_effect_analysis_prompt(
        self,
        medication_names: List[str],
        medication_ingredients: List[List[str]],
        common_ingredients: List[str],
        allergy_ingredients: List[str],
        description: Optional[str] = None
    ) -> str:
        prompt = f"""사용자가 다음 약물들을 복용한 후 부작용을 경험했습니다:

{chr(10).join(f'- {name}' for name in medication_names)}

"""
        
        if description:
            prompt += f"부작용 설명: {description}\n\n"
        
        prompt += "각 약물의 성분 정보:\n"
        for name, ingredients in zip(medication_names, medication_ingredients):
            prompt += f"- {name}: {', '.join(ingredients)}\n"
        prompt += "\n"
        
        if common_ingredients:
            prompt += f"공통 성분: {', '.join(common_ingredients)}\n\n"
        
        if allergy_ingredients:
            prompt += f"""사용자의 알러지 성분 목록:
{chr(10).join(f'- {ing}' for ing in allergy_ingredients)}

"""
        
        prompt += """다음 정보를 포함하여 JSON 형식으로 응답해주세요:
1. 공통 성분 (common_ingredients): 모든 약물에 공통으로 포함된 성분 목록
2. 사용자 민감 가능 성분 (user_sensitive_ingredients): 사용자의 알러지 성분과 일치하거나 유사한 성분, 이유, 심각도
3. 많은 사람에게 부작용이 일어나는 성분 (common_side_effect_ingredients): 일반적으로 부작용을 일으키는 것으로 알려진 성분, 부작용 설명, 발생 빈도
4. 요약 (summary): 전체 분석 요약

JSON 형식:
{
  "common_ingredients": ["성분1", "성분2"],
  "user_sensitive_ingredients": [
    {
      "ingredient_name": "성분명",
      "reason": "민감한 이유",
      "severity": "MILD|MODERATE|SEVERE"
    }
  ],
  "common_side_effect_ingredients": [
    {
      "ingredient_name": "성분명",
      "side_effect_description": "부작용 설명",
      "frequency": "빈도"
    }
  ],
  "summary": "전체 분석 요약"
}"""
        
        return prompt

