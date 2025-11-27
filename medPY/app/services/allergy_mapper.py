"""
식품 알러지 → 의약품 부형제 매핑 유틸리티
식품 알러지가 있는 사용자가 복용하는 약물의 부형제에 알러지 유발 성분이 포함되어 있는지 확인합니다.
"""
from typing import Dict, List
import re
import logging

class FoodAllergenMapper:
    """
    식품 알러지와 의약품 부형제 간의 매핑을 관리합니다.
    예: 땅콩 알러지 → 땅콩유, 땅콩기름 체크
    """
    
    # 식품 알러지별 트리거 성분 매핑
    # Java 백엔드의 OcrAnalysisService와 일관성 유지
    FOOD_ALLERGEN_TRIGGERS: Dict[str, List[str]] = {
        "땅콩": ["땅콩", "땅콩유", "땅콩기름", "peanut", "peanut oil", "arachis oil"],
        "글루텐": ["글루텐", "밀전분", "밀단백질", "gluten", "wheat", "밀", "wheat starch", "wheat protein"],
        "유당": ["유당", "락토스", "lactose", "lactose monohydrate"],
        "갑각류": ["새우", "게", "크랩", "shrimp", "crab", "crustacean", "갑각류"],
        "계란": ["계란", "난백", "계란알부민", "egg", "albumin", "egg white", "ovalbumin", "lysozyme"],
        "대두": ["대두", "콩", "콩유", "대두유", "레시틴", "대두레시틴", "soy", "soybean", "lecithin", "soy lecithin"],
        "우유": ["우유", "카제인", "우유단백질", "milk", "casein", "milk protein", "whey", "유청"],
        "젤라틴": ["젤라틴", "소젤라틴", "돼지젤라틴", "gelatin", "bovine gelatin", "porcine gelatin", "gelatin capsule"],
        "견과류": ["호두", "아몬드", "헤이즐넛", "walnut", "almond", "hazelnut", "nuts"],
        "참깨": ["참깨", "sesame", "sesame oil", "sesame seed"]
    }
    
    @classmethod
    def get_triggers_for_allergy(cls, food_allergy: str) -> List[str]:
        """
        특정 식품 알러지에 대한 트리거 성분 목록을 반환합니다.
        
        Args:
            food_allergy: 식품 알러지 이름 (예: "땅콩", "계란")
            
        Returns:
            해당 알러지와 관련된 부형제 성분 목록
        """
        # 대소문자 구분 없이 검색
        normalized_allergy = food_allergy.strip()
        
        # 직접 매칭 시도
        if normalized_allergy in cls.FOOD_ALLERGEN_TRIGGERS:
            return cls.FOOD_ALLERGEN_TRIGGERS[normalized_allergy]
        
        # 소문자로 매칭 시도
        for key, triggers in cls.FOOD_ALLERGEN_TRIGGERS.items():
            if key.lower() == normalized_allergy.lower():
                return triggers
        
        # 기본값: 알러지 이름 자체를 포함
        return [normalized_allergy]
    
    @classmethod
    def _is_match(cls, trigger: str, ingredient: str) -> bool:
        """
        트리거 성분과 약물 성분이 매칭되는지 정확하게 확인합니다.
        
        단어 경계 기반 매칭으로 오탐 방지 (예: "락토스프리" → "락토스" 오탐 방지)
        복합어 처리 유지 (예: "소젤라틴" → "젤라틴" 매칭)
        
        Args:
            trigger: 트리거 성분 이름 (예: "락토스", "젤라틴")
            ingredient: 약물 성분 이름 (예: "락토스 모노하이드레이트", "락토스프리")
            
        Returns:
            매칭 여부 (True/False)
        """
        trigger_lower = trigger.lower().strip()
        ing_lower = ingredient.lower().strip()
        
        # 완전 일치
        if trigger_lower == ing_lower:
            return True
        
        # 특수 케이스: 복합어 처리
        # 예: "소젤라틴", "돼지젤라틴" 등은 "젤라틴"을 포함하므로 매칭
        gelatin_variants = ["젤라틴", "gelatin"]
        if trigger_lower in gelatin_variants:
            return "젤라틴" in ing_lower or "gelatin" in ing_lower
        
        # 단어 경계 기반 매칭 (정규식 사용)
        # 예: "락토스"는 "락토스 모노하이드레이트"에 매칭되지만
        # "락토스프리"에는 매칭되지 않음
        # \b는 단어 경계를 의미 (알파벳/숫자와 비알파벳 문자 사이)
        try:
            # 특수 문자 이스케이프
            escaped_trigger = re.escape(trigger_lower)
            pattern = r'\b' + escaped_trigger + r'\b'
            
            if re.search(pattern, ing_lower):
                return True
        except Exception as e:
            logging.warning(f"정규식 매칭 중 오류 발생: {e}, trigger: {trigger}, ingredient: {ingredient}")
            # 정규식 실패 시 기본 substring 체크로 폴백
            # 하지만 False Positive 방지를 위해 더 엄격한 조건 사용
            if trigger_lower in ing_lower:
                # 앞뒤로 단어 경계가 있는지 간단히 확인
                # (완벽하지 않지만 최소한의 방어)
                idx = ing_lower.find(trigger_lower)
                if idx == 0 or not ing_lower[idx-1].isalnum():
                    # 끝부분도 확인
                    end_idx = idx + len(trigger_lower)
                    if end_idx >= len(ing_lower) or not ing_lower[end_idx].isalnum():
                        return True
        
        return False
    
    @classmethod
    def find_matching_excipients(cls, food_allergies: List[str], ingredients: List[str]) -> Dict[str, List[str]]:
        """
        식품 알러지 목록과 약물 성분 목록을 비교하여 매칭되는 부형제를 찾습니다.
        
        Args:
            food_allergies: 사용자의 식품 알러지 목록
            ingredients: 약물 성분 목록 (주성분 + 부형제)
            
        Returns:
            {알러지: [매칭된 성분 목록]} 형태의 딕셔너리
        """
        matches = {}
        
        for food_allergy in food_allergies:
            triggers = cls.get_triggers_for_allergy(food_allergy)
            matched_ingredients = []
            
            for trigger in triggers:
                # 각 성분에 대해 정확한 매칭 확인
                for ingredient in ingredients:
                    if cls._is_match(trigger, ingredient):
                        if ingredient not in matched_ingredients:
                            matched_ingredients.append(ingredient)
            
            if matched_ingredients:
                matches[food_allergy] = matched_ingredients
        
        return matches
    
    @classmethod
    def check_food_allergy_risk(cls, food_allergies: List[str], ingredients: List[str]) -> Dict[str, any]:
        """
        식품 알러지 위험도를 평가합니다.
        
        Args:
            food_allergies: 사용자의 식품 알러지 목록
            ingredients: 약물 성분 목록
            
        Returns:
            위험도 평가 결과 딕셔너리
            {
                "has_risk": bool,
                "risk_level": "LOW" | "MEDIUM" | "HIGH",
                "matched_allergens": {알러지: [매칭된 성분]},
                "explanation": str
            }
        """
        try:
            if not food_allergies or not ingredients:
                return {
                    "has_risk": False,
                    "risk_level": "LOW",
                    "matched_allergens": {},
                    "explanation": "식품 알러지 또는 약물 성분 정보가 없습니다."
                }
            
            # 타입 검증
            if not isinstance(food_allergies, list) or not isinstance(ingredients, list):
                logging.warning(f"잘못된 타입: food_allergies={type(food_allergies)}, ingredients={type(ingredients)}")
                return {
                    "has_risk": False,
                    "risk_level": "LOW",
                    "matched_allergens": {},
                    "explanation": "입력값 형식이 올바르지 않습니다."
                }
            
            # 매칭 수행 (에러 처리 포함)
            try:
                matches = cls.find_matching_excipients(food_allergies, ingredients)
            except Exception as e:
                logging.error(f"부형제 매칭 중 오류 발생: {e}")
                return {
                    "has_risk": False,
                    "risk_level": "LOW",
                    "matched_allergens": {},
                    "explanation": "부형제 매칭 중 오류가 발생했습니다."
                }
            
            if not matches:
                return {
                    "has_risk": False,
                    "risk_level": "LOW",
                    "matched_allergens": {},
                    "explanation": "식품 알러지와 관련된 부형제가 감지되지 않았습니다."
                }
            
            # 위험도 평가: 매칭된 알러지 개수와 성분 개수에 따라 결정
            total_matches = sum(len(matched_ings) for matched_ings in matches.values())
            
            if total_matches >= 3:
                risk_level = "HIGH"
            elif total_matches >= 2:
                risk_level = "MEDIUM"
            else:
                risk_level = "LOW"
            
            # 설명 생성
            explanations = []
            for allergy, matched_ings in matches.items():
                explanations.append(f"{allergy} 알러지: {', '.join(matched_ings)}")
            
            explanation = f"식품 알러지 관련 부형제가 감지되었습니다. {', '.join(explanations)}"
            
            return {
                "has_risk": True,
                "risk_level": risk_level,
                "matched_allergens": matches,
                "explanation": explanation
            }
        except Exception as e:
            # 예상치 못한 오류 발생 시 안전한 기본값 반환
            logging.error(f"식품 알러지 위험도 평가 중 예상치 못한 오류: {e}", exc_info=True)
            return {
                "has_risk": False,
                "risk_level": "LOW",
                "matched_allergens": {},
                "explanation": "식품 알러지 평가 중 오류가 발생했습니다."
            }

