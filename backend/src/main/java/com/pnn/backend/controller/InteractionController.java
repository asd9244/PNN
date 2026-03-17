package com.pnn.backend.controller;

import com.pnn.backend.dto.InteractionCheckRequestDto;
import com.pnn.backend.dto.InteractionCheckResponseDto;
import com.pnn.backend.service.InteractionCheckService;
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
 * Case A: 신규 처방약 ↔ 기복용 영양제 충돌 검사 API
 * <p>POST /api/interaction/check — 신규 처방약과 기복용 영양제 간 상호작용을 AI(Gemini)로 분석</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/interaction")
@RequiredArgsConstructor
@Tag(name = "Interaction Check API", description = "처방약-영양제 상호작용 검사 (Case A)")
public class InteractionController {

    private final InteractionCheckService interactionCheckService;

    @Operation(summary = "상호작용 검사", description = "신규 처방약과 기복용 영양제 간의 상호작용을 AI로 분석합니다.")
    @PostMapping("/check")
    public ResponseEntity<InteractionCheckResponseDto> checkInteraction(
            @Valid @RequestBody InteractionCheckRequestDto request) {
        log.info("상호작용 검사 API 호출 - drugId: {}", request.getDrugId());
        InteractionCheckResponseDto response = interactionCheckService.checkInteraction(request);
        return ResponseEntity.ok(response);
    }
}
