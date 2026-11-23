package com.pnn.backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class NutrientGroupTest {

    @ParameterizedTest(name = "\"{0}\" → {1}")
    @DisplayName("성분표에 적힌 이름(한글·영어·괄호·숫자 포함)을 성분 묶음으로 바꾼다")
    @CsvSource(delimiter = '|', value = {
            "철 | IRON",
            "철(푸마르산제일철) | IRON",
            "Iron | IRON",
            "칼슘 | CALCIUM",
            "비타민 D3 | VITAMIN_D",
            "Vitamin B12 | VITAMIN_B12",
            "비타민K2(메나퀴논-7) | VITAMIN_K",
            "EPA 및 DHA 함유 유지 | OMEGA3",
            "St. John's Wort | ST_JOHNS_WORT"
    })
    void mapsNutrientNames(String name, NutrientGroup expected) {
        assertThat(NutrientGroup.fromNutrientName(name)).contains(expected);
    }

    @ParameterizedTest(name = "\"{0}\" → 대응표에 없음")
    @DisplayName("대응표에 없는 성분은 빈 값")
    @NullAndEmptySource
    @ValueSource(strings = {"나트륨", "탄수화물", "판토텐산", "비타민B1", "비타민"})
    void returnsEmptyForUnknown(String name) {
        assertThat(NutrientGroup.fromNutrientName(name)).isEmpty();
    }

    @Test
    @DisplayName("씬지로이드 설명서 문장에서 철분은 찾고, 없는 칼슘은 찾지 않는다")
    void findsMentionInLabelSentence() {
        String sentence = "5) 콜레스티라민, 철분제제, 알루미늄 함유 제산제와 병용투여시 이 약의 흡수가 지연 또는 감소될 수 있다.";

        assertThat(NutrientGroup.IRON.isMentionedIn(sentence)).isTrue();
        assertThat(NutrientGroup.CALCIUM.isMentionedIn(sentence)).isFalse();
    }

    @Test
    @DisplayName("약 계열·병명·검사 이름 속 같은 글자는 영양제로 세지 않는다")
    void ignoresLookAlikeWords() {
        assertThat(NutrientGroup.CALCIUM.isMentionedIn("칼슘채널길항제와 병용 시 혈압이 떨어질 수 있다.")).isFalse();
        assertThat(NutrientGroup.VITAMIN_K.isMentionedIn("비타민K의존성 응고인자의 이화를 촉진하여")).isFalse();
        assertThat(NutrientGroup.POTASSIUM.isMentionedIn("칼륨보존이뇨제와 병용시 혈청 중 칼륨 농도가 증가할 수 있다.")).isFalse();
        assertThat(NutrientGroup.OMEGA3.isMentionedIn("유산산증의 위험을 증가시킬 수있어유산산증")).isFalse();
    }

    @Test
    @DisplayName("피 속 칼슘 수치 이야기(혈중·혈청 칼슘, 칼슘치)는 칼슘 영양제로 세지 않는다 — 베네프 설명서 문장")
    void ignoresBloodCalciumLevel() {
        assertThat(NutrientGroup.CALCIUM.isMentionedIn("부신피질 호르몬 - 덱사메타손 등 혈청 칼슘치가 저하할 우려가 있다.")).isFalse();
        assertThat(NutrientGroup.CALCIUM.isMentionedIn("이약의 혈중 칼슘 저하 작용이 증강 될 가능성이 있다.")).isFalse();
        assertThat(NutrientGroup.CALCIUM.isMentionedIn("칼슘 보충제와 병용 시 혈중 칼슘 농도가 증가할 수 있다.")).isTrue(); // 보충제 이야기가 같이 있으면 찾음
    }

    @Test
    @DisplayName("같은 문장에 진짜 영양제 표현이 있으면 찾는다")
    void findsRealSupplementNextToLookAlike() {
        assertThat(NutrientGroup.POTASSIUM.isMentionedIn("칼륨보조제, 칼륨보존이뇨제, 칼륨을 함유한 식염대용물과 병용 시")).isTrue();
        assertThat(NutrientGroup.CALCIUM.isMentionedIn("칼슘제제, 세벨라머 등과 병용투여시 최소 4시간 간격")).isTrue();
        assertThat(NutrientGroup.IRON.isMentionedIn("칼슘 또는 철을 함유한 보충제와 함께 투여할 경우")).isTrue();
        assertThat(NutrientGroup.IRON.isMentionedIn("상태를 철저히 관찰한다")).isFalse(); // "철저"는 철분이 아님
    }
}
