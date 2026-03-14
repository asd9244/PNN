-- =============================================================================
-- 07. 건강보험심사평가원 약가마스터 (drug_price_master)
-- 소스: 건강보험심사평가원_약가마스터_의약품표준코드_20251031.csv (약 30만 건)
-- =============================================================================

CREATE TABLE IF NOT EXISTS drug_price_master (
    id BIGSERIAL PRIMARY KEY,
    item_seq VARCHAR(50) NOT NULL,            -- 품목기준코드 (drugs_master.item_seq와 JOIN)
    item_name TEXT NOT NULL,                  -- 한글상품명
    entp_name VARCHAR(255),                   -- 업체명
    pkg_spec TEXT,                            -- 약품규격
    formulation VARCHAR(100),                 -- 제형구분
    pkg_form VARCHAR(100),                    -- 포장형태
    rep_code VARCHAR(50),                     -- 대표코드
    standard_code VARCHAR(50),                -- 표준코드 (13자리)
    insur_code VARCHAR(50),                   -- 제품코드(개정후) = 보험코드
    main_ingr_code VARCHAR(50),               -- 일반명코드(성분명코드) = 주성분코드
    atc_code VARCHAR(50),                     -- 국제표준코드(ATC코드)
    cancel_date VARCHAR(20),                  -- 취소일자
    remark TEXT                               -- 비고
);

CREATE INDEX IF NOT EXISTS idx_drug_price_master_item_seq ON drug_price_master(item_seq);
CREATE INDEX IF NOT EXISTS idx_drug_price_master_insur_code ON drug_price_master(insur_code);
CREATE INDEX IF NOT EXISTS idx_drug_price_master_main_ingr_code ON drug_price_master(main_ingr_code);