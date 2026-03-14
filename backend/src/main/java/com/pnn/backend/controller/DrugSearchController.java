package com.pnn.backend.controller;

import com.pnn.backend.dto.DrugSearchRequestDto;
import com.pnn.backend.dto.DrugSearchResponseDto;
import com.pnn.backend.dto.PillIdentifyRequestDto;
import com.pnn.backend.service.DrugSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프레젠테이션 계층 (Controller)
 * 
 * 클라이언트(웹/앱)의 HTTP 요청을 받아 적절한 Service로 넘겨주고,
 * 처리된 결과를 다시 JSON 형태로 클라이언트에게 응답(Response)하는 역할을 합니다.
 */
@Slf4j // 로깅 기능을 사용하기 위한 어노테이션 (log.info 등)
@RestController // 이 클래스가 REST API를 처리하는 컨트롤러임을 스프링에 알림
@RequestMapping("/api/drugs/search") // 이 컨트롤러의 모든 API는 공통적으로 이 주소로 시작함
@RequiredArgsConstructor // final이 붙은 필드(의존성)를 가지고 자동으로 생성자를 만들어줌 (의존성 주입)
public class DrugSearchController {

    // 우리가 만든 비즈니스 로직(Service)을 호출하기 위해 가져옵니다.
    private final DrugSearchService drugSearchService;

    /**
     * 약품 상세 검색 API
     * 
     * 요청 예시: GET /api/drugs/search/detail?itemName=타이레놀&page=0&size=20
     * 
     * @param request 클라이언트가 보낸 검색 조건 (이름, 제조사, 성분명). 
     *                @ModelAttribute는 URL 쿼리 파라미터(?key=value)를 DTO 객체에 쏙쏙 매핑해줍니다.
     * @param pageable 페이징 정보 (page, size, sort). 
     *                 @PageableDefault를 쓰면 클라이언트가 페이지 번호를 안 보내도 기본값(0페이지, 20개씩)을 설정해줍니다.
     * @return 검색된 약품 목록 (Page 객체로 감싸서 반환하여 총 갯수 등의 메타데이터 포함)
     */
    @GetMapping("/detail")
    public ResponseEntity<Page<DrugSearchResponseDto>> searchDrugDetail(
            @ModelAttribute DrugSearchRequestDto request,
            @PageableDefault(page = 0, size = 20) Pageable pageable) {

        // 로깅: 서버에 어떤 요청이 들어왔는지 기록 (문제 발생 시 추적 용이)
        log.info("약품 상세 검색 API 호출");

        // Service 계층으로 비즈니스 로직(검증 및 DB 조회) 처리를 위임(떠넘김)
        Page<DrugSearchResponseDto> response = drugSearchService.searchDrugDetail(request, pageable);

        // 처리된 결과를 HTTP 상태코드 200(OK)과 함께 클라이언트에게 반환
        return ResponseEntity.ok(response);
    }

    /**
     * 낱알 식별 검색 API
     * 
     * 요청 예시: GET /api/drugs/search/pillIdentifier?color=하양&drugShape=원형
     * 
     * @param request 클라이언트가 보낸 낱알 식별 조건 (모양, 색상, 각인 등)
     * @param pageable 페이징 정보
     * @return 식별 조건에 맞는 약품 목록
     */
    @GetMapping("/pillIdentifier")
    public ResponseEntity<Page<DrugSearchResponseDto>> searchPillIdentifier(
            @ModelAttribute PillIdentifyRequestDto request,
            @PageableDefault(page = 0, size = 20) Pageable pageable) {

        log.info("낱알 식별 검색 API 호출");

        // Service 계층으로 비즈니스 로직(검증 및 DB 조회) 처리를 위임
        Page<DrugSearchResponseDto> response = drugSearchService.searchPillIdentifier(request, pageable);

        return ResponseEntity.ok(response);
    }
}
