package com.pnn.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 약물-영양제 간 실제 상호작용(충돌)을 판별하는 핵심 비즈니스 로직 서비스 (Case A)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InteractionCheckService {

    // private final DrugRepository drugRepository;
    // private final InteractionRuleService interactionRuleService;
    // private final PythonAiClient pythonAiClient;

    /**
     * 특정 약물(Drug)과 영양제(Supplement)들의 상호작용을 검사합니다.
     */
    public void checkInteraction(/* TODO: Request DTO */) {
        // 1. Drug 정보 조회 및 main_ingr_eng split
        
        // 2. Rule-based 1차 필터링 (interaction_rules)
        
        // 3. 미결정 시 Python AI (LLM+RAG) 2차 분석 요청
        
        // 4. 결과 통합 반환
    }
}