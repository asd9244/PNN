package com.pnn.backend.controller;

import com.pnn.backend.dto.RecommendationRequestDto;
import com.pnn.backend.dto.RecommendationResponseDto;
import com.pnn.backend.service.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 기복용 처방약 기반 영양제 추천 (Case B) 관련 컨트롤러
 * 클라이언트의 HTTP 요청을 수신하고, Service 계층으로 처리를 위임한 뒤 결과를 반환합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    /**
     * POST /api/v1/recommendations/safe-nutrients
     * 처방약 정보를 바탕으로 안전한 영양 성분 추천 및 금기 성분 경고를 요청하는 엔드포인트
     *
     * @param request 클라이언트가 보낸 JSON 본문. 기복용 중인 약물 ID 목록(drugIds) 포함.
     * @return 금기 성분 목록(warnings)과 추천 안전 성분 목록(recommendations)
     */
    @PostMapping("/safe-nutrients")
    public ResponseEntity<RecommendationResponseDto> recommendSafeNutrients(
            @Valid @RequestBody RecommendationRequestDto request) {
        
        log.info("안전 영양제 추천(Case B) 요청 수신. 복용 중인 약물 개수={}",
                request.getDrugIds() != null ? request.getDrugIds().size() : 0);

        RecommendationResponseDto response = recommendationService.recommendSafeNutrients(request);

        return ResponseEntity.ok(response);
    }
}
