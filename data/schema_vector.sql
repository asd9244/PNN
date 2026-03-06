-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Create knowledge_embeddings table
CREATE TABLE IF NOT EXISTS knowledge_embeddings (
    id BIGSERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    embedding vector(1536), -- text-embedding-3-small (1536 dim)
    source VARCHAR(255),
    category VARCHAR(50),
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Create HNSW index for cosine similarity search
CREATE INDEX IF NOT EXISTS idx_knowledge_embeddings_embedding ON knowledge_embeddings USING hnsw (embedding vector_cosine_ops);

-- Create GIN index for metadata JSONB search
CREATE INDEX IF NOT EXISTS idx_knowledge_embeddings_metadata ON knowledge_embeddings USING gin (metadata);
