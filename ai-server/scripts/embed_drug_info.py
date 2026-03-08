import sys
import os

# ai-server 디렉토리를 Python 모듈 경로에 추가
sys.path.append(os.path.join(os.path.dirname(__file__), '..'))

from app.core.database import fetch_drug_easy_info
from app.services.ollama_embedding_service import get_embedding
from app.services.vector_store import save_embedding
from tqdm import tqdm

def process_and_embed_data():
    """
    drug_easy_info 데이터를 조회하여 임베딩을 생성하고 저장합니다.
    """
    print("Fetching drug info from database...")
    drugs = fetch_drug_easy_info()
    
    print(f"Total drugs to process: {len(drugs)}")
    
    for drug in tqdm(drugs, desc="Processing Drugs"):
        # 임베딩할 텍스트 구성
        # 상호작용 정보가 있으면 최우선으로 포함
        parts = []
        
        # 기본 정보 (약 이름, 제조사)
        base_info = f"약품명: {drug['item_name']}, 제조사: {drug['entp_name']}"
        parts.append(base_info)

        # 성분명 (drug_ingredients 조인 결과)
        if drug.get('ingredient_names'):
            parts.append(f"성분: {drug['ingredient_names']}")

        # 효능효과
        if drug['efcy_qesitm']:
             parts.append(f"효능효과: {drug['efcy_qesitm']}")
        
        # 용법용량
        if drug['use_method_qesitm']:
             parts.append(f"용법용량: {drug['use_method_qesitm']}")
        
        # 주의사항 (경고, 주의)
        if drug['atpn_warn_qesitm']:
            parts.append(f"경고: {drug['atpn_warn_qesitm']}")
        if drug['atpn_qesitm']:
            parts.append(f"주의사항: {drug['atpn_qesitm']}")
            
        # 상호작용 (가장 중요)
        if drug['intrc_qesitm']:
            parts.append(f"상호작용: {drug['intrc_qesitm']}")
            
        # 부작용
        if drug['se_qesitm']:
             parts.append(f"부작용: {drug['se_qesitm']}")
        
        full_text = "\n\n".join(parts)
        
        if not full_text.strip():
            continue

        try:
            # BGE-M3는 8K 토큰까지 지원. 한글 기준 6000자 정도로 제한.
            if len(full_text) > 6000:
                full_text = full_text[:6000]

            embedding = get_embedding(full_text)
            
            ingredient_list = [s.strip() for s in drug.get('ingredient_names', '').split(',') if s.strip()] if drug.get('ingredient_names') else []
            metadata = {
                "item_seq": drug['item_seq'],
                "item_name": drug['item_name'],
                "entp_name": drug['entp_name'],
                "ingredients": ingredient_list
            }
            
            save_embedding(full_text, embedding, metadata)
            
        except Exception as e:
            print(f"Error processing drug {drug['item_name']} ({drug['item_seq']}): {e}")
            continue

if __name__ == "__main__":
    process_and_embed_data()
