import os, psycopg
from dotenv import load_dotenv
load_dotenv(dotenv_path=os.path.join(os.path.dirname(__file__), "../../ai-server/.env"))
c = f"host={os.getenv('DB_HOST','localhost')} port={os.getenv('DB_PORT','5432')} dbname={os.getenv('DB_NAME','pnn-db')} user={os.getenv('DB_USER','postgres')} password={os.getenv('DB_PASS','1234')}"
with psycopg.connect(c) as conn:
    with conn.cursor() as cur:
        cur.execute("""
            SELECT dm.id, dm.item_seq, dm.item_name
            FROM drugs_master dm
            WHERE EXISTS (SELECT 1 FROM drug_ingredients di WHERE di.item_seq = dm.item_seq)
            ORDER BY dm.id LIMIT 5
        """)
        for r in cur.fetchall():
            print(r)
