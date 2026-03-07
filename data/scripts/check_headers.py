import pandas as pd
import os

# CSV 파일 경로
PRICE_FILE = "../건강보험심사평가원_약가마스터_의약품표준코드_20251031.csv"
CONTRA_FILE = "../한국의약품안전관리원_병용금기약물_20240625.csv"

def check_header(file_path):
    print(f"\nChecking: {file_path}")
    if not os.path.exists(file_path):
        print("File not found!")
        return

    try:
        # EUC-KR (cp949)로 시도
        df = pd.read_csv(file_path, encoding='cp949', nrows=0)
        print("Encoding: cp949")
        print("Columns:", df.columns.tolist())
    except UnicodeDecodeError:
        try:
            # UTF-8로 시도
            df = pd.read_csv(file_path, encoding='utf-8', nrows=0)
            print("Encoding: utf-8")
            print("Columns:", df.columns.tolist())
        except Exception as e:
            print(f"Error reading file: {e}")

if __name__ == "__main__":
    check_header(PRICE_FILE)
    check_header(CONTRA_FILE)
