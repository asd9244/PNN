-- ingredient_mapping 테이블 삭제 (Case A/B 흐름에서 미사용)
-- 실행: psql -U postgres -d pnn-db -f drop_ingredient_mapping.sql

DROP TABLE IF EXISTS ingredient_mapping;
