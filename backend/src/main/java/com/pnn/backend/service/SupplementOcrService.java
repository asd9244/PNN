package com.pnn.backend.service;

import com.pnn.backend.dto.SupplementOcrResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.Collections;

/**
 * 영양제 OCR 서비스
 * <p>이미지를 Python AI 서버 /api/v1/supplement/extract 로 전달</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplementOcrService {

    @Value("${ai.server.url:http://localhost:8000}")
    private String aiServerUrl;

    public SupplementOcrResponseDto extractNutrients(MultipartFile image) {
        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(Duration.ofSeconds(10));
            factory.setReadTimeout(Duration.ofSeconds(90)); // OCR + LLM 추론 대기 (기본값 대비 충분히 여유)

            RestClient restClient = RestClient.builder()
                    .baseUrl(aiServerUrl)
                    .requestFactory(factory)
                    .build();

            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            String contentType = image.getContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = "image/jpeg";
            }
            builder.part("image", new ByteArrayResource(image.getBytes()) {
                @Override
                public String getFilename() {
                    return image.getOriginalFilename() != null ? image.getOriginalFilename() : "image.jpg";
                }
            }).contentType(MediaType.parseMediaType(contentType));

            var response = restClient.post()
                    .uri("/api/v1/supplement/extract")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(builder.build())
                    .retrieve()
                    .body(SupplementOcrResponseDto.class);

            if (response == null) {
                return SupplementOcrResponseDto.builder()
                        .error("OCR 응답이 비어 있습니다.")
                        .nutrients(Collections.emptyList())
                        .build();
            }
            return response;
        } catch (Exception e) {
            log.warn("OCR 호출 실패", e);
            return SupplementOcrResponseDto.builder()
                    .error("OCR 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.")
                    .nutrients(Collections.emptyList())
                    .build();
        }
    }
}
