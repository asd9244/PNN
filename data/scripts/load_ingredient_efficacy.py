"""
심평원 의약품성분약효정보 API → ingredient_efficacy 테이블 적재
API: https://apis.data.go.kr/B551182/msupCmpnMeftInfoService/getMajorCmpnNmCdList
응답: XML
전략: meftDivNo로 100개씩 청크 수신 + drug_price_master.ingr_code에 있는 코드만 적재.
"""
import os
import sys
import time
import xml.etree.ElementTree as ET
import requests
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

# API 설정
API_BASE = "https://apis.data.go.kr/B551182/msupCmpnMeftInfoService/getMajorCmpnNmCdList"
# 공공데이터 Decoding Key (application.properties의 api.public.drug.key와 동일)
API_KEY = os.getenv("API_PUBLIC_DRUG_KEY", "d155020ceba987a316ca5b9ea39eb6f4d7bf71357b449f6c1e57b6f2e4e9dc92")

# meftDivNo 범위: 3자리 (001~999). 실제 존재하는 번호만 응답함.
MEFTDIV_START = 1
MEFTDIV_END = 999
CHUNK_SIZE = 100

# 스키마 컬럼 길이 (VARCHAR 초과 방지)
COL_LIMITS = {"gnl_nm_cd": 20, "gnl_nm": 255, "meft_div_no": 10, "div_nm": 255, "fomn_tp_nm": 100, "injc_pth_nm": 100, "iqty_txt": 500, "unit": 50}


def fetch_by_meft_div_no(meft_div_no: int, page_no: int = 1) -> tuple[list[dict], int]:
    """
    meftDivNo로 API 조회. 한 번에 최대 CHUNK_SIZE건 수신.
    Returns: (rows, total_count). total_count=0이면 해당 meftDivNo에 데이터 없음.
    """
    params = {
        "serviceKey": API_KEY,
        "numOfRows": CHUNK_SIZE,
        "pageNo": page_no,
        "meftDivNo": str(meft_div_no).zfill(3),
    }
    try:
        resp = requests.get(API_BASE, params=params, timeout=30)
        resp.raise_for_status()
        root = ET.fromstring(resp.content)
        body = root.find("body")
        if body is None:
            return [], 0

        total_elem = body.find("totalCount")
        total_count = int(total_elem.text) if total_elem is not None and total_elem.text else 0

        items = body.find("items")
        if items is None:
            return [], total_count

        rows = []
        for item in items.findall("item"):
            row = {
                "gnl_nm_cd": _text(item.find("gnlNmCd")),
                "gnl_nm": _text(item.find("gnlNm")),
                "meft_div_no": _text(item.find("meftDivNo")),
                "div_nm": _text(item.find("divNm")),
                "fomn_tp_nm": _text(item.find("fomnTpCdNm")) or _text(item.find("fomnTpNm")),
                "injc_pth_nm": _text(item.find("injcPthCdNm")) or _text(item.find("injcPthNm")),
                "iqty_txt": _text(item.find("iqtyTxt")),
                "unit": _text(item.find("unit")),
            }
            if row["gnl_nm_cd"] and row["gnl_nm"]:
                rows.append(row)
        return rows, total_count
    except Exception as e:
        print(f"API Error (meftDivNo={meft_div_no}, pageNo={page_no}): {e}")
        return [], 0


def _text(elem) -> str | None:
    """Element에서 텍스트 추출, 없으면 None"""
    if elem is not None and elem.text:
        return elem.text.strip() or None
    return None


def _truncate(row: dict) -> dict:
    """스키마 VARCHAR 길이 초과 방지"""
    out = {}
    for k, v in row.items():
        if v is None:
            out[k] = None
        elif k in COL_LIMITS:
            out[k] = v[: COL_LIMITS[k]] if len(v) > COL_LIMITS[k] else v
        else:
            out[k] = v
    return out


