package com.pnn.backend.batch.client;

import com.pnn.backend.batch.dto.DrugEasyInfoResponse;
import com.pnn.backend.batch.dto.DrugIdentificationResponse;
import com.pnn.backend.batch.dto.DrugIngredientResponse;
import com.pnn.backend.batch.dto.DrugPermissionDetailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Slf4j // 로깅
@Component // Spring Bean 등록
public class PublicDataClient {

    private final RestClient restClient; // Spring 6.1+ HTTP 클라이언트

    @Value("${api.public.drug.key}")
    private String serviceKey; // 공공데이터 API 키 (Decoded)

    @Value("${api.public.drug-identification.url}")
    private String drugIdUrl; // 낱알식별 API URL

    @Value("${api.public.drug-permission-detail.url}")
    private String drugPermissionDetailUrl; // 허가 상세정보 API URL

    @Value("${api.public.drug-ingredient.url}")
    private String drugIngredientUrl; // 주성분 상세정보 API URL

    @Value("${api.public.drug-easy.url}")
    private String drugEasyUrl; // e약은요 API URL

    public PublicDataClient() {
        this.restClient = RestClient.builder().build();
    }

    public DrugIdentificationResponse fetchDrugIdentification(int pageNo, int numOfRows) { // 낱알식별 정보 조회
        URI uri = UriComponentsBuilder.fromHttpUrl(drugIdUrl)
                .queryParam("serviceKey", serviceKey)
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", numOfRows)
                .queryParam("type", "json")
                .build()
                .toUri();

        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(DrugIdentificationResponse.class);
    }

    public DrugPermissionDetailResponse fetchDrugPermissionDetail(int pageNo, int numOfRows) { // 허가 상세정보 조회
        URI uri = UriComponentsBuilder.fromHttpUrl(drugPermissionDetailUrl)
                .queryParam("serviceKey", serviceKey)
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", numOfRows)
                .queryParam("type", "json")
                .build()
                .toUri();

        try {
            return restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(DrugPermissionDetailResponse.class);
        } catch (Exception e) {
            log.error("허가 상세정보 API 호출 실패 (page={}): {}", pageNo, e.getMessage());
            return null;
        }
    }

    public DrugIngredientResponse fetchDrugIngredient(int pageNo, int numOfRows) { // 주성분 상세정보 조회
        URI uri = UriComponentsBuilder.fromHttpUrl(drugIngredientUrl)
                .queryParam("serviceKey", serviceKey)
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", numOfRows)
                .queryParam("type", "json")
                .build()
                .toUri();

        try {
            return restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(DrugIngredientResponse.class);
        } catch (Exception e) {
            log.error("주성분 API 호출 실패 (page={}): {}", pageNo, e.getMessage());
            return null;
        }
    }

    public DrugEasyInfoResponse fetchDrugEasyInfo(int pageNo, int numOfRows) { // e약은요 정보 조회
        URI uri = UriComponentsBuilder.fromHttpUrl(drugEasyUrl)
                .queryParam("serviceKey", serviceKey)
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", numOfRows)
                .queryParam("type", "json")
                .build()
                .toUri();

        try {
            return restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(DrugEasyInfoResponse.class);
        } catch (Exception e) {
            log.error("e약은요 API 호출 실패 (page={}): {}", pageNo, e.getMessage());
            return null;
        }
    }
}
