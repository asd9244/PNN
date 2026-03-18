-- =============================================================================
-- DUR 통합 테이블 마이그레이션
-- 기존 dur_rules 삭제 후 새 스키마로 재생성
-- 6개 CSV: 노인주의, 용량주의, 임산부금기, 투여기간주의, 특정연령대금기, 효능군중복주의
-- =============================================================================

-- 기존 테이블 삭제
DROP TABLE IF EXISTS dur_rules;

-- 새 통합 테이블 생성
CREATE TABLE dur_rules (
    id BIGSERIAL PRIMARY KEY,
    dur_type VARCHAR(50) NOT NULL,
    product_code TEXT,
    ingr_code TEXT,
    ingr_name TEXT,
    warning_text TEXT,
    raw_data JSONB
);

CREATE INDEX idx_dur_rules_product_code ON dur_rules(product_code);
CREATE INDEX idx_dur_rules_dur_type ON dur_rules(dur_type);
CREATE INDEX idx_dur_rules_ingr_name ON dur_rules(ingr_name);
