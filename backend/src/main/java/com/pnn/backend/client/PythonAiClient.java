package com.pnn.backend.client;

import com.pnn.backend.dto.InteractionRequestDto;
import com.pnn.backend.dto.InteractionResponseDto;
import com.pnn.backend.dto.RecommendationAnalyzeRequestDto;
import com.pnn.backend.dto.RecommendationAnalyzeResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component // Spring Bean으로 등록
public class PythonAiClient {

    private final RestClient restClient; // Spring 6.1+ 내장 HTTP 클라이언트

    // application.properties에서 URL 주입
    public PythonAiClient(@Value("${ai.server.url}") String aiServerUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(aiServerUrl)
                .build();
    }

    public Map<String, Object> healthCheck() { // Python 서버 헬스 체크 호출
        return restClient.get()
                .uri("/api/v1/health") // Python 헬스 체크 엔드포인트
                .retrieve()
                .body(Map.class); // JSON 응답을 Map으로 역직렬화
    }

    /**
     * Python AI 서버에 상호작용 분석을 요청합니다.
     */
    public InteractionResponseDto analyzeInteraction(InteractionRequestDto request, String drugName,
            java.util.List<String> ingredients) {
        // Python 서버가 기대하는 입력 형태에 맞춰 데이터 조립 (InteractionAnalyzeRequest)
        Map<String, Object> requestBody = new HashMap<>();

        Map<String, Object> drugData = new HashMap<>();
        drugData.put("id", request.getDrugId().toString());
        drugData.put("name", drugName);
        drugData.put("ingredients", ingredients); // 빈 리스트면 fallback 안내로 빠짐

        requestBody.put("drug", drugData);
        requestBody.put("supplements", request.getSupplements());

        try {
            return restClient.post()
                    .uri("/api/v1/interaction/analyze")
                    .body(requestBody)
                    .retrieve()
                    .body(InteractionResponseDto.class);
        } catch (Exception e) {
            throw new RuntimeException("AI 서버 호출 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * Python AI 서버에 안전 영양제 추천 분석(Case B)을 요청합니다.
     * 
     * @param request 처방약 목록과 금기 성분 목록을 포함한 요청 DTO
     * @return AI가 생성한 안전 영양제 추천 결과
     */
    public RecommendationAnalyzeResponseDto analyzeSafeNutrients(RecommendationAnalyzeRequestDto request) {
        try {
            return restClient.post()
                    .uri("/api/v1/recommendation/analyze-safe")
                    .body(request)
                    .retrieve()
                    .body(RecommendationAnalyzeResponseDto.class);
        } catch (Exception e) {
            throw new RuntimeException("AI 서버 호출(안전 성분 추천) 중 오류가 발생했습니다.", e);
        }
    }
}
