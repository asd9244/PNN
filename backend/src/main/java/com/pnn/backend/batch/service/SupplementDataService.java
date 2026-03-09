package com.pnn.backend.batch.service;

import com.pnn.backend.batch.client.FoodSafetyClient;
import com.pnn.backend.domain.Supplement;
import com.pnn.backend.batch.dto.SupplementResponse;
import com.pnn.backend.repository.SupplementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplementDataService {

    private final FoodSafetyClient foodSafetyClient;
    private final SupplementRepository supplementRepository;
    private final TransactionTemplate transactionTemplate;

    public String fetchAllAndSaveSupplements() { // 전체 건강기능식품 데이터 적재
        log.info("=== supplements 적재 시작 (건강기능식품 API) ===");

        SupplementResponse firstResponse = foodSafetyClient.fetchSupplements(1, 1);
        if (firstResponse == null || firstResponse.getData() == null) {
            return "건강기능식품 API 초기 호출 실패";
        }

        int totalCount = Integer.parseInt(firstResponse.getData().getTotalCount());
        int batchSize = 500; // 한 번에 가져올 건수
        int totalSaved = 0;
        int totalSkipped = 0;
        int failedBatches = 0;

        int totalBatches = (int) Math.ceil((double) totalCount / batchSize);
        log.info("건강기능식품 전체 건수: {}, 배치 크기: {}, 총 배치: {}", totalCount, batchSize, totalBatches);

        for (int i = 0; i < totalBatches; i++) {
            int startIdx = i * batchSize + 1;
            int endIdx = Math.min((i + 1) * batchSize, totalCount);
            final int batchNum = i + 1;

            Integer savedInBatch = transactionTemplate.execute(status -> {
                try {
                    return saveSupplementsBatch(startIdx, endIdx);
                } catch (Exception e) {
                    log.error("적재 실패 (batch {}, {}-{}): {}", batchNum, startIdx, endIdx, e.getMessage());
                    status.setRollbackOnly();
                    return -1;
                }
            });

            if (savedInBatch != null && savedInBatch >= 0) {
                totalSaved += savedInBatch;
                totalSkipped += (endIdx - startIdx + 1) - savedInBatch;
            } else {
                failedBatches++;
            }

            if (batchNum % 10 == 0 || batchNum == totalBatches) {
                log.info("적재 진행: {}/{} 배치 (저장: {}, 스킵: {}, 실패 배치: {})",
                        batchNum, totalBatches, totalSaved, totalSkipped, failedBatches);
            }
        }

        String result = String.format("적재 완료! 전체: %d건, 저장: %d건, 스킵(중복): %d건, 실패 배치: %d",
                totalCount, totalSaved, totalSkipped, failedBatches);
        log.info(result);
        return result;
    }

    public String fetchAndSaveSupplements(int startIdx, int endIdx) { // 단일 범위 테스트용
        return transactionTemplate.execute(status -> {
            int saved = saveSupplementsBatch(startIdx, endIdx);
            return String.format("범위 %d-%d 처리 완료. 저장된 건수: %d", startIdx, endIdx, saved);
        });
    }

    private int saveSupplementsBatch(int startIdx, int endIdx) { // 한 배치의 데이터 저장
        SupplementResponse response = foodSafetyClient.fetchSupplements(startIdx, endIdx);

        if (response == null || response.getData() == null || response.getData().getRow() == null) {
            log.warn("범위 {}-{} 응답이 비어있음", startIdx, endIdx);
            return 0;
        }

        String resultCode = response.getData().getResult() != null
                ? response.getData().getResult().getCode() : null;
        if (!"INFO-000".equals(resultCode)) {
            log.warn("API 오류 응답 (범위 {}-{}): {}", startIdx, endIdx, resultCode);
            return 0;
        }

        List<SupplementResponse.Row> rows = response.getData().getRow();
        int count = 0;

        for (SupplementResponse.Row row : rows) {
            if (row.getPrdlstReportNo() == null || row.getPrdlstNm() == null) continue;

            if (supplementRepository.existsByPrdlstReportNo(row.getPrdlstReportNo())) {
                continue; // 이미 저장된 품목 스킵
            }

            Supplement supplement = new Supplement();
            supplement.setPrdlstReportNo(row.getPrdlstReportNo());
            supplement.setPrdlstNm(row.getPrdlstNm());
            supplement.setBsshNm(row.getBsshNm());
            supplement.setPrimaryFnclty(row.getPrimaryFnclty());
            supplement.setRawmtrlNm(row.getRawmtrlNm());
            supplement.setIndivRawmtrlNm(row.getIndivRawmtrlNm());
            supplement.setEtcRawmtrlNm(row.getEtcRawmtrlNm());
            supplement.setCapRawmtrlNm(row.getCapRawmtrlNm());
            supplement.setNtkMthd(row.getNtkMthd());
            supplement.setIftknAtntMatrCn(row.getIftknAtntMatrCn());
            supplement.setStdrStnd(row.getStdrStnd());

            supplementRepository.save(supplement);
            count++;
        }
        return count;
    }
}
