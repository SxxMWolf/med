import os
import json
import httpx
from typing import Dict, Any, Optional
from dotenv import load_dotenv

load_dotenv()

class GptService:
    def __init__(self):
        self.api_key = os.getenv("OPENAI_API_KEY", "")
        self.api_url = os.getenv("OPENAI_API_URL", "https://api.openai.com/v1/chat/completions")
        self.model = os.getenv("GPT_MODEL", "gpt-4o-mini")
    
    async def analyze_with_gpt(self, prompt: str) -> Dict[str, Any]:
        """
        GPT API를 호출하여 분석을 수행합니다.
        """
        if not self.api_key:
            raise ValueError("OPENAI_API_KEY 환경변수가 설정되지 않았습니다")
        
        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json"
        }
        
        payload = {
            "model": self.model,
            "messages": [
                {"role": "system", "content": "You are a medical assistant. Always respond in valid JSON format only."},
                {"role": "user", "content": prompt}
            ],
            "temperature": 0.3,
            "response_format": {"type": "json_object"}
        }
        
        async with httpx.AsyncClient(timeout=60.0) as client:
            response = await client.post(self.api_url, headers=headers, json=payload)
            response.raise_for_status()
            
            result = response.json()
            
            if "choices" not in result or not result["choices"]:
                raise ValueError("GPT API 응답에 choices가 없습니다")
            
            content = result["choices"][0]["message"]["content"]
            
            # JSON 파싱
            try:
                return json.loads(content)
            except json.JSONDecodeError as e:
                raise ValueError(f"GPT 응답을 JSON으로 파싱할 수 없습니다: {e}, 응답: {content}")

