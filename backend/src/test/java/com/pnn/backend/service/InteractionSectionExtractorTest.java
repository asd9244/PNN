package com.pnn.backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InteractionSectionExtractorTest {

    @Test
    @DisplayName("본문 속 '(6. 상호작용 참조)'는 건너뛰고, 진짜 상호작용 항목만 다음 항목 앞까지 잘라낸다")
    void extractsRealSectionOnly() {
        String caution = "5. 일반적 주의 1) 이 약은 철분제와 간격을 둔다(6. 상호작용 참조). "
                + "6. 상호작용 1) 철분제제와 병용투여시 이 약의 흡수가 감소될 수 있다. "
                + "2) 칼슘제제와 병용투여시 최소 4시간 간격을 둔다. "
                + "7. 임부 및 수유부에 대한 투여 1) 임신 중에도 치료를 중단하지 않는다.";

        String section = InteractionSectionExtractor.extract(caution);

        assertThat(section).startsWith("6. 상호작용 1) 철분제제"); // 참조 문구가 아닌 진짜 제목에서 시작
        assertThat(section).contains("칼슘제제"); // 항목 안의 내용은 모두 포함
        assertThat(section).doesNotContain("임부"); // 다음 항목(7.)은 포함하지 않음
    }

    @Test
    @DisplayName("PDF에서 글자 사이에 공백이 끼어 '6 . 상 호작용'이 되어도 찾는다")
    void toleratesSpacesInsideHeading() {
        String caution = "6 . 상 호작용 1) 제산제와 병용 시 흡수가 감소한다. 7. 고령자에 대한 투여";

        assertThat(InteractionSectionExtractor.extract(caution))
                .isEqualTo("6 . 상 호작용 1) 제산제와 병용 시 흡수가 감소한다.");
    }

    @Test
    @DisplayName("상호작용 항목이 없으면 null")
    void returnsNullWhenNoSection() {
        assertThat(InteractionSectionExtractor.extract("1. 경고 2. 다음 환자에는 투여하지 말 것")).isNull();
    }

    @Test
    @DisplayName("주의사항이 비었거나 아직 PDF 주소이면 null")
    void returnsNullForEmptyOrUrl() {
        assertThat(InteractionSectionExtractor.extract(null)).isNull();
        assertThat(InteractionSectionExtractor.extract("  ")).isNull();
        assertThat(InteractionSectionExtractor.extract("https://nedrug.mfds.go.kr/pbp/cmn/pdfDownload/197400278/NB")).isNull();
    }
}
