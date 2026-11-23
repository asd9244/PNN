package com.pnn.backend.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 상호작용 항목을 번호 문장("1)", "(2)", "③") 단위로 나누고, 성분이 언급된 문장만 고른다. AI를 쓰지 않는다.
 */
public final class EvidenceSentenceFinder {

    private static final Pattern HEADING = Pattern.compile("^\\d{1,2}\\s*\\.\\s*상\\s*호\\s*작\\s*용\\s*"); // 맨 앞의 "6. 상호작용" 제목
    private static final Pattern ITEM_START = Pattern.compile("\\s(?=(\\(?\\d{1,2}\\)|[①-⑳])\\s?\\S)"); // "1)" "(2)" "③" 앞에서 나눔
    private static final Pattern SENTENCE_END = Pattern.compile("(?<=다\\.)\\s+"); // 한국어 문장 끝 "…다." 뒤에서 나눔
    private static final int LONG_ITEM = 500; // 이보다 긴 번호 문장은 성분이 나온 문장만 남김
    private static final int MAX_EVIDENCE = 400; // 문장으로도 안 나뉘는 긴 글(표가 풀린 글 등)은 언급 앞뒤만 남김
    private static final int BEFORE = 150; // 언급 위치 앞으로 남길 글자 수
    private static final int AFTER = 250;  // 언급 위치 뒤로 남길 글자 수

    private EvidenceSentenceFinder() {
    }

    /** 번호 문장 단위로 나눈다. PDF 추출 중 같은 문장이 두 번 나오면 하나만 남긴다. */
    public static List<String> splitItems(String section) {
        if (section == null || section.isBlank()) {
            return List.of();
        }
        String body = HEADING.matcher(section.trim()).replaceFirst("");
        Set<String> items = new LinkedHashSet<>(); // 순서 유지 + 중복 제거
        for (String item : ITEM_START.split(body)) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                items.add(trimmed);
            }
        }
        return new ArrayList<>(items);
    }

    /**
     * 성분이 언급된 설명서 원문 문장들을 돌려준다.
     * @param section 상호작용 항목 (InteractionSectionExtractor 결과)
     * @param group   찾을 성분 묶음
     * @return 언급이 없으면 빈 목록
     */
    public static List<String> findSentences(String section, NutrientGroup group) {
        List<String> found = new ArrayList<>();
        for (String item : splitItems(section)) {
            if (!group.isMentionedIn(item)) {
                continue;
            }
            if (item.length() <= LONG_ITEM) {
                found.add(item); // 번호 문장 하나를 통째로 근거로 씀 (뒤따르는 조치 문장까지 포함)
                continue;
            }
            List<String> sentences = new ArrayList<>();
            for (String sentence : SENTENCE_END.split(item)) {
                if (group.isMentionedIn(sentence)) {
                    sentences.add(sentence.trim());
                }
            }
            for (String sentence : sentences.isEmpty() ? List.of(item) : sentences) { // 문장 경계에 걸쳐 있으면 통째로
                found.add(trimAroundMention(sentence, group));
            }
        }
        return found;
    }

    /** 긴 글은 언급 위치 앞뒤만 남기고 잘린 쪽에 "…"를 붙인다. */
    private static String trimAroundMention(String text, NutrientGroup group) {
        if (text.length() <= MAX_EVIDENCE) {
            return text;
        }
        int at = Math.max(0, group.indexOfMention(text));
        int start = Math.max(0, at - BEFORE);
        int end = Math.min(text.length(), at + AFTER);
        return (start > 0 ? "…" : "") + text.substring(start, end).trim() + (end < text.length() ? "…" : "");
    }
}
