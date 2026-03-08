-- drugs 테이블에 main_ingr_eng 컬럼 추가 (drug당 1건만 저장, 중복 제거)
-- 실행: psql -U postgres -d pnn-db -f add_main_ingr_eng_to_drugs.sql

-- 1) 컬럼 추가 (이미 있으면 무시)
ALTER TABLE drugs ADD COLUMN IF NOT EXISTS main_ingr_eng TEXT;

-- 2) 기존 drug_ingredients 데이터에서 drug당 1건만 가져와 이전
UPDATE drugs d SET main_ingr_eng = sub.main_ingr_eng
FROM (
    SELECT DISTINCT ON (drug_id) drug_id, main_ingr_eng
    FROM drug_ingredients
    WHERE main_ingr_eng IS NOT NULL AND TRIM(main_ingr_eng) != ''
) sub
WHERE d.id = sub.drug_id AND (d.main_ingr_eng IS NULL OR d.main_ingr_eng = '');
