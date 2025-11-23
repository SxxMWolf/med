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
    ocr_text: str

@router.post("/normalize", response_model=OcrNormalizeResponse)
async def normalize_ocr(request: OcrNormalizeRequest):
    """
    OCR 텍스트를 받아 정규화된 성분 리스트를 반환합니다.
    """
    try:
        result = await ocr_service.normalize_ocr_text(request.ocr_text)
        return OcrNormalizeResponse(
            normalized_ingredients=result,
            ocr_text=request.ocr_text
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"OCR 정규화 중 오류 발생: {str(e)}")

