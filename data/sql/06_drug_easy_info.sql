-- =============================================================================
-- 06. e약은요정보 (drug_easy_info)
-- 소스: e약은요정보.csv (상비약 중심, 품목일련번호 = item_seq)
-- drugs_master, drug_permit_detail과 item_seq로 JOIN
-- =============================================================================

CREATE TABLE IF NOT EXISTS drug_easy_info (
    id BIGSERIAL PRIMARY KEY,

    item_seq VARCHAR(50) NOT NULL UNIQUE,       -- 품목일련번호 (drugs_master.item_seq)
    product_name TEXT NOT NULL,                -- 제품명
    entp_name VARCHAR(255),                     -- 업체명
    bizrno VARCHAR(50),                        -- 사업자번호

    -- 복약정보 (이미 정제된 텍스트)
    efficacy TEXT,                             -- 이 약의 효능은 무엇입니까?
    dosage TEXT,                               -- 이 약은 어떻게 사용합니까?
    before_use TEXT,                           -- 이 약을 사용하기 전에 반드시 알아야 할 내용은?
    caution_use TEXT,                          -- 이 약의 사용상 주의사항은?
    interaction_drug_food TEXT,                -- 주의해야 할 약 또는 음식
    adverse_reaction TEXT,                      -- 이상반응
    storage TEXT,                              -- 보관방법

    -- 메타
    publish_date VARCHAR(20),                  -- 공개일자
    modify_date VARCHAR(20),                    -- 수정일자
    pill_image_url VARCHAR(1000)               -- 낱알이미지
);

CREATE INDEX IF NOT EXISTS idx_drug_easy_info_item_seq ON drug_easy_info(item_seq);
