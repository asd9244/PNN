-- =============================================================================
-- 05. 병용금기약물 (drug_contraindication)
-- 소스: 한국의약품안전관리원_병용금기약물_20240625.csv
-- 성분명1+성분명2 조합으로 매칭 (drug_ingredients 주성분과 JOIN)
-- =============================================================================

CREATE TABLE IF NOT EXISTS drug_contraindication (
    id BIGSERIAL PRIMARY KEY,

    -- 약 1
    ingr_name_1 VARCHAR(255) NOT NULL,         -- 성분명1 (영문, 매칭 키)
    ingr_code_1 VARCHAR(50),                    -- 성분코드1
    product_code_1 VARCHAR(50),                -- 제품코드1
    product_name_1 TEXT,                       -- 제품명1
    entp_name_1 VARCHAR(255),                 -- 업체명1
    pay_type_1 VARCHAR(50),                    -- 급여구분1

    -- 약 2
    ingr_name_2 VARCHAR(255) NOT NULL,         -- 성분명2 (영문, 매칭 키)
    ingr_code_2 VARCHAR(50),                    -- 성분코드2
    product_code_2 VARCHAR(50),                -- 제품코드2
    product_name_2 TEXT,                       -- 제품명2
    entp_name_2 VARCHAR(255),                  -- 업체명2
    pay_type_2 VARCHAR(50),                     -- 급여구분2

    -- 금기 정보
    notice_no VARCHAR(50),                     -- 공고번호
    notice_date VARCHAR(20),                   -- 공고일자
    contraind_reason TEXT NOT NULL             -- 금기사유
);

CREATE INDEX IF NOT EXISTS idx_drug_contraindication_ingr1 ON drug_contraindication(ingr_name_1);
CREATE INDEX IF NOT EXISTS idx_drug_contraindication_ingr2 ON drug_contraindication(ingr_name_2);
CREATE INDEX IF NOT EXISTS idx_drug_contraindication_ingr_pair ON drug_contraindication(ingr_name_1, ingr_name_2);
