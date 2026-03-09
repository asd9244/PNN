package com.pnn.backend.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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
}
