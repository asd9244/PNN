-- =============================================================================
-- 01. 의약품 낱알식별 (drugs_master)
-- 소스: 의약품 낱알식별.csv (약 2.7만 건)
-- 품목일련번호(item_seq) = 식약처 품목기준코드, 다른 테이블과 JOIN 핵심 키
-- =============================================================================

CREATE TABLE IF NOT EXISTS drugs_master (
    id BIGSERIAL PRIMARY KEY,

    -- 기본 정보
    item_seq VARCHAR(50) NOT NULL UNIQUE,     -- 품목일련번호 (품목기준코드)
    item_name VARCHAR(500) NOT NULL,          -- 품목명
    entp_seq VARCHAR(50),                     -- 업소일련번호
    entp_name VARCHAR(255),                  -- 업소명
    item_eng_name VARCHAR(255),              -- 품목영문명

    -- 분류 및 규제 (핵심)
    class_no VARCHAR(50),                     -- 분류번호
    class_name VARCHAR(255),                 -- 분류명 (예: 기타의 소화기관용약, 혈압강하제)
    etc_otc_code VARCHAR(50),                -- 전문일반구분
    permit_date VARCHAR(20),                 -- 품목허가일자
    change_date VARCHAR(20),                 -- 변경일자
    bizrno VARCHAR(50),                      -- 사업자번호
    insur_code VARCHAR(50),                 -- 보험코드
    std_code TEXT,                           -- 표준코드 (쉼표 구분 바코드 목록)

    -- 외형 및 식별 (낱알 검색용)
    appearance TEXT,                         -- 성상
    drug_shape VARCHAR(100),                 -- 의약품제형 (원형, 장방형 등)
    color_front VARCHAR(100),                -- 색상앞
    color_back VARCHAR(100),                 -- 색상뒤
    print_front VARCHAR(255),                -- 표시앞 (각인)
    print_back VARCHAR(255),                 -- 표시뒤 (각인)
    line_front VARCHAR(50),                  -- 분할선앞
    line_back VARCHAR(50),                   -- 분할선뒤
    form_code_name VARCHAR(255),             -- 제형코드명 (필름코팅정 등)

    -- 크기
    length_long VARCHAR(50),                 -- 크기장축
    length_short VARCHAR(50),                -- 크기단축
    thickness VARCHAR(50),                   -- 크기두께

    -- 이미지 및 표기
    item_image_url VARCHAR(1000),            -- 큰제품이미지
    img_regist_date VARCHAR(20),             -- 이미지생성일자(약학정보원)
    mark_text_front VARCHAR(255),            -- 표기내용앞
    mark_text_back VARCHAR(255),             -- 표기내용뒤
    mark_img_front VARCHAR(1000),            -- 표기이미지앞
    mark_img_back VARCHAR(1000),             -- 표기이미지뒤
    mark_code_front VARCHAR(255),            -- 표기코드앞
    mark_code_back VARCHAR(255)              -- 표기코드뒤
);

CREATE INDEX IF NOT EXISTS idx_drugs_master_class_no ON drugs_master(class_no);
CREATE INDEX IF NOT EXISTS idx_drugs_master_class_name ON drugs_master(class_name);
