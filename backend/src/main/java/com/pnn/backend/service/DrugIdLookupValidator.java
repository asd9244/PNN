package com.pnn.backend.service;

import com.pnn.backend.domain.DrugsMaster;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * drugs_master 조회 결과가 요청한 ID 목록과 일치하는지 검증한다.
 */
public final class DrugIdLookupValidator {

    private DrugIdLookupValidator() {
    }

    /**
     * 요청에 포함된 drugId 중 DB에 없는 것이 있으면 IllegalArgumentException.
     * (요청에 동일 ID가 여러 번 있어도 distinct 기준으로 판단)
     */
    public static void assertAllRequestedIdsExist(List<Long> requestedDrugIds, List<DrugsMaster> found) {
        Set<Long> foundIds = found.stream()
                .map(DrugsMaster::getId)
                .collect(Collectors.toSet());
        List<Long> missing = requestedDrugIds.stream()
                .distinct()
                .filter(id -> !foundIds.contains(id))
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 drugId: " + missing);
        }
    }
}
