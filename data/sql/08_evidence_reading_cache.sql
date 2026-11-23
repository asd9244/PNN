-- =============================================================================
-- 08. 설명서 근거 읽기 결과 저장소 (evidence_reading_cache)
-- 문장 1개 × 성분 1개에 대해 검증까지 끝난 AI 읽기 결과를 저장한다.
-- 같은 짝은 다음 검사부터 AI를 부르지 않고 이 값을 쓴다 (속도·일관성).
-- Spring(ddl-auto=update)이 켜질 때 자동으로 만들어지며, 이 파일은 기록·수동 생성용이다.
-- =============================================================================

CREATE TABLE IF NOT EXISTS evidence_reading_cache (
    id BIGSERIAL PRIMARY KEY,

    sentence_hash VARCHAR(64) NOT NULL,        -- 설명서 문장 내용의 지문 (SHA-256, 띄어쓰기 차이 무시)
    nutrient_group VARCHAR(50) NOT NULL,       -- 성분 묶음 이름 (예: 철분)
    cache_version VARCHAR(100) NOT NULL,       -- 결과 버전 (모델+프롬프트, 예: gemma4-e4b-prompt3)

    kind VARCHAR(20) NOT NULL,                 -- CAUTION / NO_EFFECT / OTHER / UNVERIFIED
    explanation TEXT,                          -- 쉬운 설명
    action TEXT,                               -- 할 일
    note TEXT,                                 -- 원문을 확인해 달라는 안내
    sentence TEXT NOT NULL,                    -- 사람이 확인할 수 있게 원문도 저장

    created_at TIMESTAMP NOT NULL,

    CONSTRAINT uk_evidence_reading_cache_key UNIQUE (sentence_hash, nutrient_group, cache_version)
);
