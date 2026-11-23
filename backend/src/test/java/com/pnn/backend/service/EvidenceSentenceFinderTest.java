package com.pnn.backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EvidenceSentenceFinderTest {

    // 씬지로이드 설명서 상호작용 항목을 줄인 것 (11번이 PDF 추출 중 두 번 나온 것까지 재현)
    private static final String SECTION = "6. 상호작용 "
            + "1) 쿠마린계 항응고제와 병용투여시 비타민K의존성 응고인자의 이화를 촉진한다. "
            + "5) 콜레스티라민, 철분제제, 알루미늄 함유 제산제와 병용투여시 이 약의 흡수가 지연 또는 감소될 수 있다. "
            + "11) 살리실산 등은 이 약의 혈장 함유율을 증가시킬 수 있다. "
            + "11) 살리실산 등은 이 약의 혈장 함유율을 증가시킬 수 있다. "
            + "12) 칼슘제제, 세벨라머 등과 병용투여시 투여간격에 주의한다.(최소 4시간 간격) "
            + "15) 세인트존스워트가 포함된 제품은 이 약의 혈청 농도를 감소시킬 수 있다. 이러한 제품을 동시 투여 시 용량을 늘려야 할 수 있다.";

    @Test
    @DisplayName("번호 문장 단위로 나누고, 제목은 빼고, 두 번 나온 문장은 하나만 남긴다")
    void splitsItems() {
        List<String> items = EvidenceSentenceFinder.splitItems(SECTION);

        assertThat(items).hasSize(5); // 1), 5), 11), 12), 15) — 중복된 11)은 하나
        assertThat(items.get(0)).startsWith("1) 쿠마린계"); // "6. 상호작용" 제목은 빠짐
    }

    @Test
    @DisplayName("성분이 나온 번호 문장만 고른다 — 철분은 5번, 칼슘은 12번")
    void findsOnlyMentionedItems() {
        assertThat(EvidenceSentenceFinder.findSentences(SECTION, NutrientGroup.IRON))
                .containsExactly("5) 콜레스티라민, 철분제제, 알루미늄 함유 제산제와 병용투여시 이 약의 흡수가 지연 또는 감소될 수 있다.");
        assertThat(EvidenceSentenceFinder.findSentences(SECTION, NutrientGroup.CALCIUM))
                .singleElement().asString().startsWith("12) 칼슘제제");
    }

    @Test
    @DisplayName("뒤따르는 조치 문장도 같은 번호 안이면 함께 가져온다")
    void keepsFollowUpSentenceInSameItem() {
        assertThat(EvidenceSentenceFinder.findSentences(SECTION, NutrientGroup.ST_JOHNS_WORT))
                .singleElement().asString().contains("용량을 늘려야 할 수 있다");
    }

    @Test
    @DisplayName("언급이 없거나 함정 단어뿐이면 빈 목록 — 마그네슘 없음, 비타민K의존성은 영양제 아님")
    void returnsEmptyWhenNotMentioned() {
        assertThat(EvidenceSentenceFinder.findSentences(SECTION, NutrientGroup.MAGNESIUM)).isEmpty();
        assertThat(EvidenceSentenceFinder.findSentences(SECTION, NutrientGroup.VITAMIN_K)).isEmpty();
    }

    @Test
    @DisplayName("① ② 처럼 안쪽 번호가 있으면 그 단위까지 나눈다")
    void splitsCircledSubItems() {
        String section = "5. 상호작용 1) 발사르탄 ① 이중차단은 저혈압 위험을 높인다. ② 칼륨: 칼륨보조제, 칼륨을 함유한 식염대용물과 병용 시 혈청 칼륨이 증가할 수 있다.";

        assertThat(EvidenceSentenceFinder.findSentences(section, NutrientGroup.POTASSIUM))
                .singleElement().asString().startsWith("② 칼륨: 칼륨보조제");
    }

    @Test
    @DisplayName("번호 없이 긴 문단이면 성분이 나온 문장만 잘라 낸다")
    void cutsLongParagraphToSentence() {
        String filler = "병용 투여하는 다른 약물들은 이 약의 노출을 변화시킬 수 있음을 주의해야 한다. ".repeat(10);
        String section = "5. 상호작용 " + filler + "칼슘 또는 철을 함유한 보충제와 함께 투여할 경우 공복 시에는 2시간 전에 복용한다. " + filler;

        assertThat(EvidenceSentenceFinder.findSentences(section, NutrientGroup.CALCIUM))
                .containsExactly("칼슘 또는 철을 함유한 보충제와 함께 투여할 경우 공복 시에는 2시간 전에 복용한다.");
    }

    @Test
    @DisplayName("문장 끝이 없는 긴 글(설명서의 표가 풀린 글)은 언급 앞뒤만 남기고 '…'를 붙인다")
    void trimsTableLikeTextAroundMention() {
        String row = "병용한 약물 요법 이 약 요법 AUC 변화 "; // 표 한 줄처럼 "…다."가 없는 글
        String section = "5. 상호작용 " + row.repeat(20) + "제산제(수산화알루미늄마그네슘) 동시 투여 54% 감소 " + row.repeat(20);

        String evidence = EvidenceSentenceFinder.findSentences(section, NutrientGroup.MAGNESIUM).get(0);

        assertThat(evidence).startsWith("…").endsWith("…").contains("마그네슘");
        assertThat(evidence.length()).isLessThanOrEqualTo(402); // 앞 150자 + 뒤 250자 + "…" 2개
    }
}