def load_data(meft_div_limit: int | None = None):
    """
    meftDivNo로 100개씩 청크 수신 + drug_price_master.ingr_code에 있는 코드만 적재.
    meft_div_limit: 처리할 meftDivNo 개수 제한 (테스트용). None이면 001~999 전체.
    """
    print("Starting ingredient_efficacy load (meftDivNo 청크, 필요한 코드만 적재)...")

    conn_str = f"host={DB_HOST} port={DB_PORT} dbname={DB_NAME} user={DB_USER} password={DB_PASS}"
    insert_sql = """
        INSERT INTO ingredient_efficacy (gnl_nm_cd, gnl_nm, meft_div_no, div_nm, fomn_tp_nm, injc_pth_nm, iqty_txt, unit)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
        ON CONFLICT (gnl_nm_cd) DO UPDATE SET
            gnl_nm = EXCLUDED.gnl_nm,
            meft_div_no = EXCLUDED.meft_div_no,
            div_nm = EXCLUDED.div_nm,
            fomn_tp_nm = EXCLUDED.fomn_tp_nm,
            injc_pth_nm = EXCLUDED.injc_pth_nm,
            iqty_txt = EXCLUDED.iqty_txt,
            unit = EXCLUDED.unit
    """

    try:
        with psycopg.connect(conn_str) as conn:
            with conn.cursor() as cur:
                # drug_price_master에서 필요한 ingr_code만 대상
                cur.execute(
                    "SELECT DISTINCT ingr_code FROM drug_price_master WHERE ingr_code IS NOT NULL AND ingr_code != ''"
                )
                remaining_codes = {r[0] for r in cur.fetchall()}
                print(f"Target ingr_code count: {len(remaining_codes)}")

                total_inserted = 0
                api_calls = 0
                meft_range = range(MEFTDIV_START, MEFTDIV_END + 1)
                if meft_div_limit:
                    meft_range = list(meft_range)[:meft_div_limit]
                    print(f"Test mode: processing meftDivNo 001~{str(meft_div_limit).zfill(3)}")

                for meft_div_no in meft_range:
                    if not remaining_codes:
                        print("All target codes collected. Stopping early.")
                        break
                    page_no = 1
                    while True:
                        rows, total_count = fetch_by_meft_div_no(meft_div_no, page_no)
                        api_calls += 1
                        time.sleep(0.1)  # API 부하 방지

                        if page_no == 1 and total_count == 0:
                            break  # 해당 meftDivNo에 데이터 없음

                        for row in rows:
                            if row["gnl_nm_cd"] not in remaining_codes:
                                continue  # 필요한 코드만 적재
                            safe = _truncate(row)
                            cur.execute(
                                insert_sql,
                                (
                                    safe["gnl_nm_cd"],
                                    safe["gnl_nm"],
                                    safe["meft_div_no"],
                                    safe["div_nm"],
                                    safe["fomn_tp_nm"],
                                    safe["injc_pth_nm"],
                                    safe["iqty_txt"],
                                    safe["unit"],
                                ),
                            )
                            total_inserted += 1
                            remaining_codes.discard(row["gnl_nm_cd"])

                        if len(rows) < CHUNK_SIZE or page_no * CHUNK_SIZE >= total_count:
                            break
                        page_no += 1

                    if meft_div_no % 50 == 0:
                        conn.commit()
                        print(f"Progress: meftDivNo {meft_div_no:03d}, {total_inserted} rows, {api_calls} API calls")

                conn.commit()
                print(f"Done. Total inserted/updated: {total_inserted}, API calls: {api_calls}")

    except Exception as e:
        print(f"DB Error: {e}")
        raise


if __name__ == "__main__":
    # 테스트: python load_ingredient_efficacy.py 5  |  전체: python load_ingredient_efficacy.py
    meft_limit = int(sys.argv[1]) if len(sys.argv) > 1 else None
    load_data(meft_div_limit=meft_limit)
