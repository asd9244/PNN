package com.pnn.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController // 이 클래스를 REST API 컨트롤러로 등록, 반환값을 JSON으로 자동 직렬화
@RequestMapping("/api/health") // 이 컨트롤러의 모든 엔드포인트 공통 경로 prefix
public class HealthController {

    @GetMapping // GET /api/health 요청을 이 메서드에 매핑
    public ResponseEntity<Map<String, Object>> health() { // ResponseEntity: HTTP 상태코드 + 응답 본문을 함께 반환
        Map<String, Object> body = new HashMap<>(); // JSON 응답 본문을 담을 Map 생성
        body.put("status", "ok"); // { "status": "ok" } 형태의 응답 데이터
        return ResponseEntity.ok(body); // HTTP 200 + body를 JSON으로 반환
    }
}
