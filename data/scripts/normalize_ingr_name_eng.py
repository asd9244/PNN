"""
drug_ingredients: ingr_name_eng에 /로 구분된 복합 성분이 있으면,
해당 행의 ingr_name_kr에 대응하는 영문 성분만 남김 (seq_no로 순서 매칭)
"""
import os
import psycopg
from dotenv import load_dotenv

load_dotenv(dotenv_path=os.path.join(os.path.dirname(__file__), "../../ai-server/.env"))

conn_str = f"host={os.getenv('DB_HOST','localhost')} port={os.getenv('DB_PORT','5432')} dbname={os.getenv('DB_NAME','pnn-db')} user={os.getenv('DB_USER','postgres')} password={os.getenv('DB_PASS','1234')}"


def main():
    with psycopg.connect(conn_str) as conn:
        with conn.cursor() as cur:
            # 1. 처리 대상 건수
            cur.execute("""
                SELECT COUNT(*) FROM drug_ingredients
                WHERE ingr_name_eng LIKE '%/%'
                  AND seq_no IS NOT NULL AND seq_no != ''
                  AND seq_no ~ '^[0-9]+$'
                  AND seq_no::int >= 1
                  AND seq_no::int <= array_length(string_to_array(ingr_name_eng, '/'), 1)
            """)
            target_count = cur.fetchone()[0]
            print(f"처리 대상: {target_count:,}건")

            # 2. seq_no 유효하지 않아 건너뛸 건수
            cur.execute("""
                SELECT COUNT(*) FROM drug_ingredients
                WHERE ingr_name_eng LIKE '%/%'
                  AND (
                    seq_no IS NULL OR seq_no = '' OR
                    seq_no !~ '^[0-9]+$' OR
                    seq_no::int < 1 OR
                    seq_no::int > array_length(string_to_array(ingr_name_eng, '/'), 1)
                  )
            """)
            skip_count = cur.fetchone()[0]
            print(f"건너뜀 (seq_no 무효): {skip_count:,}건")

            # 3. UPDATE 실행
            cur.execute("""
                UPDATE drug_ingredients
                SET ingr_name_eng = TRIM((string_to_array(ingr_name_eng, '/'))[seq_no::int]::text)
                WHERE ingr_name_eng LIKE '%/%'
                  AND seq_no IS NOT NULL AND seq_no != ''
                  AND seq_no ~ '^[0-9]+$'
                  AND seq_no::int >= 1
                  AND seq_no::int <= array_length(string_to_array(ingr_name_eng, '/'), 1)
            """)
            updated = cur.rowcount
            conn.commit()
            print(f"UPDATE 완료: {updated:,}건")

            # 4. 검증: item_seq=202400655 (장이더락캡슐) 확인
            cur.execute("""
                SELECT id, item_seq, ingr_name_kr, ingr_name_eng, seq_no
                FROM drug_ingredients
                WHERE item_seq = '202400655'
                ORDER BY seq_no
            """)
            print("\n[검증] item_seq=202400655 (장이더락캡슐):")
            for r in cur.fetchall():
                print(f"  id={r[0]} seq_no={r[4]} ingr_name_kr={r[2]} | ingr_name_eng={r[3]}")


if __name__ == "__main__":
    main()
