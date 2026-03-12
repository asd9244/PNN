-- =============================================================================
-- 03. 의약품 제품허가 상세정보 (drug_permit_detail)
-- 소스: 의약품 제품허가 상세정보.csv (약 5.5만 건)
-- 품목일련번호(item_seq) = drugs_master.item_seq 와 JOIN 가능 (낱알식별에 없는 품목도 존재)
-- =============================================================================

CREATE TABLE IF NOT EXISTS drug_permit_detail (
    id BIGSERIAL PRIMARY KEY,

    -- 기본 정보
    item_seq VARCHAR(50) NOT NULL,            -- 품목일련번호 (품목기준코드)
    item_name TEXT NOT NULL,                  -- 품목명
    item_eng_name TEXT,                       -- 품목 영문명
    entp_name TEXT,                           -- 업체명
    entp_eng_name TEXT,                       -- 업체 영문명
    bizrno VARCHAR(50),                       -- 사업자번호
    permit_no VARCHAR(50),                    -- 업체허가번호

    -- 허가/규제
    permit_type VARCHAR(50),                  -- 허가/신고구분
    cancel_status VARCHAR(50),                -- 취소상태
    cancel_date VARCHAR(20),                  -- 취소일자
    change_date VARCHAR(20),                  -- 변경일자
    permit_date VARCHAR(20),                  -- 허가일자
    etc_otc_code VARCHAR(50),                -- 전문일반
    narcotic_class TEXT,                     -- 마약류분류
    finished_raw_type TEXT,                  -- 완제원료구분
    new_drug_yn VARCHAR(10),                 -- 신약여부
    biz_type TEXT,                            -- 업종구분
    rare_drug_yn VARCHAR(10),                 -- 희귀의약품여부
    consign_entp TEXT,                        -- 위탁제조업체

    -- 성분 및 효능 (핵심)
    raw_ingredients TEXT,                     -- 원료성분 (파이프 구분 구조)
    ingr_name_eng TEXT,                       -- 영문성분명
    main_ingr_name TEXT,                      -- 주성분명 (파이프 구분)
    additive_name TEXT,                       -- 첨가제명
    atc_code VARCHAR(50),                     -- ATC코드
    total_qty TEXT,                           -- 총량

    -- 상세 설명 (긴 텍스트)
    efficacy TEXT,                           -- 효능효과
    dosage TEXT,                             -- 용법용량
    caution TEXT,                            -- 주의사항
    change_history TEXT,                     -- 변경내용

    -- 물리적 특성
    appearance TEXT,                         -- 성상
    storage_method TEXT,                     -- 저장방법
    validity_period TEXT,                    -- 유효기간
    package_unit TEXT,                       -- 포장단위

    -- 첨부/문서 (URL)
    attach_doc TEXT,                         -- 첨부문서 (EE, UD, NB URL 구분자 포함 가능)

    -- 재심사
    reexam_target TEXT,                       -- 재심사대상
    reexam_period TEXT,                       -- 재심사기간

    -- 코드
    std_code TEXT,                           -- 표준코드 (쉼표 구분)
    insur_code TEXT                          -- 보험코드 (쉼표 구분)
);

CREATE INDEX IF NOT EXISTS idx_drug_permit_detail_item_seq ON drug_permit_detail(item_seq);
CREATE INDEX IF NOT EXISTS idx_drug_permit_detail_atc ON drug_permit_detail(atc_code);
