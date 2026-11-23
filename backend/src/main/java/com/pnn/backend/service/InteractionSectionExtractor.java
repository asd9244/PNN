package com.pnn.backend.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 식약처 주의사항(사용상의 주의사항) 글에서 "N. 상호작용" 항목만 잘라낸다.
 * <p>본문 속 "(5. 상호작용 참조)" 같은 언급은 건너뛰고, 다음 번호 항목(N+1.) 앞에서 끝낸다.</p>
 */
public final class InteractionSectionExtractor {

    private static final Pattern HEADING = Pattern.compile("(\\d{1,2})\\s*\\.\\s*상\\s*호\\s*작\\s*용"); // "6. 상호작용" (PDF 추출 시 글자 사이 공백 허용)
    private static final Pattern FIRST_ITEM = Pattern.compile("^\\s*\\(?1\\)"); // 제목 바로 뒤의 "1)" 또는 "(1)"
    private static final int MAX_LENGTH_WITHOUT_END = 4000; // 다음 항목을 못 찾았을 때 자를 최대 길이

    private InteractionSectionExtractor() {
    }

    /**
     * 상호작용 항목 글을 반환한다.
     * @param caution 주의사항 글 (drug_permit_detail.caution)
     * @return 상호작용 항목. 주의사항이 비었거나 아직 PDF 주소이거나 항목이 없으면 null
     */
    public static String extract(String caution) {
        if (caution == null || caution.isBlank() || caution.trim().startsWith("http")) {
            return null;
        }
        String text = caution.replaceAll("\\s+", " "); // 줄바꿈·연속 공백을 한 칸으로

        int start = -1;
        int headingEnd = -1;
        int number = -1;
        Matcher heading = HEADING.matcher(text);
        while (heading.find()) {
            String after = text.substring(heading.end(), Math.min(text.length(), heading.end() + 8));
            if (after.contains("참조") || after.trim().startsWith("항")) {
                continue; // "(5. 상호작용 참조)" 같은 본문 속 언급은 제목이 아님
            }
            start = heading.start();
            headingEnd = heading.end();
            number = Integer.parseInt(heading.group(1));
            if (FIRST_ITEM.matcher(after).find()) {
                break; // 바로 뒤에 "1)"이 오면 진짜 제목으로 확정
            }
        }
        if (start < 0) {
            return null;
        }

        Matcher next = Pattern.compile("\\s" + (number + 1) + "\\s*\\.\\s*[가-힣]").matcher(text); // 다음 항목 제목 (예: "7. 임부")
        int end = next.find(headingEnd) ? next.start() : Math.min(text.length(), start + MAX_LENGTH_WITHOUT_END);
        return text.substring(start, end).trim();
    }
}
