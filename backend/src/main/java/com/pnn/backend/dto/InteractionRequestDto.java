package com.pnn.backend.dto;

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

    // 검사할 대상 처방약의 고유 ID (drugs 테이블의 id)
    private Long drugId;

    // 복용 중인 영양제 목록
    private List<SupplementInputDto> supplements;
}
