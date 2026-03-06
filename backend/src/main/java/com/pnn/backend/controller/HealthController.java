package com.pnn.backend.controller;

import com.pnn.backend.client.PythonAiClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController // REST API 컨트롤러 등록
@RequestMapping("/api/health") // 공통 경로 prefix
public class HealthController {

    private final PythonAiClient pythonAiClient; // Python 서버 호출 클라이언트

    public HealthController(PythonAiClient pythonAiClient) { // 생성자 주입
        this.pythonAiClient = pythonAiClient;
    }

    @GetMapping // GET /api/health — Spring 자체 헬스 체크
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "ok");
        body.put("server", "PNN Backend");
        return ResponseEntity.ok(body);
    }

    @GetMapping("/ai") // GET /api/health/ai — Python AI 서버 연결 확인
    public ResponseEntity<Map<String, Object>> aiHealth() {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> response = pythonAiClient.healthCheck(); // Python 서버 호출
            result.put("spring", "ok");
            result.put("python", response);
        } catch (Exception e) {
            result.put("spring", "ok");
            result.put("python", "error: " + e.getMessage()); // 실패 시 에러 메시지 반환
        }
        return ResponseEntity.ok(result);
    }
}
