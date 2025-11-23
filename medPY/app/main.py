from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv
from app.routers import ingredients, sideeffects, ocr

# 환경변수 로드
load_dotenv()

app = FastAPI(title="Med Analysis Service", version="1.0.0")

# CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 프로덕션에서는 특정 도메인으로 제한
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 라우터 등록
app.include_router(ingredients.router, prefix="/analyze", tags=["ingredients"])
app.include_router(sideeffects.router, prefix="/analyze", tags=["sideeffects"])
app.include_router(ocr.router, prefix="/ocr", tags=["ocr"])

@app.get("/")
async def root():
    return {"message": "Med Analysis Service API", "version": "1.0.0"}

@app.get("/health")
async def health_check():
    return {"status": "healthy"}

