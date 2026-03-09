package com.pnn.backend.batch.service;

import com.pnn.backend.batch.client.PublicDataClient;
import com.pnn.backend.domain.Drug;
import com.pnn.backend.domain.DrugIdentification;
import com.pnn.backend.domain.DrugIngredient;
import com.pnn.backend.batch.dto.DrugIdentificationResponse;
import com.pnn.backend.batch.dto.DrugIngredientResponse;
import com.pnn.backend.batch.dto.DrugPermissionDetailResponse;
import com.pnn.backend.repository.DrugIdentificationRepository;
import com.pnn.backend.repository.DrugIngredientRepository;
import com.pnn.backend.repository.DrugRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j // 로깅 기능 활성화
@Service // 서비스 계층 컴포넌트 등록
@RequiredArgsConstructor // final 필드에 대한 생성자 자동 생성 (의존성 주입)
public class DrugDataService {

    private final PublicDataClient publicDataClient; // API 클라이언트
    private final DrugRepository drugRepository; // 약품 정보 DB 접근
    private final DrugIdentificationRepository drugIdentificationRepository; // 낱알 식별 정보 DB 접근
    private final DrugIngredientRepository drugIngredientRepository; // 주성분 정보 DB 접근
    private final TransactionTemplate transactionTemplate; // 트랜잭션 수동 제어용

    // =====================================================
    // 낱알식별 정보 적재 (기존)
    // =====================================================

    // 전체 데이터 적재 메서드 (페이지 순회)
    public String fetchAllAndSaveDrugIdentification() {
        int pageNo = 1;
        int numOfRows = 100; // 한 번에 가져올 데이터 수 (API 제한 고려)
        int totalSaved = 0;
        int totalProcessed = 0;

        log.info("Start fetching ALL drug identification data...");

        // 첫 번째 호출로 전체 개수 파악
        DrugIdentificationResponse firstResponse = publicDataClient.fetchDrugIdentification(1, 1);
        if (firstResponse == null || firstResponse.getBody() == null) {
            return "Failed to fetch initial data.";
        }
        int totalCount = firstResponse.getBody().getTotalCount();
        log.info("Total items to fetch: {}", totalCount);

        // 전체 페이지 수 계산
        int totalPages = (int) Math.ceil((double) totalCount / numOfRows);

        for (int i = 1; i <= totalPages; i++) {
            final int currentPage = i;
            // 각 페이지 처리를 별도의 트랜잭션으로 실행 (실패 시 해당 페이지 데이터만 롤백, 메모리 관리 용이)
            Integer savedInPage = transactionTemplate.execute(status -> {
                try {
                    return processPage(currentPage, numOfRows);
                } catch (Exception e) {
                    log.error("Error processing page {}", currentPage, e);
                    status.setRollbackOnly(); // 에러 발생 시 롤백
                    return 0;
                }
            });

            if (savedInPage != null) {
                totalSaved += savedInPage;
                totalProcessed += numOfRows; // 대략적인 처리 수
            }
            
            // API 과부하 방지를 위한 약간의 딜레이 (선택 사항, 필요 시 Thread.sleep(100))
            if (i % 10 == 0) {
                log.info("Progress: {}/{} pages processed. (Saved so far: {})", i, totalPages, totalSaved);
            }
        }

        return String.format("Completed! Total items: %d, Processed pages: %d, Total saved: %d", totalCount, totalPages, totalSaved);
    }

    // 단일 페이지 처리 (트랜잭션 내부에서 실행될 로직)
    private int processPage(int pageNo, int numOfRows) {
        log.info("Processing page {}...", pageNo);
        DrugIdentificationResponse response = publicDataClient.fetchDrugIdentification(pageNo, numOfRows);

        if (response == null || response.getBody() == null || response.getBody().getItems() == null) {
            return 0;
        }

        List<DrugIdentificationResponse.Item> items = response.getBody().getItems();
        int count = 0;

        for (DrugIdentificationResponse.Item item : items) {
            if (item.getItemSeq() == null) continue;

            // 1. Drug 엔티티 확인 또는 생성
            Drug drug = drugRepository.findByItemSeq(item.getItemSeq())
                    .orElseGet(() -> {
                        Drug newDrug = new Drug();
                        newDrug.setItemSeq(item.getItemSeq());
                        newDrug.setItemName(item.getItemName());
                        newDrug.setEntpName(item.getEntpName());
                        newDrug.setChart(item.getChart());
                        newDrug.setBigPrdtImgUrl(item.getItemImage());
                        return drugRepository.save(newDrug);
                    });

            // 2. DrugIdentification 엔티티 생성 및 저장 (중복 체크)
            // 지연 로딩 문제 방지를 위해 여기서 직접 조회하지 않고, 저장 시 예외 처리를 하거나 로직 유지
            // 여기서는 간단히 null 체크 (같은 트랜잭션 내에서는 1차 캐시로 인해 drug 객체가 관리되므로 getIdentification()이 동작할 수 있음)
            // 하지만 확실한 중복 방지를 위해 repository 조회를 권장하거나, DB 유니크 제약조건에 의존해야 함.
            // 일단 기존 로직 유지하되, drug 객체가 영속 상태여야 함.
            
            if (drug.getIdentification() == null) {
                DrugIdentification iden = new DrugIdentification();
                iden.setDrug(drug);
                iden.setDrugShape(item.getDrugShape());
                iden.setColorClass1(item.getColorClass1());
                iden.setColorClass2(item.getColorClass2());
                iden.setPrintFront(item.getPrintFront());
                iden.setPrintBack(item.getPrintBack());
                iden.setLineFront(item.getLineFront());
                iden.setLineBack(item.getLineBack());
                iden.setFormCodeName(item.getFormCodeName());
                iden.setClassName(item.getClassName());
                iden.setItemImage(item.getItemImage());

                drugIdentificationRepository.save(iden);
                count++;
            }
        }
        return count;
    }

