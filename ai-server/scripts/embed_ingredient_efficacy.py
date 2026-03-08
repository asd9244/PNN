"""
ingredient_efficacy 테이블 데이터를 임베딩하여 knowledge_embeddings에 적재합니다.
RAG 검색 시 성분 단위 지식 보강용 (drug_easy_info 보완).
"""
import sys
import os

sys.path.append(os.path.join(os.path.dirname(__file__), '..'))

from app.core.database import fetch_ingredient_efficacy
from app.services.ollama_embedding_service import get_embedding
from app.services.vector_store import save_embedding, clear_embeddings_by_source
from tqdm import tqdm


def process_and_embed_data():
    """
    ingredient_efficacy 데이터를 조회하여 임베딩을 생성하고 저장합니다.
    기존 ingredient_efficacy 임베딩을 삭제한 후 재적재합니다.
    """
    print("Clearing existing ingredient_efficacy embeddings...")
    clear_embeddings_by_source('ingredient_efficacy')

    print("Fetching ingredient_efficacy from database...")
    ingredients = fetch_ingredient_efficacy()

    print(f"Total ingredients to process: {len(ingredients)}")

    for ing in tqdm(ingredients, desc="Processing Ingredients"):
        parts = []
        parts.append(f"성분: {ing['gnl_nm']}")
        if ing.get('div_nm'):
            parts.append(f"약효분류: {ing['div_nm']}")
        if ing.get('fomn_tp_nm'):
            parts.append(f"제형: {ing['fomn_tp_nm']}")
        if ing.get('injc_pth_nm'):
            parts.append(f"투여경로: {ing['injc_pth_nm']}")
        if ing.get('iqty_txt') and ing.get('unit'):
            parts.append(f"함량: {ing['iqty_txt']} {ing['unit']}")

        full_text = "\n\n".join(parts)
        if not full_text.strip():
            continue

        try:
            if len(full_text) > 6000:
                full_text = full_text[:6000]

            embedding = get_embedding(full_text)
            metadata = {
                "gnl_nm_cd": ing['gnl_nm_cd'],
                "gnl_nm": ing['gnl_nm'],
                "div_nm": ing.get('div_nm'),
            }
            save_embedding(
                full_text,
                embedding,
                metadata,
                source='ingredient_efficacy',
                category='drug_ingredient',
            )
        except Exception as e:
            print(f"Error processing {ing.get('gnl_nm', '')} ({ing.get('gnl_nm_cd', '')}): {e}")
            continue


if __name__ == "__main__":
    process_and_embed_data()
