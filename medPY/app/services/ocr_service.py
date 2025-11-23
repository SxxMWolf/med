import re
from typing import List, Dict, Any
from app.services.gpt_service import GptService

class OcrService:
    def __init__(self):
        self.gpt_service = GptService()
    
    async def normalize_ocr_text(self, ocr_text: str) -> Dict[str, Any]:
        """
        OCR 텍스트를 정규화하여 성분 목록을 추출하고, 텍스트를 정리합니다.
        반환값: {"normalized_ingredients": List[str], "cleaned_text": str}
        """
        if not ocr_text or not ocr_text.strip():
            return {"normalized_ingredients": [], "cleaned_text": ""}
        
        # GPT를 사용하여 텍스트 정리 및 성분 파싱
        cleaned_text = ocr_text
        ingredients = []
        
        # GPT API 키가 있는지 확인
        try:
            # 먼저 텍스트를 보기 좋게 정리
            clean_prompt = f"""다음은 의약품 성분표의 OCR 텍스트입니다. 
이 텍스트를 보기 좋게 정리하여 가독성 높은 형식으로 변환해주세요.
- 불필요한 공백 제거
- 줄바꿈 정리
- 성분명과 함량을 명확하게 구분
- 읽기 쉬운 형식으로 포맷팅

OCR 텍스트:
{ocr_text}

중요: 반드시 다음 JSON 형식으로만 응답하세요:
{{"cleaned_text": "정리된 텍스트를 여기에 문자열로 넣으세요"}}

cleaned_text는 반드시 문자열(string)이어야 하며, 다른 필드나 중첩된 객체를 포함하지 마세요.
응답은 반드시 유효한 JSON 형식으로만 제공해야 합니다."""
            
            try:
                clean_response = await self.gpt_service.analyze_with_gpt(clean_prompt)
                if isinstance(clean_response, dict):
                    # cleaned_text 필드 확인
                    if "cleaned_text" in clean_response:
                        cleaned_text_value = clean_response["cleaned_text"]
                        # 문자열인지 확인
                        if isinstance(cleaned_text_value, str):
                            cleaned_text = cleaned_text_value
                            print(f"GPT 텍스트 정리 성공: 텍스트 길이={len(cleaned_text)}")
                        else:
                            print(f"GPT 응답의 cleaned_text가 문자열이 아닙니다: {type(cleaned_text_value)}, 원본 텍스트 사용")
                            cleaned_text = ocr_text
                    else:
                        # cleaned_text가 없으면 전체 응답을 문자열로 변환 시도
                        print(f"GPT 응답에 cleaned_text 필드가 없습니다: {list(clean_response.keys())}")
                        cleaned_text = ocr_text
                else:
                    print(f"GPT 응답이 딕셔너리가 아닙니다: {type(clean_response)}, 원본 텍스트 사용")
                    cleaned_text = ocr_text
            except Exception as e:
                print(f"GPT 텍스트 정리 실패: {e}, 원본 텍스트 사용")
                cleaned_text = ocr_text  # 실패 시 원본 텍스트 사용
            
            # 정리된 텍스트에서 성분 추출
            ingredient_prompt = f"""다음은 의약품 성분표의 정리된 텍스트입니다. 
이 텍스트에서 의약품 성분명만 추출하여 JSON 배열 형태로 반환해주세요.
각 성분은 순수한 성분명만 포함해야 하며, 함량 정보는 제외해주세요.

텍스트:
{cleaned_text}

JSON 형식: {{"ingredients": ["성분1", "성분2", ...]}}
응답은 반드시 유효한 JSON 형식으로만 제공해야 합니다."""
            
            try:
                ingredient_response = await self.gpt_service.analyze_with_gpt(ingredient_prompt)
                if isinstance(ingredient_response, dict) and "ingredients" in ingredient_response:
                    ingredients = ingredient_response["ingredients"]
                    if isinstance(ingredients, list):
                        # 빈 문자열 제거 및 정규화
                        ingredients = [
                            ing.strip() 
                            for ing in ingredients 
                            if ing and ing.strip()
                        ]
                        print(f"GPT 성분 추출 성공: 성분 개수={len(ingredients)}")
            except Exception as e:
                print(f"GPT 성분 추출 실패: {e}, 기본 파싱 사용")
                ingredients = []  # 실패 시 빈 리스트, 나중에 기본 파싱 사용
                
        except Exception as e:
            print(f"GPT 처리 중 예상치 못한 오류 발생: {e}, 기본 파싱 사용")
            ingredients = []
        
        # GPT가 실패했거나 성분을 추출하지 못한 경우 기본 파싱 사용
        if not ingredients:
            ingredients = self._parse_ingredients_basic(cleaned_text if cleaned_text != ocr_text else ocr_text)
            print(f"기본 파싱 사용: 성분 개수={len(ingredients)}")
        
        # 최종 검증: cleaned_text가 문자열인지 확인
        if not isinstance(cleaned_text, str):
            print(f"경고: cleaned_text가 문자열이 아닙니다 ({type(cleaned_text)}), 원본 텍스트로 대체")
            cleaned_text = ocr_text if ocr_text else ""
        
        return {
            "normalized_ingredients": ingredients,
            "cleaned_text": str(cleaned_text)  # 안전하게 문자열로 변환
        }
    
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