    // 기존 단일 호출 메서드 (컨트롤러 테스트용, 트랜잭션 직접 사용)
    public String fetchAndSaveDrugIdentification(int pageNo, int numOfRows) {
        return transactionTemplate.execute(status -> {
            int saved = processPage(pageNo, numOfRows);
            return "Processed " + numOfRows + " items (requested), Saved " + saved + " new identifications.";
        });
    }

    // =====================================================
    // 허가 상세정보로 drugs 테이블 보강
    // =====================================================

    public String enrichAllDrugsWithPermissionDetail() { // 전체 페이지 순회하며 drugs 보강
        log.info("=== drugs 테이블 보강 시작 (허가 상세정보 API) ===");

        DrugPermissionDetailResponse firstResponse = publicDataClient.fetchDrugPermissionDetail(1, 1);
        if (firstResponse == null || firstResponse.getBody() == null) {
            return "허가 상세정보 API 초기 호출 실패";
        }

        int totalCount = firstResponse.getBody().getTotalCount();
        int numOfRows = 100; // 페이지당 100건
        int totalPages = (int) Math.ceil((double) totalCount / numOfRows);
        int totalEnriched = 0;
        int failedPages = 0;

        log.info("허가 상세정보 전체 건수: {}, 총 페이지: {}", totalCount, totalPages);

        for (int i = 1; i <= totalPages; i++) {
            final int currentPage = i;
            Integer enrichedInPage = transactionTemplate.execute(status -> {
                try {
                    return enrichDrugsPage(currentPage, numOfRows);
                } catch (Exception e) {
                    log.error("보강 실패 (page {}): {}", currentPage, e.getMessage());
                    status.setRollbackOnly();
                    return -1; // 실패 표시
                }
            });

            if (enrichedInPage != null && enrichedInPage >= 0) {
                totalEnriched += enrichedInPage;
            } else {
                failedPages++;
            }

            if (i % 10 == 0) {
                log.info("보강 진행: {}/{} 페이지 완료 (보강된 건: {}, 실패 페이지: {})", i, totalPages, totalEnriched, failedPages);
            }
        }

        String result = String.format("보강 완료! 전체: %d건, 페이지: %d, 보강: %d건, 실패 페이지: %d", totalCount, totalPages, totalEnriched, failedPages);
        log.info(result);
        return result;
    }

    public String enrichDrugsWithPermissionDetail(int pageNo, int numOfRows) { // 단일 페이지 테스트용
        return transactionTemplate.execute(status -> {
            int enriched = enrichDrugsPage(pageNo, numOfRows);
            return String.format("페이지 %d 처리 완료. 보강된 건수: %d", pageNo, enriched);
        });
    }

    private int enrichDrugsPage(int pageNo, int numOfRows) { // 한 페이지의 API 데이터로 drugs 업데이트
        DrugPermissionDetailResponse response = publicDataClient.fetchDrugPermissionDetail(pageNo, numOfRows);

        if (response == null || response.getBody() == null || response.getBody().getItems() == null) {
            log.warn("페이지 {} 응답이 비어있음 (null body 또는 items)", pageNo);
            return 0;
        }

        List<DrugPermissionDetailResponse.Item> items = response.getBody().getItems();
        AtomicInteger count = new AtomicInteger(0); // 람다 내에서 증가시키기 위해 AtomicInteger 사용

        for (DrugPermissionDetailResponse.Item item : items) {
            if (item.getItemSeq() == null) continue;

            drugRepository.findByItemSeq(item.getItemSeq()).ifPresent(drug -> { // ITEM_SEQ로 기존 Drug 조회
                boolean updated = false;

                if (drug.getEtcOtcCode() == null && item.getEtcOtcCode() != null) { // 전문/일반 구분
                    drug.setEtcOtcCode(item.getEtcOtcCode());
                    updated = true;
                }
                if (drug.getMaterialName() == null && item.getMaterialName() != null) { // 원료성분
                    drug.setMaterialName(item.getMaterialName());
                    updated = true;
                }
                if (drug.getMainItemIngr() == null && item.getMainItemIngr() != null) { // 주성분
                    drug.setMainItemIngr(item.getMainItemIngr());
                    updated = true;
                }
                if (drug.getIngrName() == null && item.getIngrName() != null) { // 성분명
                    drug.setIngrName(item.getIngrName());
                    updated = true;
                }
                if (drug.getAtcCode() == null && item.getAtcCode() != null) { // ATC 코드
                    drug.setAtcCode(item.getAtcCode());
                    updated = true;
                }
                if (drug.getTotalContent() == null && item.getTotalContent() != null) { // 총량
                    drug.setTotalContent(item.getTotalContent());
                    updated = true;
                }
                if (drug.getBigPrdtImgUrl() == null && item.getBigPrdtImgUrl() != null) { // 제품 이미지
                    drug.setBigPrdtImgUrl(item.getBigPrdtImgUrl());
                    updated = true;
                }

                if (updated) {
                    drugRepository.save(drug); // 변경사항 저장
                    count.incrementAndGet(); // 보강 건수 증가
                }
            });
        }
        return count.get();
    }

