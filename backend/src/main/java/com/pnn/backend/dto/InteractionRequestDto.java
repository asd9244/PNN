package com.pnn.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 클라이언트가 상호작용 검사(Case A)를 요청할 때 보내는 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class InteractionRequestDto {

    @NotNull(message = "drugId는 필수입니다")
    @Positive(message = "drugId는 양수여야 합니다")
    private Long drugId; // drugs 테이블의 id

    @NotNull(message = "supplements는 필수입니다")
    @NotEmpty(message = "supplements는 비어 있을 수 없습니다")
    @Valid
    private List<SupplementInputDto> supplements;
}
