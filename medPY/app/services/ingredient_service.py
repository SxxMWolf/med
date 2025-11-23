from typing import List
from app.models.ingredient_analysis import IngredientAnalysisResponse, IngredientRisk
from app.services.gpt_service import GptService

class IngredientService:
    def __init__(self):
        self.gpt_service = GptService()
    
    async def analyze_ingredients(
        self,
        ingredients: List[str],
        allergy_ingredients: List[str]
    ) -> IngredientAnalysisResponse:
        """
        성분 리스트를 분석하여 위험도와 알러지 위험을 평가합니다.
        """
        # GPT를 사용한 분석
        prompt = self._build_ingredient_analysis_prompt(ingredients, allergy_ingredients)
        response = await self.gpt_service.analyze_with_gpt(prompt)
        
        return IngredientAnalysisResponse(**response)
    
    def _build_ingredient_analysis_prompt(
        self,
        ingredients: List[str],
        allergy_ingredients: List[str]
    ) -> str:
        prompt = f"""의약품 성분표에서 추출한 성분 목록:
{chr(10).join(f'- {ing}' for ing in ingredients)}

"""
        
        if allergy_ingredients:
            prompt += f"""사용자의 알러지 성분 목록:
{chr(10).join(f'- {ing}' for ing in allergy_ingredients)}

"""
        
        prompt += """다음 정보를 포함하여 JSON 형식으로 응답해주세요:
1. 각 성분의 위험도 분석 (ingredient_risks): 성분명, 함량 정보(가능한 경우), 알러지 위험도, 위험 수준, 이유
2. 예상 부작용 (expected_side_effects): 이 약물을 복용할 때 예상되는 부작용 목록
3. 전체 안전성 평가 (overall_assessment): 이 약물의 전체적인 안전성에 대한 평가
4. 복용 안전성 수준 (safety_level): SAFE, CAUTION, DANGEROUS 중 하나
5. 권장 사항 (recommendations): 복용 전 주의사항 및 권장사항

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
  "recommendations": ["권장사항1", "권장사항2"]
}"""
        
        return prompt

