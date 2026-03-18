import os
import psycopg
from psycopg.types.json import Jsonb
from dotenv import load_dotenv

# --- 설정 ---
load_dotenv(dotenv_path=os.path.join(os.path.dirname(__file__), "../../ai-server/.env"))

DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = os.getenv("DB_PORT", "5432")
DB_NAME = os.getenv("DB_NAME", "pnn-db")
DB_USER = os.getenv("DB_USER", "postgres")
DB_PASS = os.getenv("DB_PASS", "1234")

UNIT_MAP = {
    "밀리그램": "mg", "밀리리터": "mL", "그램": "g", "리터": "L",
    "마이크로그램": "μg", "마이크로리터": "μL", "나노그램": "ng",
    "국제단위": "IU", "단위": "U",
}

def _parse_amount(amount: str):
    """함량 문자열을 float으로 변환, 실패 시 원문 반환"""
    if not amount:
        return None
    try:
        return float(amount)
    except (ValueError, TypeError):
        return amount


def refine_to_ingredients(raw: str) -> list[dict] | None:
    """raw_ingredients 원문을 정제하여 프론트에서 바로 쓸 수 있는 dict 리스트로 반환.
    name, amount, unit 기준 중복 제거 (내수용/수출용1/수출용2 등 동일 성분 반복 제거).
    """
    if not raw or not raw.strip():
        return None

    ingredients = []
    seen: set[tuple[str, str, str]] = set()  # (name, amount_str, unit) 중복 체크
    blocks = raw.split(";")

    for block in blocks:
        trimmed = block.strip()
        if not trimmed:
            continue

        fields = trimmed.split("|")
        if len(fields) < 5:
            continue

        ingredient_name = fields[1].strip()
        if not ingredient_name:
            continue

        amount_raw = fields[3].strip() if len(fields) > 3 else ""
        unit_raw = fields[4].strip() if len(fields) > 4 else ""
        # 추가 정보가 있는 경우 (예: 비타민E(으)로서...), 빈 문자열은 None
        extra_note = (fields[5].strip() or None) if len(fields) > 5 else None

        amount_val = _parse_amount(amount_raw)
        unit = UNIT_MAP.get(unit_raw, unit_raw)
        # 중복 제거: name, amount, unit 동일하면 1개만 유지
        unique_key = (ingredient_name, str(amount_val), unit)
        if unique_key in seen:
            continue
        seen.add(unique_key)

        ingredients.append({
            "name": ingredient_name,
            "standard": fields[2].strip() if len(fields) > 2 else "",
            "amount": amount_val,
            "unit": unit,
            "note": extra_note,
        })

    return ingredients if ingredients else None

def main():
    conn_str = f"host={DB_HOST} port={DB_PORT} dbname={DB_NAME} user={DB_USER} password={DB_PASS}"
    print("1. drug_permit_detail 성분 데이터 정제 시작...")

    with psycopg.connect(conn_str) as conn:
        with conn.cursor() as cur:
            # 기존 parsed_ingredients 컬럼 비우기
            cur.execute("UPDATE drug_permit_detail SET parsed_ingredients = NULL")
            cleared = cur.rowcount
            print(f"   -> parsed_ingredients 컬럼 초기화: {cleared}건")

            # 원본 데이터 가져오기
            cur.execute("SELECT id, raw_ingredients FROM drug_permit_detail WHERE raw_ingredients IS NOT NULL AND raw_ingredients != ''")
            rows = cur.fetchall()

            total = len(rows)
            print(f"   -> 정제 대상 {total}건")

            if total == 0:
                conn.commit()
                print("   -> 정제할 raw_ingredients 데이터가 없습니다.")
                return

            update_data = []
            skipped = 0

            for row_id, raw_val in rows:
                ingredients = refine_to_ingredients(raw_val)
                if ingredients is None:
                    skipped += 1
                else:
                    update_data.append((Jsonb(ingredients), row_id))

            print(f"   -> 정제 완료. DB 업데이트({len(update_data)}건) 준비 중...")

            if update_data:
                cur.executemany(
                    "UPDATE drug_permit_detail SET parsed_ingredients = %s WHERE id = %s",
                    update_data,
                )
            
            conn.commit()

    print(f"2. 완료: {len(update_data)}건 업데이트, {skipped}건 스킵")

if __name__ == "__main__":
    main()