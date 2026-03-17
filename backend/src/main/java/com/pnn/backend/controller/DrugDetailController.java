package com.pnn.backend.controller;

import com.pnn.backend.dto.DrugDetailResponseDto;
import com.pnn.backend.service.DrugDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프레젠테이션 계층 (Controller)
 * 약품 상세 정보 조회를 전담하는 컨트롤러입니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/drugs")
@RequiredArgsConstructor
@Tag(name = "Drug Detail API", description = "단일 약품의 상세 정보(허가정보, DUR 등)를 조회하는 API")
public class DrugDetailController {

    private final DrugDetailService drugDetailService;

    /**
     * 약품 상세 정보 조회 API
     * 
     * [URL 경로에 중괄호 { }를 쓰는 이유 (Path Variable 기법)]
     * - RESTful API에서는 특정 아이템 하나를 가리킬 때 명사 뒤에 ID를 바로 붙이는 방식을 권장합니다. (예: /users/1,
     * /items/99)
     * - `@GetMapping("/{drugId}")` 라고 쓰면, 사용자가 `GET /api/drugs/1234` 라고 요청했을 때 스프링
     * 부트가 맨 뒤의 숫자 '1234'를 동적으로 낚아챕니다.
     * - 낚아챈 숫자는 아래 메서드의 파라미터인 `@PathVariable("drugId") Long drugId` 에 자동으로
     * 주입(연결)됩니다.
     * 
     * @param drugId URL 경로에서 추출할 약품의 고유 식별자 (drugs_master 테이블의 pk)
     * @return 약품의 상단정보, 약품정보, 복약정보, 허가정보, DUR(병용/임부금기) 정보를 묶은 통합 DTO
     */
    @Operation(summary = "특정 약품의 상세 정보 통합 조회", description = "목록에서 선택한 약품의 ID를 입력하면 허가, 복약, 심평원 코드 및 DUR 경고 정보를 모두 묶어서 반환합니다.")
    @GetMapping("/{drugId}")
    public ResponseEntity<DrugDetailResponseDto> getDrugDetail(
            @Parameter(description = "약품의 고유 식별자 (ID)", required = true, example = "1") @PathVariable("drugId") Long drugId) {

        log.info("약품 상세 정보 조회 API 호출 - drugId: {}", drugId);

        // DrugDetailService를 호출하여 결과를 받아옵니다.
        DrugDetailResponseDto response = drugDetailService.getDrugDetail(drugId);

        return ResponseEntity.ok(response);
    }
}
