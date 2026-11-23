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
    @DisplayName("[고침 B] 성분 가까이에 '관찰되지 않았다'가 있는데 주의로 읽으면 원문만 + 안내 — 넬슨라베프라졸 문장")
    void cautionNextToNoEffectCueBecomesSentenceOnly() {
        String sentence = "5) 임상시험에서 제산제와 이 약 병용시 액제형 제산제와 상호작용이 관찰되지 않았다. 수산화알루미늄겔, 수산화마그네슘 함유 "
                + "제산제와 이 약을 동시에 병용 투여한 경우와 제산제 투여 1시 간 후에 복용한 경우 평균 혈중농도곡선하면적이 각각 8 %, 6 % 감소하였다.";
        givenDrugExists(1L);
        givenEvidence(1L, nutrient("마그네슘", "마그네슘", InteractionEvidenceResponseDto.Status.FOUND, sentence));
        when(aiServerClient.readEvidence(any())).thenReturn(aiAnswer(
                result(1, "마그네슘", "CAUTION", "수산화마그네슘 함유 제산제와 이 약을 동시에 병용 투여한 경우", "약 효과가 떨어질 수 있어요.", "제산제 투여 1시간 후에 복용")));

        var finding = service.check(request(1L, "마그네슘")).getDrugs().get(0).getNutrients().get(0).getFindings().get(0);

        assertThat(finding.getKind()).isEqualTo(Kind.UNVERIFIED); // 틀린 할 일("1시간 후")이 나가지 않음
        assertThat(finding.getAction()).isNull();
        assertThat(finding.getNote()).isEqualTo(EvidenceCheckService.NOTE_MIXED);
    }

    @Test
    @DisplayName("[고침 B] 신호 말이 표의 다른 줄(성분에서 멀리)에 있으면 주의를 그대로 둔다 — 로수바스타틴 표")
    void cueFarAwayDoesNotBlockCaution() {
        String sentence = "에리스로마이신 500 mg 1일 4회, 7일간 80 mg, 단회 투여 유의한 차이 없음 바이칼린 50 mg 1일 3회, 14일간 20 mg, 단회 투여 "
                + "47% 감소 기타 약물의 영향 - 제산제 : 수산화알루미늄, 수산화마그네슘을 함유하는 제산제와 로수바스타틴을 병용 투여한 결과, 로수바스타틴의 혈장 농도가 54% 감소하였다.";
        givenDrugExists(1L);
        givenEvidence(1L, nutrient("마그네슘", "마그네슘", InteractionEvidenceResponseDto.Status.FOUND, sentence));
        when(aiServerClient.readEvidence(any())).thenReturn(aiAnswer(
                result(1, "마그네슘", "CAUTION", "제산제와 로수바스타틴을 병용 투여한 결과", "마그네슘 제산제는 약 농도를 낮춰요.", "")));

        var magnesium = service.check(request(1L, "마그네슘")).getDrugs().get(0).getNutrients().get(0);

        assertThat(magnesium.getStatus()).isEqualTo(Status.CAUTION);
    }

    @Test
    @DisplayName("[고침 B] 원문에 '영향 없음' 신호가 없는데 영향 없음으로 읽으면 원문만 + 안내 (근거 없는 '괜찮다' 차단)")
    void noEffectWithoutCueBecomesSentenceOnly() {
        givenDrugExists(1L);
        givenEvidence(1L, nutrient("칼슘", "칼슘", InteractionEvidenceResponseDto.Status.FOUND, IRON_CALCIUM));
        when(aiServerClient.readEvidence(any())).thenReturn(aiAnswer(
                result(1, "칼슘", "NO_EFFECT", "칼슘 함유 제제와의 병용에 의해", "칼슘은 같이 먹어도 돼요.", "")));

        var finding = service.check(request(1L, "칼슘")).getDrugs().get(0).getNutrients().get(0).getFindings().get(0);

        assertThat(finding.getKind()).isEqualTo(Kind.UNVERIFIED);
        assertThat(finding.getNote()).isEqualTo(EvidenceCheckService.NOTE_NO_EFFECT_UNCONFIRMED);
    }

    @Test
    @DisplayName("[고침 B·C] 원문에 '안전하였다'가 있으면 영향 없음과 '안전' 표현을 그대로 둔다 — 브이포지알 문장")
    void noEffectWithCueAndStrongWordInSentenceIsKept() {
        String sentence = "④ 기타 : 암로디핀 단독투여시, 디곡 신, 와파린, 알루미늄/마그네슘(제산제), 시메티딘, 비스테로이드성 소염제, 사이클로스포린과 병용시 안전하였다.";
        givenDrugExists(1L);
        givenEvidence(1L, nutrient("마그네슘", "마그네슘", InteractionEvidenceResponseDto.Status.FOUND, sentence));
        when(aiServerClient.readEvidence(any())).thenReturn(aiAnswer(
                result(1, "마그네슘", "NO_EFFECT", "사이클로스포린과 병용시 안전하였다", "함께 복용해도 안전했다고 적혀 있습니다.", "")));

        var magnesium = service.check(request(1L, "마그네슘")).getDrugs().get(0).getNutrients().get(0);

        assertThat(magnesium.getStatus()).isEqualTo(Status.NO_EFFECT);
        assertThat(magnesium.getFindings().get(0).getNote()).isNull();
    }

    @Test
    @DisplayName("[고침 C] 원문에 없는 강한 표현('크게')이 설명에 있으면 원문만 보여 준다 — 비엘아토르 사례")
    void strongWordNotInSentenceIsDropped() {
        givenDrugExists(1L);
        givenEvidence(1L, nutrient("철", "철분", InteractionEvidenceResponseDto.Status.FOUND, IRON_CALCIUM));
        when(aiServerClient.readEvidence(any())).thenReturn(aiAnswer(
                result(1, "철분", "CAUTION", "흡수가 저하될 수 있으므로", "철분제는 약 흡수를 크게 줄여요.", "")));

        var iron = service.check(request(1L, "철")).getDrugs().get(0).getNutrients().get(0);

        assertThat(iron.getStatus()).isEqualTo(Status.UNVERIFIED);
        assertThat(iron.getFindings().get(0).getExplanation()).isNull();
    }

    @Test
    @DisplayName("[고침 B] PDF 줄바꿈으로 깨진 '영향 이 없었다'도 신호 말로 찾는다 — 비엘아토르 문장")
    void findsCueWithBrokenSpacing() {
        String sentence = "11) 제산제 : 이 약과 수산화마그네슘 그리고 수산화알루미늄을 함유하는 경구용 제산제(antacid) 현탁액과 병용투여시 "
                + "아토르바스타틴 혈장농도가 약 35 % 정도 감소하였다. 그러나 LDL-콜레스테롤의 감소에는 영향 이 없었다.";

        assertThat(EvidenceCheckService.noEffectCueNear(sentence, "마그네슘")).isTrue();
        assertThat(EvidenceCheckService.noEffectCueNear(IRON_CALCIUM, "철분")).isFalse();
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
