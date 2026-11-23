package com.pnn.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 설명서 근거 읽기 결과 저장소 (evidence_reading_cache)
 * <p>문장 1개 × 성분 1개에 대해 검증까지 끝난 AI 읽기 결과를 저장한다. 같은 짝은 다음부터 AI를 부르지 않고 이 값을 쓴다.</p>
 * <p>열쇠: 문장 내용 지문 + 성분 묶음 + 결과 버전. 설명서 문장이 바뀌면 지문이 달라져 자동으로 새로 읽는다.</p>
 */
@Entity
@Table(name = "evidence_reading_cache",
        uniqueConstraints = @UniqueConstraint(name = "uk_evidence_reading_cache_key",
                columnNames = {"sentence_hash", "nutrient_group", "cache_version"}))
@Getter
@Setter
public class EvidenceReadingCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sentence_hash", nullable = false, length = 64)
    private String sentenceHash; // 설명서 문장 내용의 지문 (SHA-256, 16진수 64자)

    @Column(name = "nutrient_group", nullable = false, length = 50)
    private String nutrientGroup; // 성분 묶음 이름 (예: 철분)

    @Column(name = "cache_version", nullable = false, length = 100)
    private String cacheVersion; // 결과 버전 (모델+프롬프트). 바꾸면 예전 저장분은 쓰지 않음

    @Column(name = "kind", nullable = false, length = 20)
    private String kind; // CAUTION / NO_EFFECT / OTHER / UNVERIFIED

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation; // 쉬운 설명 (없으면 null)

    @Column(name = "action", columnDefinition = "TEXT")
    private String action; // 할 일 (없으면 null)

    @Column(name = "note", columnDefinition = "TEXT")
    private String note; // 원문을 확인해 달라는 안내 (없으면 null)

    @Column(name = "sentence", nullable = false, columnDefinition = "TEXT")
    private String sentence; // 사람이 확인할 수 있게 원문도 함께 저장

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
