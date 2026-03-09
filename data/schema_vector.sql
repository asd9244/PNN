-- knowledge_embeddings 테이블 제거 (2026-03)
-- drugs + interaction_rules 중심 구조로 전환. RAG/knowledge_embeddings 미사용.
-- 기존 DB 정리 시 실행: psql -U postgres -d pnn-db -f schema_vector.sql

DROP TABLE IF EXISTS knowledge_embeddings CASCADE;