    // =====================================================
    // 주성분 상세정보 적재 (drug_ingredients)
    // =====================================================

    public String fetchAllAndSaveDrugIngredients() { // 전체 페이지 순회하며 성분 적재
        log.info("=== drug_ingredients 적재 시작 (주성분 상세정보 API) ===");

        DrugIngredientResponse firstResponse = publicDataClient.fetchDrugIngredient(1, 1);
        if (firstResponse == null || firstResponse.getBody() == null) {
            return "주성분 API 초기 호출 실패";
        }

        int totalCount = firstResponse.getBody().getTotalCount();
        int numOfRows = 100;
        int totalPages = (int) Math.ceil((double) totalCount / numOfRows);
        int totalSaved = 0;
        int failedPages = 0;

        log.info("주성분 전체 건수: {}, 총 페이지: {}", totalCount, totalPages);

        for (int i = 1; i <= totalPages; i++) {
            final int currentPage = i;
            Integer savedInPage = transactionTemplate.execute(status -> {
                try {
                    return saveIngredientsPage(currentPage, numOfRows);
                } catch (Exception e) {
                    log.error("성분 적재 실패 (page {}): {}", currentPage, e.getMessage());
                    status.setRollbackOnly();
                    return -1;
                }
            });

            if (savedInPage != null && savedInPage >= 0) {
                totalSaved += savedInPage;
            } else {
                failedPages++;
            }

            if (i % 10 == 0) {
                log.info("성분 적재 진행: {}/{} 페이지 (저장: {}, 실패 페이지: {})", i, totalPages, totalSaved, failedPages);
            }
        }

        String result = String.format("성분 적재 완료! 전체: %d건, 페이지: %d, 저장: %d건, 실패 페이지: %d", totalCount, totalPages, totalSaved, failedPages);
        log.info(result);
        return result;
    }

    public String fetchAndSaveDrugIngredients(int pageNo, int numOfRows) { // 단일 페이지 테스트용
        return transactionTemplate.execute(status -> {
            int saved = saveIngredientsPage(pageNo, numOfRows);
            return String.format("페이지 %d 처리 완료. 저장된 성분: %d건", pageNo, saved);
        });
    }

    private int saveIngredientsPage(int pageNo, int numOfRows) { // 한 페이지의 성분 데이터 저장
        DrugIngredientResponse response = publicDataClient.fetchDrugIngredient(pageNo, numOfRows);

        if (response == null || response.getBody() == null || response.getBody().getItems() == null) {
            log.warn("페이지 {} 응답이 비어있음", pageNo);
            return 0;
        }

        List<DrugIngredientResponse.Item> items = response.getBody().getItems();
        int count = 0;

        for (DrugIngredientResponse.Item item : items) {
            if (item.getItemSeq() == null || item.getMtralCode() == null) continue;

            Drug drug = drugRepository.findByItemSeq(item.getItemSeq()).orElse(null);
            if (drug == null) continue; // drugs 테이블에 없는 약품은 스킵

            if (drugIngredientRepository.existsByDrugIdAndMtralCode(drug.getId(), item.getMtralCode())) {
                continue; // 이미 저장된 성분은 스킵 (중복 방지)
            }

            DrugIngredient ingredient = new DrugIngredient();
            ingredient.setDrug(drug);
            ingredient.setMtralCode(item.getMtralCode());
            ingredient.setMtralNm(item.getMtralNm());
            ingredient.setQnt(item.getQnt());
            ingredient.setIngdUnitCd(item.getIngdUnitCd());
            ingredient.setMainIngrEng(item.getMainIngrEng());

            drugIngredientRepository.save(ingredient);

            // drugs.main_ingr_eng에 drug당 1건만 저장 (중복 제거)
            if ((drug.getMainIngrEng() == null || drug.getMainIngrEng().isBlank())
                    && item.getMainIngrEng() != null && !item.getMainIngrEng().isBlank()) {
                drug.setMainIngrEng(item.getMainIngrEng());
                drugRepository.save(drug);
            }
            count++;
        }
        return count;
    }
}
