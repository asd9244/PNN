package com.pnn.backend.controller;

import com.pnn.backend.dto.InteractionRequestDto;
import com.pnn.backend.dto.InteractionResponseDto;
import com.pnn.backend.service.InteractionCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 상호작용 검사 (Case A) 및 추천 (Case B) 관련 컨트롤러
 * 클라이언트의 HTTP 요청을 수신하고, Service 계층으로 처리를 위임한 뒤 결과를 HTTP 응답으로 반환하는 프레젠테이션 계층.
 */
@Slf4j
@RestController
@RequestMapping("/api/interaction")
@RequiredArgsConstructor
public class InteractionController {

        private final InteractionCheckService interactionCheckService;

        /**
         * POST /api/interaction/check
         * 처방약과 영양제 간 상호작용(충돌) 검사를 요청하는 엔드포인트 (Case A)
         *
         * @param request 클라이언트가 보낸 JSON 본문. drugId(처방약 ID)와 supplements(영양제 목록)를 포함.
         * @return 상호작용 검사 결과 (summary, source, interactions 목록)
         */
        @PostMapping("/check")
        public ResponseEntity<InteractionResponseDto> checkInteraction(@RequestBody InteractionRequestDto request) {
                log.info("상호작용 검사 요청 수신. drugId={}, supplements 수={}",
                                request.getDrugId(),
                                request.getSupplements() != null ? request.getSupplements().size() : 0);

                InteractionResponseDto response = interactionCheckService.checkInteraction(request);

                return ResponseEntity.ok(response);
        }
}