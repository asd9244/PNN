package com.pnn.backend.batch.client;

import com.pnn.backend.batch.dto.SupplementResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class FoodSafetyClient {

    private final RestClient restClient;

    @Value("${api.foodsafety.key}")
    private String apiKey; // 식품안전나라 인증키

    @Value("${api.foodsafety.supplement.url}")
    private String baseUrl; // 식품안전나라 API 베이스 URL

    public FoodSafetyClient() {
        this.restClient = RestClient.builder().build();
    }

    public SupplementResponse fetchSupplements(int startIdx, int endIdx) { // 건강기능식품 조회
        String url = String.format("%s/%s/I0030/json/%d/%d", baseUrl, apiKey, startIdx, endIdx);

        try {
            return restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(SupplementResponse.class);
        } catch (Exception e) {
            log.error("건강기능식품 API 호출 실패 (startIdx={}, endIdx={}): {}", startIdx, endIdx, e.getMessage());
            return null;
        }
    }
}
