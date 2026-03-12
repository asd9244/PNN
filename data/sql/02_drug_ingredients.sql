-- =============================================================================
-- 02. 의약품 주성분 (drug_ingredients)
-- 소스: 의약품 제품 허가정보_의약품 상세조회(주성분).csv (약 12.9만 건)
-- 품목기준코드(item_seq) = drugs_master.item_seq 와 1:N 관계
-- =============================================================================

CREATE TABLE IF NOT EXISTS drug_ingredients (
    id BIGSERIAL PRIMARY KEY,

    item_seq VARCHAR(50) NOT NULL,           -- 품목기준코드 (drugs_master.item_seq FK)
    entp_name TEXT,                          -- 업소명
    item_name TEXT,                          -- 품목명
    ingr_name_kr TEXT,                       -- 성분명한글
    ingr_name_eng TEXT,                      -- 성분영문명 (상호작용 매칭 핵심)
    ingr_code VARCHAR(50),                   -- 성분코드
    unit VARCHAR(50),                        -- 단위
    raw_qty TEXT,                            -- 원료분량 (구조화된 파이프 구분 등)
    seq_no VARCHAR(50),                     -- 순번
    total_seq VARCHAR(50),                   -- 총량순번
    bizrno VARCHAR(50),                      -- 사업자번호
    permit_no VARCHAR(50),                   -- 업허가번호

    CONSTRAINT fk_drug_ingredients_item_seq
        FOREIGN KEY (item_seq) REFERENCES drugs_master(item_seq)
);

CREATE INDEX IF NOT EXISTS idx_drug_ingredients_item_seq ON drug_ingredients(item_seq);
CREATE INDEX IF NOT EXISTS idx_drug_ingredients_ingr_eng ON drug_ingredients(ingr_name_eng);
