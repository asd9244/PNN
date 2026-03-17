package com.pnn.backend.controller;

import com.pnn.backend.dto.SupplementOcrResponseDto;
import com.pnn.backend.service.SupplementOcrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 영양제 OCR 프록시 API
 * <p>클라이언트 이미지 → Spring → Python AI 서버 /api/v1/supplement/extract</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/supplements")
@RequiredArgsConstructor
@Tag(name = "Supplement OCR API", description = "영양성분표 이미지 OCR")
public class SupplementController {

    private final SupplementOcrService supplementOcrService;

    @Operation(summary = "영양성분표 OCR", description = "이미지를 Python AI 서버로 전달하여 성분 정보를 추출합니다.")
    @PostMapping(value = "/ocr/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SupplementOcrResponseDto> extractNutrients(
            @RequestPart("image") MultipartFile image) {
        log.info("영양제 OCR API 호출 - filename: {}", image.getOriginalFilename());
        SupplementOcrResponseDto response = supplementOcrService.extractNutrients(image);
        return ResponseEntity.ok(response);
    }
}
