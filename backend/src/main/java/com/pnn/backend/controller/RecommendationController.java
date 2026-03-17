package com.pnn.backend.controller;

import com.pnn.backend.dto.RecommendationRequestDto;
import com.pnn.backend.dto.RecommendationResponseDto;
import com.pnn.backend.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * Case B: 기복용 처방약 기반 안전 영양제 추천 API
 * <p>POST /api/recommendations/safe-nutrients — 기복용 처방약을 고려해 안전한 영양 성분을 AI(Gemini)로 추천</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Tag(name = "Recommendation API", description = "안전 영양제 추천 (Case B)")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(summary = "안전 영양 성분 추천", description = "기복용 처방약을 고려하여 안전한 영양 성분을 AI로 추천합니다.")
    @PostMapping("/safe-nutrients")
    public ResponseEntity<RecommendationResponseDto> recommendSafeNutrients(
            @Valid @RequestBody RecommendationRequestDto request) {
        log.info("안전 추천 API 호출 - drugIds: {}", request.getDrugIds());
        RecommendationResponseDto response = recommendationService.recommendSafeNutrients(request);
        return ResponseEntity.ok(response);
    }
}
