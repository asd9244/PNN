package com.pnn.backend.service;

import com.pnn.backend.domain.EvidenceReadingCache;
import com.pnn.backend.repository.EvidenceReadingCacheRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 설명서 근거 읽기 결과 저장소를 다루는 서비스.
 * <p>열쇠는 (문장 지문, 성분 묶음, 결과 버전). 결과 버전은 설정값 evidence.reading.cache-version으로,
 * 모델이나 프롬프트를 바꿀 때 이 값을 바꾸면 예전 저장분은 쓰지 않는다.</p>
 */
@Slf4j
@Service
public class EvidenceReadingCacheService {

    private final EvidenceReadingCacheRepository repository;
    private final String cacheVersion;

    public EvidenceReadingCacheService(EvidenceReadingCacheRepository repository,
                                       @Value("${evidence.reading.cache-version}") String cacheVersion) {
        this.repository = repository;
        this.cacheVersion = cacheVersion;
    }

    /**
     * 문장들에 대해 저장된 결과를 찾는다.
     * @return 키: "문장 원문|성분 묶음"
     */
    public Map<String, EvidenceReadingCache> find(Collection<String> sentences) {
        if (sentences.isEmpty()) {
            return Map.of();
        }
        Map<String, String> sentenceByHash = sentences.stream()
                .collect(Collectors.toMap(EvidenceReadingCacheService::hash, s -> s, (a, b) -> a));
        Map<String, EvidenceReadingCache> found = new HashMap<>();
        for (EvidenceReadingCache row : repository.findBySentenceHashInAndCacheVersion(sentenceByHash.keySet(), cacheVersion)) {
            found.put(sentenceByHash.get(row.getSentenceHash()) + "|" + row.getNutrientGroup(), row);
        }
        return found;
    }

    /** 검증까지 끝난 결과 하나를 저장한다. 동시에 같은 짝이 저장되면(중복 열쇠) 먼저 저장된 것을 둔다. */
    public void save(String sentence, String nutrientGroup, String kind, String explanation, String action, String note) {
        EvidenceReadingCache row = new EvidenceReadingCache();
        row.setSentenceHash(hash(sentence));
        row.setNutrientGroup(nutrientGroup);
        row.setCacheVersion(cacheVersion);
        row.setKind(kind);
        row.setExplanation(explanation);
        row.setAction(action);
        row.setNote(note);
        row.setSentence(sentence);
        row.setCreatedAt(LocalDateTime.now());
        try {
            repository.save(row);
        } catch (DataIntegrityViolationException e) {
            log.debug("이미 저장된 근거 읽기 결과 (성분: {}) — 건너뜀", nutrientGroup);
        }
    }

    /** 문장 내용의 지문. 띄어쓰기·줄바꿈 차이는 같은 문장으로 본다. */
    static String hash(String sentence) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(sentence.trim().replaceAll("\\s+", " ").getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 쓸 수 없습니다.", e); // 모든 Java 환경에 있는 알고리즘이라 실제로는 일어나지 않음
        }
    }
}
