package com.pnn.backend.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 상호작용 검사 (Case A) 및 추천 (Case B) 관련 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/interaction")
@RequiredArgsConstructor
public class InteractionController {

    // private final InteractionCheckService interactionCheckService;

    // TODO: POST /check (상호작용 1차/2차 검사 엔드포인트)
}