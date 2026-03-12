"""
drug_permit_detail의 efficacy, dosage, caution 컬럼에 있는 PDF URL에서
텍스트를 추출하여 해당 컬럼을 업데이트하는 스크립트.

- DB에서 PDF URL이 있는 행을 읽어옴
- 각 URL에서 PDF 다운로드 → 메모리에서 텍스트 추출
- 추출한 텍스트로 efficacy, dosage, caution 업데이트
- 기본값: 5건만 처리 (--limit 옵션으로 변경 가능)
"""
import os
import argparse
import io
import requests
from pypdf import PdfReader
from dotenv import load_dotenv
import psycopg

# --- 설정 ---
load_dotenv(dotenv_path=os.path.join(os.path.dirname(__file__), "../../ai-server/.env"))

DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = os.getenv("DB_PORT", "5432")
DB_NAME = os.getenv("DB_NAME", "pnn-db")
DB_USER = os.getenv("DB_USER", "postgres")
DB_PASS = os.getenv("DB_PASS", "1234")


def is_pdf_url(val: str | None) -> bool:
    """값이 PDF 다운로드 URL인지 확인"""
    if not val or not isinstance(val, str):
        return False
    s = val.strip()
    return s.startswith("http") and ("pdf" in s.lower() or s.endswith("/EE") or s.endswith("/UD") or s.endswith("/NB"))


def extract_text_from_pdf_url(url: str) -> str | None:
    """URL에서 PDF를 다운로드하여 텍스트 추출 (메모리 상에서 처리)"""
    try:
        resp = requests.get(url, timeout=30)
        resp.raise_for_status()
        pdf_bytes = resp.content
        reader = PdfReader(io.BytesIO(pdf_bytes))
        texts = []
        for page in reader.pages:
            t = page.extract_text()
            if t:
                texts.append(t)
        raw = "\n".join(texts).strip() if texts else None
        # PostgreSQL text 필드는 NUL(0x00) 바이트 불가
        return raw.replace("\x00", "").strip() if raw else None
    except Exception as e:
        print(f"  [WARN] PDF 추출 실패: {url} - {e}")
        return None


def main():
    parser = argparse.ArgumentParser(description="PDF URL → 텍스트 추출 후 drug_permit_detail 업데이트")
    parser.add_argument("--limit", type=int, default=5, help="처리할 행 수 (기본 5)")
    parser.add_argument("--dry-run", action="store_true", help="실제 UPDATE 없이 추출만 수행")
    args = parser.parse_args()

    conn_str = f"host={DB_HOST} port={DB_PORT} dbname={DB_NAME} user={DB_USER} password={DB_PASS}"
    with psycopg.connect(conn_str) as conn:
        with conn.cursor() as cur:
            cur.execute("""
                SELECT id, item_seq, item_name, efficacy, dosage, caution
                FROM drug_permit_detail
                WHERE efficacy LIKE 'http%%' OR dosage LIKE 'http%%' OR caution LIKE 'http%%'
                ORDER BY id
                LIMIT %s
            """, (args.limit,))
            rows = cur.fetchall()

    if not rows:
        print("PDF URL이 있는 행이 없습니다.")
        return

    print(f"처리 대상: {len(rows)}건")
    for r in rows:
        id_, item_seq, item_name, eff_url, dos_url, cau_url = r
        print(f"\n[{id_}] {item_seq} - {item_name[:40]}...")

        eff_text = extract_text_from_pdf_url(eff_url) if is_pdf_url(eff_url) else eff_url
        dos_text = extract_text_from_pdf_url(dos_url) if is_pdf_url(dos_url) else dos_url
        cau_text = extract_text_from_pdf_url(cau_url) if is_pdf_url(cau_url) else cau_url

        if args.dry_run:
            print(f"  efficacy: {len(eff_text or '')} chars")
            print(f"  dosage:   {len(dos_text or '')} chars")
            print(f"  caution:  {len(cau_text or '')} chars")
            continue

        with psycopg.connect(conn_str) as conn:
            with conn.cursor() as cur:
                cur.execute("""
                    UPDATE drug_permit_detail
                    SET efficacy = %s, dosage = %s, caution = %s
                    WHERE id = %s
                """, (eff_text, dos_text, cau_text, id_))
            conn.commit()
        print("  → 업데이트 완료")

    print("\n완료.")


if __name__ == "__main__":
    main()
