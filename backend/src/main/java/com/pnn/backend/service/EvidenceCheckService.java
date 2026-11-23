package com.pnn.backend.service;

import com.pnn.backend.client.AiServerClient;
import com.pnn.backend.client.dto.AiEvidenceReadRequest;
import com.pnn.backend.client.dto.AiEvidenceReadResponse;
import com.pnn.backend.dto.EvidenceCheckResponseDto;
import com.pnn.backend.dto.EvidenceCheckResponseDto.DrugResult;
import com.pnn.backend.dto.EvidenceCheckResponseDto.Finding;
import com.pnn.backend.dto.EvidenceCheckResponseDto.Kind;
import com.pnn.backend.dto.EvidenceCheckResponseDto.NutrientResult;
import com.pnn.backend.dto.EvidenceCheckResponseDto.Status;
import com.pnn.backend.dto.InteractionCheckRequestDto;
import com.pnn.backend.dto.InteractionEvidenceResponseDto;
import com.pnn.backend.dto.InteractionEvidenceResponseDto.NutrientEvidence;
import com.pnn.backend.repository.DrugsMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Case A(근거 기반): 처방약 × 영양제 상호작용을 약 설명서 원문을 근거로 검사한다.
 * <p>흐름: 약마다 근거 문장 찾기(규칙) → 근거가 있을 때만 AI가 문장을 읽음(약당 1번)
 * → AI가 낸 근거 구절이 원문에 실제로 있는지 대조 → 성분별 결과</p>
 * <p>트랜잭션을 걸지 않는다. AI 호출을 기다리는 동안 DB 연결을 붙잡지 않기 위해서다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvidenceCheckService {

    private static final int DELAY_MS_BETWEEN_AI_CALLS = 1500; // AI 연속 호출 사이 대기 (기존 /check와 같음)
    private static final int MIN_QUOTE_LENGTH = 4;              // 이보다 짧은 근거 구절은 대조해도 의미가 없어 버림
    private static final List<Kind> SEVERITY = List.of(Kind.CAUTION, Kind.OTHER, Kind.UNVERIFIED, Kind.NO_EFFECT); // 주의가 필요한 순서
    private static final Pattern NUMBER = Pattern.compile("\\d+(\\.\\d+)?"); // 시간·용량 등 숫자

    private final DrugsMasterRepository drugsMasterRepository;
    private final InteractionEvidenceService interactionEvidenceService;
    private final AiServerClient aiServerClient;

    public EvidenceCheckResponseDto check(InteractionCheckRequestDto request) {
        List<Long> drugIds = request.getDrugIds() == null ? List.of() : request.getDrugIds().stream().distinct().toList();
        if (drugIds.isEmpty()) {
            throw new IllegalArgumentException("drugIds 목록이 비어있습니다.");
        }
        List<String> nutrientNames = collectNutrientNames(request);
        if (nutrientNames.isEmpty()) {
            throw new IllegalArgumentException("영양 성분 목록이 비어있습니다.");
        }
        DrugIdLookupValidator.assertAllRequestedIdsExist(drugIds, drugsMasterRepository.findAllById(drugIds)); // AI를 부르기 전에 잘못된 번호부터 거름

        List<DrugResult> results = new ArrayList<>();
        boolean aiCalledBefore = false;
        for (Long drugId : drugIds) {
            InteractionEvidenceResponseDto evidence = interactionEvidenceService.findEvidence(drugId, nutrientNames);
            Map<String, Integer> sentenceIds = numberSentences(evidence); // 같은 문장은 번호 하나
            Map<String, AiEvidenceReadResponse.Result> verified = Map.of();
            boolean aiCalled = !sentenceIds.isEmpty();
            if (aiCalled) {
                if (aiCalledBefore) {
                    sleepBetweenAiCalls();
                }
                verified = readAndVerify(evidence, sentenceIds);
                aiCalledBefore = true;
            }
            results.add(toDrugResult(evidence, sentenceIds, verified, aiCalled));
        }
        return EvidenceCheckResponseDto.builder().drugs(results).build();
    }

    /** 모든 영양제의 성분 이름을 순서대로, 중복 없이 모은다. */
    private static List<String> collectNutrientNames(InteractionCheckRequestDto request) {
        Set<String> names = new LinkedHashSet<>();
        if (request.getSupplements() != null) {
            for (InteractionCheckRequestDto.SupplementInput supplement : request.getSupplements()) {
                if (supplement.getNutrients() == null) {
                    continue;
                }
                for (InteractionCheckRequestDto.NutrientInput nutrient : supplement.getNutrients()) {
                    if (nutrient.getName() != null && !nutrient.getName().isBlank()) {
                        names.add(nutrient.getName().trim());
                    }
                }
            }
        }
        return new ArrayList<>(names);
    }

    /** 근거 문장마다 번호를 붙인다. 여러 성분이 같은 문장을 가리키면 같은 번호. */
    private static Map<String, Integer> numberSentences(InteractionEvidenceResponseDto evidence) {
        Map<String, Integer> ids = new LinkedHashMap<>();
        for (NutrientEvidence nutrient : evidence.getNutrients()) {
            for (String sentence : nutrient.getSentences()) {
                ids.putIfAbsent(sentence, ids.size() + 1);
            }
        }
        return ids;
    }

    /** AI에게 문장들을 한 번에 읽히고, 원문과 맞는 결과만 남긴다. 키: "문장번호|성분묶음" */
    private Map<String, AiEvidenceReadResponse.Result> readAndVerify(InteractionEvidenceResponseDto evidence,
                                                                     Map<String, Integer> sentenceIds) {
        Map<Integer, String> sentenceById = new HashMap<>();
        Map<Integer, Set<String>> groupsById = new LinkedHashMap<>();
        for (NutrientEvidence nutrient : evidence.getNutrients()) {
            for (String sentence : nutrient.getSentences()) {
                int id = sentenceIds.get(sentence);
                sentenceById.put(id, sentence);
                groupsById.computeIfAbsent(id, k -> new LinkedHashSet<>()).add(nutrient.getMatchedGroup());
            }
        }
        List<AiEvidenceReadRequest.Item> items = groupsById.entrySet().stream()
                .map(e -> AiEvidenceReadRequest.Item.builder()
                        .id(e.getKey())
                        .sentence(sentenceById.get(e.getKey()))
                        .nutrients(new ArrayList<>(e.getValue()))
                        .build())
                .toList();

        AiEvidenceReadResponse response;
        try {
            response = aiServerClient.readEvidence(AiEvidenceReadRequest.builder()
                    .drugName(evidence.getDrugName())
                    .items(items)
                    .build());
        } catch (RestClientException e) {
            log.warn("AI 근거 읽기 실패 (drug: {}). 원문만 반환: {}", evidence.getDrugName(), e.getMessage());
            return Map.of();
        }
        if (response == null || response.getResults() == null) {
            return Map.of();
        }

        Map<String, AiEvidenceReadResponse.Result> verified = new HashMap<>();
        for (AiEvidenceReadResponse.Result result : response.getResults()) {
            String sentence = sentenceById.get(result.getId());
            boolean askedPair = sentence != null && groupsById.get(result.getId()).contains(result.getNutrient());
            if (!askedPair || parseKind(result.getKind()) == null) {
                continue; // 묻지 않은 문장·성분이거나 정해진 종류가 아니면 버림
            }
            if (!quoteAppearsIn(result.getQuote(), sentence)) {
                log.warn("근거 구절이 원문에 없음 → 버림 (drug: {}, 성분: {}, 구절: {})",
                        evidence.getDrugName(), result.getNutrient(), result.getQuote());
                continue;
            }
            if (!numbersAppearIn(result.getExplanation() + " " + result.getAction(), sentence)) {
                log.warn("설명·할 일의 숫자가 원문에 없음 → 버림 (drug: {}, 성분: {}, 할 일: {})",
                        evidence.getDrugName(), result.getNutrient(), result.getAction());
                continue;
            }
            verified.putIfAbsent(result.getId() + "|" + result.getNutrient(), result);
        }
        return verified;
    }

    /** AI가 낸 근거 구절이 원문 문장 안에 글자 그대로 있는지. PDF 줄바꿈으로 생긴 띄어쓰기 차이는 무시한다. */
    static boolean quoteAppearsIn(String quote, String sentence) {
        if (quote == null || sentence == null) {
            return false;
        }
        String q = squash(quote);
        return q.length() >= MIN_QUOTE_LENGTH && squash(sentence).contains(q);
    }

    /** 설명·할 일에 나온 숫자(시간·용량 등)가 모두 원문에 있는지. AI가 "4시간"을 "2시간"으로 바꾸는 일을 막는다. */
    static boolean numbersAppearIn(String text, String sentence) {
        Matcher number = NUMBER.matcher(text == null ? "" : text);
        Set<String> inSentence = new LinkedHashSet<>();
        Matcher m = NUMBER.matcher(sentence);
        while (m.find()) {
            inSentence.add(m.group());
        }
        while (number.find()) {
            if (!inSentence.contains(number.group())) {
                return false;
            }
        }
        return true;
    }

    private static String squash(String text) {
        return text.replace("…", "").replaceAll("\\s+", ""); // "…"(잘라 낸 표시)와 모든 공백 제거
    }

    private static Kind parseKind(String kind) {
        if (kind == null) {
            return null;
        }
        return switch (kind.trim().toUpperCase()) {
            case "CAUTION" -> Kind.CAUTION;
            case "NO_EFFECT" -> Kind.NO_EFFECT;
            case "OTHER" -> Kind.OTHER;
            default -> null;
        };
    }

    private static DrugResult toDrugResult(InteractionEvidenceResponseDto evidence, Map<String, Integer> sentenceIds,
                                           Map<String, AiEvidenceReadResponse.Result> verified, boolean aiCalled) {
        List<NutrientResult> nutrients = new ArrayList<>();
        for (NutrientEvidence nutrient : evidence.getNutrients()) {
            List<Finding> findings = new ArrayList<>();
            for (String sentence : nutrient.getSentences()) {
                AiEvidenceReadResponse.Result read = verified.get(sentenceIds.get(sentence) + "|" + nutrient.getMatchedGroup());
                findings.add(Finding.builder()
                        .kind(read != null ? parseKind(read.getKind()) : Kind.UNVERIFIED)
                        .sentence(sentence)
                        .explanation(read != null ? read.getExplanation() : null)
                        .action(read != null && read.getAction() != null && !read.getAction().isBlank() ? read.getAction() : null)
                        .build());
            }
            nutrients.add(NutrientResult.builder()
                    .nutrient(nutrient.getNutrient())
                    .matchedGroup(nutrient.getMatchedGroup())
                    .status(findings.isEmpty() ? Status.valueOf(nutrient.getStatus().name()) : worst(findings))
                    .findings(findings)
                    .build());
        }
        return DrugResult.builder()
                .drugId(evidence.getDrugId())
                .drugName(evidence.getDrugName())
                .sectionFound(evidence.isSectionFound())
                .aiCalled(aiCalled)
                .nutrients(nutrients)
                .build();
    }

    /** 문장이 여럿이면 가장 주의가 필요한 종류를 성분의 최종 상태로 쓴다. */
    private static Status worst(List<Finding> findings) {
        Kind worst = SEVERITY.stream()
                .filter(kind -> findings.stream().map(Finding::getKind).anyMatch(kind::equals))
                .findFirst()
                .orElse(Kind.UNVERIFIED);
        return Status.valueOf(worst.name());
    }

    /** 인터럽트되면 플래그만 복원하고 계속 진행한다. */
    private static void sleepBetweenAiCalls() {
        try {
            Thread.sleep(DELAY_MS_BETWEEN_AI_CALLS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
