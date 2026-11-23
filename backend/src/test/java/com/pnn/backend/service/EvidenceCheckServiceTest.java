package com.pnn.backend.service;

import com.pnn.backend.client.AiServerClient;
import com.pnn.backend.client.dto.AiEvidenceReadRequest;
import com.pnn.backend.client.dto.AiEvidenceReadResponse;
import com.pnn.backend.domain.DrugsMaster;
import com.pnn.backend.dto.EvidenceCheckResponseDto.Kind;
import com.pnn.backend.dto.EvidenceCheckResponseDto.NutrientResult;
import com.pnn.backend.dto.EvidenceCheckResponseDto.Status;
import com.pnn.backend.dto.InteractionCheckRequestDto;
import com.pnn.backend.dto.InteractionEvidenceResponseDto;
import com.pnn.backend.dto.InteractionEvidenceResponseDto.NutrientEvidence;
import com.pnn.backend.repository.DrugsMasterRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvidenceCheckServiceTest {

    private static final String IRON_CALCIUM = "3) 철분 함유 제제, 칼슘 함유 제제와의 병용에 의해 이 약의 흡수가 저하될 수 있으므 로 2시간 이상 간격을 둔다.";

    @Mock
    private DrugsMasterRepository drugsMasterRepository; // 가짜 DB
    @Mock
    private InteractionEvidenceService interactionEvidenceService; // 가짜 근거 찾기
    @Mock
    private AiServerClient aiServerClient; // 가짜 AI 서버
    @InjectMocks
    private EvidenceCheckService service;

    @Test
    @DisplayName("근거 문장이 하나도 없으면 Gemini를 부르지 않고 '언급 없음/대상 아님'을 그대로 돌려준다")
    void skipsAiWhenNoEvidence() {
        givenDrugExists(1L);
        givenEvidence(1L, nutrient("마그네슘", "마그네슘", InteractionEvidenceResponseDto.Status.NOT_MENTIONED),
                nutrient("나트륨", null, InteractionEvidenceResponseDto.Status.NOT_IN_TABLE));

        var drug = service.check(request(1L, "마그네슘", "나트륨")).getDrugs().get(0);

        verify(aiServerClient, never()).readEvidence(any());
        assertThat(drug.isAiCalled()).isFalse();
        assertThat(drug.getNutrients()).extracting(NutrientResult::getStatus)
                .containsExactly(Status.NOT_MENTIONED, Status.NOT_IN_TABLE);
    }

    @Test
    @DisplayName("여러 성분이 같은 문장을 가리키면 한 번만 보내고, 원문과 맞는 답에는 쉬운 설명이 붙는다")
    void sendsSharedSentenceOnceAndKeepsVerifiedAnswer() {
        givenDrugExists(1L);
        givenEvidence(1L, nutrient("철", "철분", InteractionEvidenceResponseDto.Status.FOUND, IRON_CALCIUM),
                nutrient("칼슘", "칼슘", InteractionEvidenceResponseDto.Status.FOUND, IRON_CALCIUM));
        when(aiServerClient.readEvidence(any())).thenReturn(aiAnswer(
                result(1, "철분", "CAUTION", "흡수가 저하될 수 있으므로 2시간 이상 간격", "철분제와 같이 먹으면 약 흡수가 줄 수 있어요.", "2시간 이상 간격"),
                result(1, "칼슘", "CAUTION", "칼슘 함유 제제와의 병용에 의해", "칼슘제도 약 흡수를 줄일 수 있어요.", "")));

        var drug = service.check(request(1L, "철", "칼슘")).getDrugs().get(0);

        ArgumentCaptor<AiEvidenceReadRequest> sent = ArgumentCaptor.forClass(AiEvidenceReadRequest.class);
        verify(aiServerClient).readEvidence(sent.capture());
        assertThat(sent.getValue().getItems()).hasSize(1); // 같은 문장은 한 번만
        assertThat(sent.getValue().getItems().get(0).getNutrients()).containsExactly("철분", "칼슘");

        var iron = drug.getNutrients().get(0);
        assertThat(iron.getStatus()).isEqualTo(Status.CAUTION);
        assertThat(iron.getFindings().get(0).getExplanation()).contains("흡수");
        assertThat(iron.getFindings().get(0).getAction()).isEqualTo("2시간 이상 간격"); // 원문의 "있으므 로" 띄어쓰기 차이는 무시하고 대조
        assertThat(drug.getNutrients().get(1).getFindings().get(0).getAction()).isNull(); // 할 일이 없으면 null
    }

    @Test
    @DisplayName("Gemini가 낸 근거 구절이 원문에 없으면 그 답은 버리고 원문만 보여 준다")
    void dropsAnswerWhoseQuoteIsNotInSentence() {
        givenDrugExists(1L);
        givenEvidence(1L, nutrient("철", "철분", InteractionEvidenceResponseDto.Status.FOUND, IRON_CALCIUM));
        when(aiServerClient.readEvidence(any())).thenReturn(aiAnswer(
                result(1, "철분", "NO_EFFECT", "철분은 이 약과 상호작용이 없다", "같이 먹어도 괜찮아요.", "")));

        var iron = service.check(request(1L, "철")).getDrugs().get(0).getNutrients().get(0);

        assertThat(iron.getStatus()).isEqualTo(Status.UNVERIFIED);
        assertThat(iron.getFindings().get(0).getExplanation()).isNull(); // 지어낸 설명은 나가지 않음
        assertThat(iron.getFindings().get(0).getSentence()).isEqualTo(IRON_CALCIUM); // 원문은 그대로
    }

    @Test
    @DisplayName("AI 서버가 응답하지 않아도 오류 없이 원문만 돌려준다")
    void returnsSentencesWhenAiFails() {
        givenDrugExists(1L);
        givenEvidence(1L, nutrient("철", "철분", InteractionEvidenceResponseDto.Status.FOUND, IRON_CALCIUM));
        when(aiServerClient.readEvidence(any())).thenThrow(new ResourceAccessException("AI 서버 꺼짐"));

        var iron = service.check(request(1L, "철")).getDrugs().get(0).getNutrients().get(0);

        assertThat(iron.getStatus()).isEqualTo(Status.UNVERIFIED);
        assertThat(iron.getFindings()).extracting(f -> f.getKind()).containsExactly(Kind.UNVERIFIED);
    }

    @Test
    @DisplayName("한 성분에 문장이 여럿이면 가장 주의가 필요한 종류가 최종 상태 — 영향 없음 + 주의 → 주의")
    void worstKindWins() {
        String noEffect = "4) 식사의 일부로 섭취하는 식이성 칼슘은 흡수에 유의하게 영향을 주지 않는다.";
        givenDrugExists(1L);
        givenEvidence(1L, nutrient("칼슘", "칼슘", InteractionEvidenceResponseDto.Status.FOUND, noEffect, IRON_CALCIUM));
        when(aiServerClient.readEvidence(any())).thenReturn(aiAnswer(
                result(1, "칼슘", "NO_EFFECT", "식이성 칼슘은 흡수에 유의하게 영향을 주지 않는다", "음식 속 칼슘은 괜찮아요.", ""),
                result(2, "칼슘", "CAUTION", "칼슘 함유 제제와의 병용에 의해", "칼슘제는 흡수를 줄여요.", "2시간 이상 간격")));

        var calcium = service.check(request(1L, "칼슘")).getDrugs().get(0).getNutrients().get(0);

        assertThat(calcium.getStatus()).isEqualTo(Status.CAUTION);
        assertThat(calcium.getFindings()).extracting(f -> f.getKind()).containsExactly(Kind.NO_EFFECT, Kind.CAUTION);
    }

    @Test
    @DisplayName("할 일에 원문에 없는 숫자가 나오면(2시간 → 4시간) 그 답은 버리고 원문만 보여 준다")
    void dropsAnswerWithNumberNotInSentence() {
        givenDrugExists(1L);
        givenEvidence(1L, nutrient("철", "철분", InteractionEvidenceResponseDto.Status.FOUND, IRON_CALCIUM));
        when(aiServerClient.readEvidence(any())).thenReturn(aiAnswer(
                result(1, "철분", "CAUTION", "흡수가 저하될 수 있으므로", "철분제는 흡수를 줄여요.", "4시간 이상 간격")));

        var iron = service.check(request(1L, "철")).getDrugs().get(0).getNutrients().get(0);

        assertThat(iron.getStatus()).isEqualTo(Status.UNVERIFIED);
        assertThat(iron.getFindings().get(0).getAction()).isNull();
    }

    @Test
    @DisplayName("근거 구절 대조: 띄어쓰기 차이는 무시하고, 너무 짧거나 원문에 없는 구절은 거른다")
    void quoteMatching() {
        assertThat(EvidenceCheckService.quoteAppearsIn("저하될 수 있으므로", IRON_CALCIUM)).isTrue();
        assertThat(EvidenceCheckService.quoteAppearsIn("철분", IRON_CALCIUM)).isFalse(); // 4자 미만
        assertThat(EvidenceCheckService.quoteAppearsIn("흡수가 증가한다", IRON_CALCIUM)).isFalse();
    }

    // ---- 테스트 준비용 ----

    private void givenDrugExists(Long id) {
        DrugsMaster drug = new DrugsMaster();
        drug.setId(id);
        when(drugsMasterRepository.findAllById(anyList())).thenReturn(List.of(drug));
    }

    private void givenEvidence(Long drugId, NutrientEvidence... nutrients) {
        when(interactionEvidenceService.findEvidence(eq(drugId), anyList())).thenReturn(InteractionEvidenceResponseDto.builder()
                .drugId(drugId).drugName("테스트약").sectionFound(true).nutrients(List.of(nutrients)).build());
    }

    private static NutrientEvidence nutrient(String name, String group, InteractionEvidenceResponseDto.Status status, String... sentences) {
        return NutrientEvidence.builder().nutrient(name).matchedGroup(group).status(status).sentences(Arrays.asList(sentences)).build();
    }

    private static InteractionCheckRequestDto request(Long drugId, String... nutrientNames) {
        InteractionCheckRequestDto request = new InteractionCheckRequestDto();
        request.setDrugIds(List.of(drugId));
        InteractionCheckRequestDto.SupplementInput supplement = new InteractionCheckRequestDto.SupplementInput();
        supplement.setName("종합영양제");
        supplement.setNutrients(Arrays.stream(nutrientNames).map(n -> {
            InteractionCheckRequestDto.NutrientInput input = new InteractionCheckRequestDto.NutrientInput();
            input.setName(n);
            return input;
        }).toList());
        request.setSupplements(List.of(supplement));
        return request;
    }

    private static AiEvidenceReadResponse aiAnswer(AiEvidenceReadResponse.Result... results) {
        AiEvidenceReadResponse response = new AiEvidenceReadResponse();
        response.setResults(List.of(results));
        return response;
    }

    private static AiEvidenceReadResponse.Result result(int id, String nutrient, String kind, String quote, String explanation, String action) {
        AiEvidenceReadResponse.Result result = new AiEvidenceReadResponse.Result();
        result.setId(id);
        result.setNutrient(nutrient);
        result.setKind(kind);
        result.setQuote(quote);
        result.setExplanation(explanation);
        result.setAction(action);
        return result;
    }
}
