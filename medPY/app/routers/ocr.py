from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List
from app.services.ocr_service import OcrService

router = APIRouter()
ocr_service = OcrService()

class OcrNormalizeRequest(BaseModel):
    ocr_text: str

class OcrNormalizeResponse(BaseModel):
    normalized_ingredients: List[str]
    cleaned_text: str  # GPT로 정리된 텍스트

@router.post("/normalize", response_model=OcrNormalizeResponse)
async def normalize_ocr(request: OcrNormalizeRequest):
    """
    OCR 텍스트를 받아 정규화된 성분 리스트와 정리된 텍스트를 반환합니다.
    """
    try:
        import traceback
        result = await ocr_service.normalize_ocr_text(request.ocr_text)
        return OcrNormalizeResponse(
            normalized_ingredients=result.get("normalized_ingredients", []),
            cleaned_text=result.get("cleaned_text", request.ocr_text)
        )
    except Exception as e:
        import traceback
        error_detail = f"OCR 정규화 중 오류 발생: {str(e)}\n{traceback.format_exc()}"
        print(f"ERROR: {error_detail}")
        raise HTTPException(status_code=500, detail=f"OCR 정규화 중 오류 발생: {str(e)}")

