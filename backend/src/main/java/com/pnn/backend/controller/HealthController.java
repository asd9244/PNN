package com.pnn.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController // REST API 컨트롤러 등록
@RequestMapping("/api/health") // 공통 경로 prefix
public class HealthController {

    @GetMapping // GET /api/health — Spring 자체 헬스 체크
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "ok");
        body.put("server", "PNN Backend");
        return ResponseEntity.ok(body);
    }
}
