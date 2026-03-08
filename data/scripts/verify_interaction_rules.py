"""Verify interaction_rules data"""
import os
import psycopg
from dotenv import load_dotenv

load_dotenv(dotenv_path=os.path.join(os.path.dirname(__file__), "../../ai-server/.env"))
conn_str = f"host={os.getenv('DB_HOST','localhost')} port={os.getenv('DB_PORT','5432')} dbname={os.getenv('DB_NAME','pnn-db')} user={os.getenv('DB_USER','postgres')} password={os.getenv('DB_PASS','1234')}"

with psycopg.connect(conn_str) as conn:
    with conn.cursor() as cur:
        cur.execute("SELECT COUNT(*) FROM interaction_rules")
        print("interaction_rules count:", cur.fetchone()[0])
        cur.execute("SELECT drug_ingredient, nutrient, level FROM interaction_rules WHERE drug_ingredient ILIKE '%warfarin%' LIMIT 3")
        print("Sample Warfarin:", cur.fetchall())
        cur.execute("SELECT drug_ingredient, nutrient FROM interaction_rules WHERE nutrient ILIKE '%ginkgo%' LIMIT 3")
        print("Sample Ginkgo:", cur.fetchall())
