package com.pnn.backend.batch.controller;

import com.pnn.backend.batch.service.DrugDataService;
import com.pnn.backend.batch.service.SupplementDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@Slf4j // 로깅 사용
@RestController // REST API 컨트롤러
@RequestMapping("/api/batch") // 공통 URL 경로 설정
@RequiredArgsConstructor // 의존성 주입 (생성자 자동 생성)
public class BatchController {

    private final DrugDataService drugDataService;
    private final SupplementDataService supplementDataService;

    // 수동으로 데이터 적재 배치를 실행하는 엔드포인트 (단일 페이지)
    // 예: http://localhost:8080/api/batch/drug-identification?pageNo=1&numOfRows=10
    @GetMapping("/drug-identification")
    public ResponseEntity<String> runDrugIdentificationBatch(
            @RequestParam(defaultValue = "1") int pageNo, // 페이지 번호 (기본값 1)
            @RequestParam(defaultValue = "10") int numOfRows) { // 가져올 데이터 수 (기본값 10)

        log.info("Starting drug identification batch manually. page={}, rows={}", pageNo, numOfRows);
        
        // 서비스 로직 호출하여 데이터 적재 실행
        String result = drugDataService.fetchAndSaveDrugIdentification(pageNo, numOfRows);
        
        return ResponseEntity.ok(result); // 결과 메시지 반환
    }

    // 전체 데이터 적재 (비동기 실행 권장)
    // 예: http://localhost:8080/api/batch/drug-identification/all
    @GetMapping("/drug-identification/all")
    public ResponseEntity<String> runAllDrugIdentificationBatch() {
        log.info("Request received to fetch ALL drug identification data.");

        CompletableFuture.runAsync(() -> { // 비동기로 실행하여 브라우저 타임아웃 방지
            try {
                String result = drugDataService.fetchAllAndSaveDrugIdentification();
                log.info("Batch Job Completed: {}", result);
            } catch (Exception e) {
                log.error("Batch Job Failed", e);
            }
        });

        return ResponseEntity.ok("Batch job started in background. Check server logs for progress.");
    }

    // drugs 테이블 보강 — 단일 페이지 테스트용
    // 예: http://localhost:8080/api/batch/drug-enrich?pageNo=1&numOfRows=10
    @GetMapping("/drug-enrich")
    public ResponseEntity<String> runDrugEnrichBatch(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int numOfRows) {

        log.info("drugs 보강 수동 실행. page={}, rows={}", pageNo, numOfRows);
        String result = drugDataService.enrichDrugsWithPermissionDetail(pageNo, numOfRows);
        return ResponseEntity.ok(result);
    }

    // drugs 테이블 보강 — 전체 실행 (비동기)
    // 예: http://localhost:8080/api/batch/drug-enrich/all
    @GetMapping("/drug-enrich/all")
    public ResponseEntity<String> runAllDrugEnrichBatch() {
        log.info("drugs 전체 보강 요청 수신 (허가 상세정보 API)");

        CompletableFuture.runAsync(() -> { // 비동기 실행
            try {
                String result = drugDataService.enrichAllDrugsWithPermissionDetail();
                log.info("보강 배치 완료: {}", result);
            } catch (Exception e) {
                log.error("보강 배치 실패", e);
            }
        });

        return ResponseEntity.ok("drugs 보강 배치가 백그라운드에서 시작되었습니다. 서버 로그를 확인하세요.");
    }

    // drug_ingredients 적재 — 단일 페이지 테스트용
    // 예: http://localhost:8080/api/batch/drug-ingredient?pageNo=1&numOfRows=10
    @GetMapping("/drug-ingredient")
    public ResponseEntity<String> runDrugIngredientBatch(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int numOfRows) {

        log.info("drug_ingredients 적재 수동 실행. page={}, rows={}", pageNo, numOfRows);
        String result = drugDataService.fetchAndSaveDrugIngredients(pageNo, numOfRows);
        return ResponseEntity.ok(result);
    }

    // drug_ingredients 적재 — 전체 실행 (비동기)
    // 예: http://localhost:8080/api/batch/drug-ingredient/all
    @GetMapping("/drug-ingredient/all")
    public ResponseEntity<String> runAllDrugIngredientBatch() {
        log.info("drug_ingredients 전체 적재 요청 수신 (주성분 상세정보 API)");

        CompletableFuture.runAsync(() -> {
            try {
                String result = drugDataService.fetchAllAndSaveDrugIngredients();
                log.info("성분 적재 배치 완료: {}", result);
            } catch (Exception e) {
                log.error("성분 적재 배치 실패", e);
            }
        });

        return ResponseEntity.ok("drug_ingredients 적재 배치가 백그라운드에서 시작되었습니다. 서버 로그를 확인하세요.");
    }

    // supplements 적재 — 단일 범위 테스트용
    // 예: http://localhost:8080/api/batch/supplement?startIdx=1&endIdx=5
    @GetMapping("/supplement")
    public ResponseEntity<String> runSupplementBatch(
            @RequestParam(defaultValue = "1") int startIdx,
            @RequestParam(defaultValue = "10") int endIdx) {

        log.info("supplements 적재 수동 실행. startIdx={}, endIdx={}", startIdx, endIdx);
        String result = supplementDataService.fetchAndSaveSupplements(startIdx, endIdx);
        return ResponseEntity.ok(result);
    }

    // supplements 적재 — 전체 실행 (비동기)
    // 예: http://localhost:8080/api/batch/supplement/all
    @GetMapping("/supplement/all")
    public ResponseEntity<String> runAllSupplementBatch() {
        log.info("supplements 전체 적재 요청 수신 (건강기능식품 API)");

        CompletableFuture.runAsync(() -> {
            try {
                String result = supplementDataService.fetchAllAndSaveSupplements();
                log.info("supplements 적재 배치 완료: {}", result);
            } catch (Exception e) {
                log.error("supplements 적재 배치 실패", e);
            }
        });

        return ResponseEntity.ok("supplements 적재 배치가 백그라운드에서 시작되었습니다. 서버 로그를 확인하세요.");
    }
}
