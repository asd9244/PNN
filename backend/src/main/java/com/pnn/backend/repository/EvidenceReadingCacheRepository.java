package com.pnn.backend.repository;

import com.pnn.backend.domain.EvidenceReadingCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 데이터 접근 계층 (Repository)
 * evidence_reading_cache 테이블 (설명서 근거 읽기 결과 저장소) 조회·저장 용도
 */
@Repository
public interface EvidenceReadingCacheRepository extends JpaRepository<EvidenceReadingCache, Long> {

    // 약 하나의 근거 문장들에 대해, 지금 버전으로 저장된 결과를 한 번에 가져옵니다.
    List<EvidenceReadingCache> findBySentenceHashInAndCacheVersion(Collection<String> sentenceHashes, String cacheVersion);
}
