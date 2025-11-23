import re
from typing import List
from app.services.gpt_service import GptService

class OcrService:
    def __init__(self):
        self.gpt_service = GptService()
    
    async def normalize_ocr_text(self, ocr_text: str) -> List[str]:
        """
        OCR 텍스트를 정규화하여 성분 목록을 추출합니다.
        """
        if not ocr_text or not ocr_text.strip():
            return []
        
        # GPT를 사용하여 성분 파싱
        try:
            prompt = f"""다음은 의약품 성분표의 OCR 텍스트입니다. 
이 텍스트에서 의약품 성분명만 추출하여 JSON 배열 형태로 반환해주세요.
각 성분은 순수한 성분명만 포함해야 하며, 함량 정보는 제외해주세요.

OCR 텍스트:
{ocr_text}

JSON 형식: {{"ingredients": ["성분1", "성분2", ...]}}
응답은 반드시 유효한 JSON 형식으로만 제공해야 합니다."""
            
            response = await self.gpt_service.analyze_with_gpt(prompt)
            
            if isinstance(response, dict) and "ingredients" in response:
                ingredients = response["ingredients"]
                if isinstance(ingredients, list):
                    # 빈 문자열 제거 및 정규화
                    normalized = [
                        ing.strip() 
                        for ing in ingredients 
                        if ing and ing.strip()
                    ]
                    return normalized
        except Exception as e:
            print(f"GPT를 통한 성분 파싱 실패: {e}, 기본 파싱 사용")
        
        # 기본 파싱 로직 (정규식 기반)
        return self._parse_ingredients_basic(ocr_text)
    
    def _parse_ingredients_basic(self, ocr_text: str) -> List[str]:
        """
        기본 파싱 로직 (정규식 기반)
        """
        # 쉼표나 줄바꿈으로 구분된 텍스트를 성분 리스트로 변환
        ingredients = re.split(r'[,\n]', ocr_text)
        normalized = [
            ing.strip()
            for ing in ingredients
            if ing.strip()
        ]
        return normalized

