package com.pnn.backend.client;

import com.pnn.backend.client.dto.AiEvidenceReadRequest;
import com.pnn.backend.client.dto.AiEvidenceReadResponse;
import com.pnn.backend.client.dto.AiInteractionRequest;
import com.pnn.backend.client.dto.AiInteractionResponse;
import com.pnn.backend.client.dto.AiRecommendationRequest;
import com.pnn.backend.client.dto.AiRecommendationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Python AI 서버와 통신하는 RestClient 래퍼
 * <p>설정: ai.server.url (기본값 http://localhost:8000)</p>
 * <p>Case A: POST /api/v1/interaction/analyze — 처방약+영양제 상호작용 분석</p>
 * <p>Case B: POST /api/v1/recommendation/analyze-safe — 안전 영양 성분 추천</p>
 * <p>Case A(근거 기반): POST /api/v1/interaction/read-evidence — 설명서 근거 문장 읽기</p>
 */
@Slf4j
@Component
public class AiServerClient {

    private final RestClient restClient;

    public AiServerClient(
            @Value("${ai.server.url:http://localhost:8000}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(90)); // LLM 추론 대기 (Gemini 등)

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader(org.springframework.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Case A: 처방약 + 영양제 상호작용 분석
     * POST /api/v1/interaction/analyze
     */
    public AiInteractionResponse analyzeInteraction(AiInteractionRequest request) {
        log.debug("AI 서버 상호작용 분석 요청: drug={}", request.getDrug() != null ? request.getDrug().getName() : null);
        return restClient.post()
                .uri("/api/v1/interaction/analyze")
                .body(request)
                .retrieve()
                .body(AiInteractionResponse.class);
    }

    /** Case A(근거 기반): 설명서 근거 문장을 AI로 읽기. POST /api/v1/interaction/read-evidence */
    public AiEvidenceReadResponse readEvidence(AiEvidenceReadRequest request) {
        log.debug("AI 서버 근거 읽기 요청: drug={}, 문장 수={}", request.getDrugName(),
                request.getItems() != null ? request.getItems().size() : 0);
        return restClient.post()
                .uri("/api/v1/interaction/read-evidence")
                .body(request)
                .retrieve()
                .body(AiEvidenceReadResponse.class);
    }

    /** Case B: POST /api/v1/recommendation/analyze-safe */
    public AiRecommendationResponse recommendSafeNutrients(AiRecommendationRequest request) {
        log.debug("AI 서버 안전 추천 요청: patient_drugs 수={}", request.getPatientDrugs() != null ? request.getPatientDrugs().size() : 0);
        return restClient.post()
                .uri("/api/v1/recommendation/analyze-safe")
                .body(request)
                .retrieve()
                .body(AiRecommendationResponse.class);
    }
}
