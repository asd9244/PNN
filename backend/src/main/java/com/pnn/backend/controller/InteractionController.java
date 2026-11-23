package com.pnn.backend.controller;

import com.pnn.backend.dto.InteractionCheckRequestDto;
import com.pnn.backend.dto.InteractionCheckResponseDto;
import com.pnn.backend.dto.InteractionEvidenceResponseDto;
import com.pnn.backend.service.InteractionCheckService;
import com.pnn.backend.service.InteractionEvidenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import java.util.List;

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
    private final InteractionEvidenceService interactionEvidenceService;

    @Operation(summary = "상호작용 검사", description = "여러 신규 처방약과 기복용 영양제 간의 상호작용을 AI로 분석합니다.")
    @PostMapping("/check")
    public ResponseEntity<InteractionCheckResponseDto> checkInteraction(
            @Valid @RequestBody InteractionCheckRequestDto request) {
        log.info("상호작용 검사 API 호출 - drugIds: {}", request.getDrugIds());
        InteractionCheckResponseDto response = interactionCheckService.checkInteraction(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "설명서 근거 문장 찾기",
            description = "약 설명서(식약처 주의사항)의 상호작용 항목에서 영양 성분별로 언급된 원문 문장을 찾습니다. AI를 쓰지 않습니다.")
    @GetMapping("/evidence")
    public ResponseEntity<InteractionEvidenceResponseDto> findEvidence(
            @RequestParam Long drugId, // 약 번호 (drugs_master.id)
            @RequestParam List<String> nutrients) { // 성분표 이름 목록 (예: 철, 칼슘, 비타민 C)
        log.info("근거 문장 찾기 API 호출 - drugId: {}, nutrients: {}", drugId, nutrients);
        return ResponseEntity.ok(interactionEvidenceService.findEvidence(drugId, nutrients));
    }
}
