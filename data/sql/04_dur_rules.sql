-- =============================================================================
-- 04. DUR 유형별 성분 현황 (dur_rules)
-- 소스: DUR유형별 성분 현황_*.csv 8종 (병용금기, 투여기간주의, 용량주의, 임산부금기 등)
-- 성분코드/성분명 기준 매칭 (drug_ingredients, drug_permit_detail과 JOIN)
-- =============================================================================

CREATE TABLE IF NOT EXISTS dur_rules (
    id BIGSERIAL PRIMARY KEY,

    -- DUR 기본
    dur_seq VARCHAR(50) NOT NULL,              -- DUR일련번호
    dur_type VARCHAR(50) NOT NULL,             -- DUR유형 (병용금기, 투여기간주의, 용량주의 등)
    single_complex_code VARCHAR(20),           -- 단일복합구분코드
    dur_ingr_code VARCHAR(50),                -- DUR성분코드
    dur_ingr_name_eng VARCHAR(255),            -- DUR성분명영문 (매칭 키)
    dur_ingr_name VARCHAR(255),               -- DUR성분명
    complex_drug TEXT,                         -- 복합제
    related_ingr TEXT,                         -- 관계성분
    efficacy_class_code TEXT,                  -- 약효분류코드
    efficacy_group TEXT,                       -- 효능군
    notice_date VARCHAR(20),                   -- 고시일자
    contraind_content TEXT,                    -- 금기내용
    dosage_form TEXT,                          -- 제형
    age_criteria TEXT,                         -- 연령기준
    max_duration TEXT,                         -- 최대투여기간
    max_daily_dose TEXT,                       -- 1일최대용량
    grade TEXT,                                -- 등급
    note TEXT,                                 -- 비고
    status VARCHAR(50),                        -- 상태
    series_name TEXT,                          -- 계열명

    -- 병용금기 전용 (DUR유형=병용금기일 때만 값 있음)
    contraind_single_complex_code VARCHAR(20), -- 병용금기단일복합구분코드
    contraind_dur_ingr_code VARCHAR(50),       -- 병용금기DUR성분코드
    contraind_dur_ingr_name_eng VARCHAR(255),  -- 병용금기DUR성분영문명 (매칭 키)
    contraind_dur_ingr_name VARCHAR(255),      -- 병용금기DUR성분명
    contraind_complex_drug TEXT,               -- 병용금기복합제
    contraind_related_ingr TEXT,              -- 병용금기관계성분
    contraind_efficacy_class TEXT             -- 병용금기약효분류
);

CREATE INDEX IF NOT EXISTS idx_dur_rules_dur_type ON dur_rules(dur_type);
CREATE INDEX IF NOT EXISTS idx_dur_rules_ingr_eng ON dur_rules(dur_ingr_name_eng);
CREATE INDEX IF NOT EXISTS idx_dur_rules_contraind_ingr_eng ON dur_rules(contraind_dur_ingr_name_eng);
