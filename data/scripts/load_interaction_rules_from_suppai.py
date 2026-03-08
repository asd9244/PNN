"""
SUPP.AI JSON → interaction_rules 테이블 적재
- cui_metadata.json, sentence_dict.json 사용
- drug-supplement 쌍만 필터
- level=CAUTION, description=evidence 1~3문장 (최대 2000자)
"""
import argparse
import json
import os
import sys

import psycopg
from dotenv import load_dotenv

# .env 로드 (ai-server 또는 프로젝트 루트)
load_dotenv(dotenv_path=os.path.join(os.path.dirname(__file__), "../../ai-server/.env"))

# DB 접속 정보
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = os.getenv("DB_PORT", "5432")
DB_NAME = os.getenv("DB_NAME", "pnn-db")
DB_USER = os.getenv("DB_USER", "postgres")
DB_PASS = os.getenv("DB_PASS", "1234")

# SUPP.AI JSON 경로 (data/supa-ai/)
SUPAI_DIR = os.path.join(os.path.dirname(__file__), "../supa-ai")
CUI_METADATA_PATH = os.path.join(SUPAI_DIR, "cui_metadata.json")
SENTENCE_DICT_PATH = os.path.join(SUPAI_DIR, "sentence_dict.json")

DESCRIPTION_MAX_LEN = 2000
DEFAULT_LEVEL = "CAUTION"


def load_suppai_data():
    """SUPP.AI JSON 로드"""
    if not os.path.exists(CUI_METADATA_PATH):
        raise FileNotFoundError(f"cui_metadata.json not found: {CUI_METADATA_PATH}")
    if not os.path.exists(SENTENCE_DICT_PATH):
        raise FileNotFoundError(f"sentence_dict.json not found: {SENTENCE_DICT_PATH}")

    with open(CUI_METADATA_PATH, "r", encoding="utf-8") as f:
        cui_metadata = json.load(f)
    with open(SENTENCE_DICT_PATH, "r", encoding="utf-8") as f:
        sentence_dict = json.load(f)

    return cui_metadata, sentence_dict


def extract_drug_supplement_pairs(cui_metadata, sentence_dict, evidence_min=1):
    """
    drug-supplement 쌍만 추출.
    Returns: list of (drug_ingredient, nutrient, description)
    """
    pairs = []
    for pair_id, ev_list in sentence_dict.items():
        if not ev_list or len(ev_list) < evidence_min:
            continue

        c1, c2 = pair_id.split("-")
        meta1 = cui_metadata.get(c1, {})
        meta2 = cui_metadata.get(c2, {})

        t1 = meta1.get("ent_type", "")
        t2 = meta2.get("ent_type", "")

        # drug-supplement 또는 supplement-drug
        if t1 == "drug" and t2 == "supplement":
            drug_name = meta1.get("preferred_name", "").strip()
            supp_name = meta2.get("preferred_name", "").strip()
        elif t1 == "supplement" and t2 == "drug":
            drug_name = meta2.get("preferred_name", "").strip()
            supp_name = meta1.get("preferred_name", "").strip()
        else:
            continue

        if not drug_name or not supp_name:
            continue

        # description: evidence 1~3문장 concat (최대 2000자)
        sentences = [e.get("sentence", "") for e in ev_list[:3] if e.get("sentence")]
        description = " ".join(sentences).strip()
        if len(description) > DESCRIPTION_MAX_LEN:
            description = description[: DESCRIPTION_MAX_LEN - 3] + "..."

        pairs.append((drug_name, supp_name, description))

    return pairs


def ensure_unique_constraint(conn):
    """interaction_rules에 (drug_ingredient, nutrient) UNIQUE 제약 추가"""
    with conn.cursor() as cur:
        cur.execute("""
            DO $$
            BEGIN
                IF NOT EXISTS (
                    SELECT 1 FROM pg_constraint
                    WHERE conrelid = 'interaction_rules'::regclass
                    AND conname = 'interaction_rules_drug_ingredient_nutrient_key'
                ) THEN
                    ALTER TABLE interaction_rules
                    ADD CONSTRAINT interaction_rules_drug_ingredient_nutrient_key
                    UNIQUE (drug_ingredient, nutrient);
                END IF;
            EXCEPTION WHEN duplicate_object THEN NULL;
            END $$;
        """)
        conn.commit()


def load_to_db(pairs, limit=None):
    """interaction_rules에 적재 (ON CONFLICT DO NOTHING)"""
    if limit:
        pairs = pairs[:limit]
        print(f"TEST mode: loading first {limit} pairs")

    conn_str = f"host={DB_HOST} port={DB_PORT} dbname={DB_NAME} user={DB_USER} password={DB_PASS}"

    with psycopg.connect(conn_str) as conn:
        ensure_unique_constraint(conn)

    insert_sql = """
        INSERT INTO interaction_rules (drug_ingredient, nutrient, level, description, action)
        VALUES (%s, %s, %s, %s, %s)
        ON CONFLICT (drug_ingredient, nutrient) DO NOTHING
    """

    inserted = 0
    with psycopg.connect(conn_str) as conn:
        with conn.cursor() as cur:
            for drug, nutrient, description in pairs:
                cur.execute(
                    insert_sql,
                    (drug, nutrient, DEFAULT_LEVEL, description or None, None),
                )
                if cur.rowcount > 0:
                    inserted += 1

            conn.commit()

    return inserted, len(pairs)


def main():
    parser = argparse.ArgumentParser(description="Load SUPP.AI data into interaction_rules")
    parser.add_argument("--limit", type=int, default=None, help="Limit number of pairs (test mode)")
    parser.add_argument(
        "--evidence-min",
        type=int,
        default=1,
        help="Minimum evidence count per interaction (default: 1)",
    )
    args = parser.parse_args()

    print("Loading SUPP.AI JSON...")
    cui_metadata, sentence_dict = load_suppai_data()
    print(f"  cui_metadata: {len(cui_metadata)} CUIs")
    print(f"  sentence_dict: {len(sentence_dict)} interactions")

    print(f"Extracting drug-supplement pairs (evidence_min={args.evidence_min})...")
    pairs = extract_drug_supplement_pairs(
        cui_metadata, sentence_dict, evidence_min=args.evidence_min
    )
    print(f"  Extracted {len(pairs)} drug-supplement pairs")

    if not pairs:
        print("No pairs to load. Exiting.")
        sys.exit(0)

    print("Loading to DB...")
    inserted, total = load_to_db(pairs, limit=args.limit)
    print(f"  Inserted: {inserted} / {total} (skipped duplicates)")


if __name__ == "__main__":
    main()
