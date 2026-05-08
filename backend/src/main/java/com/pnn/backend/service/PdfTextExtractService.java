package com.pnn.backend.service;

import com.pnn.backend.domain.DrugPermitDetail;
import com.pnn.backend.repository.DrugPermitDetailRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * 비즈니스 계층 (Service)
 * drug_permit_detail의 efficacy/dosage/caution에 저장된 PDF URL을 텍스트로 변환합니다.
 * 최초 변환 후 DB에 캐시하여, 이후 조회에서는 텍스트가 즉시 반환됩니다.
 */
@Slf4j
@Service
public class PdfTextExtractService {

    private final DrugPermitDetailRepository drugPermitDetailRepository;
    private final RestClient pdfRestClient;

    public PdfTextExtractService(DrugPermitDetailRepository drugPermitDetailRepository) {
        this.drugPermitDetailRepository = drugPermitDetailRepository;
        this.pdfRestClient = RestClient.builder()
                .defaultHeader("User-Agent", "PNN-Backend/1.0")
                .build();
    }

    /** nedrug.mfds.go.kr PDF URL 패턴 판별 */
    public boolean isPdfUrl(String value) {
        if (value == null || value.isBlank()) return false;
        String trimmed = value.trim();
        return trimmed.startsWith("http")
                && (trimmed.endsWith("/EE") || trimmed.endsWith("/UD") || trimmed.endsWith("/NB")
                    || trimmed.toLowerCase().contains("pdf"));
    }

    /**
     * DrugPermitDetail의 efficacy/dosage/caution이 PDF URL이면 텍스트로 변환하고 DB에 캐시합니다.
     * 변환된 텍스트가 담긴 DrugPermitDetail을 반환합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW) // 읽기 전용 getDrugDetail 트랜잭션과 분리
    public DrugPermitDetail resolveAndCache(DrugPermitDetail permit) {
        if (permit == null) return null;

        boolean changed = false;

        if (isPdfUrl(permit.getEfficacy())) {
            String text = extractTextFromPdfUrl(permit.getEfficacy());
            if (text != null) {
                permit.setEfficacy(text);
                changed = true;
            }
        }
        if (isPdfUrl(permit.getDosage())) {
            String text = extractTextFromPdfUrl(permit.getDosage());
            if (text != null) {
                permit.setDosage(text);
                changed = true;
            }
        }
        if (isPdfUrl(permit.getCaution())) {
            String text = extractTextFromPdfUrl(permit.getCaution());
            if (text != null) {
                permit.setCaution(text);
                changed = true;
            }
        }

        if (changed) {
            drugPermitDetailRepository.save(permit);
            log.info("PDF 텍스트 캐시 저장 완료 - itemSeq: {}", permit.getItemSeq());
        }

        return permit;
    }

    /** URL에서 PDF를 다운로드하여 텍스트를 추출합니다. 실패 시 null 반환. */
    private String extractTextFromPdfUrl(String url) {
        try {
            byte[] pdfBytes = pdfRestClient.get()
                    .uri(url.trim())
                    .retrieve()
                    .body(byte[].class);

            if (pdfBytes == null || pdfBytes.length == 0) {
                log.warn("PDF 다운로드 결과가 비어 있음: {}", url);
                return null;
            }

            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);
                if (text != null && !text.isBlank()) {
                    return text.replace("\u0000", "").trim(); // PostgreSQL NUL 바이트 제거
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("PDF 텍스트 추출 실패: {} - {}", url, e.getMessage());
            return null;
        }
    }
}
