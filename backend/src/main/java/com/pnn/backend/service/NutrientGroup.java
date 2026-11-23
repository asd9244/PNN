package com.pnn.backend.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 영양제 성분 대응표.
 * <p>성분표에 적힌 이름(철, 비타민 D3, Iron …)을 성분 묶음으로 바꾸고,
 * 식약처 주의사항에서 그 성분을 가리키는 표현(철분제제, 철염 …)을 찾는다.</p>
 * <p>설명서 표현은 의약품 설명서 100개의 상호작용 항목을 보고 정했다. 약 계열·병명처럼
 * 영양제가 아닌데 같은 글자가 들어가는 표현(칼슘채널차단제, 비타민K의존성 등)은 먼저 지운다.</p>
 */
public enum NutrientGroup {

    IRON("철분",
            List.of("철", "철분", "헴철", "푸마르산제일철", "황산제일철", "iron", "ferrous", "ferric"),
            "철분|철염|철제제|철(을|이)?\\s*함유|철\\s*보충제|철\\s*ㆍ", null), // "철" 한 글자만으로는 찾지 않음 (철저, 철회 등)
    CALCIUM("칼슘",
            List.of("칼슘", "탄산칼슘", "구연산칼슘", "calcium"),
            "칼슘", "칼슘\\s*(채널|통로|길항)|(저|고)\\s*칼슘\\s*(혈증|뇨증)|칼슘\\s*(농도|수치)|(혈중|혈청)\\s*칼슘|칼슘\\s*치(?!료)"), // 혈중·혈청 칼슘, 칼슘치 = 피 속 칼슘 수치 이야기
    MAGNESIUM("마그네슘",
            List.of("마그네슘", "산화마그네슘", "magnesium"),
            "마그네슘", "(저|고)\\s*마그네슘\\s*혈증|마그네슘\\s*(농도|수치)"),
    ZINC("아연",
            List.of("아연", "산화아연", "글루콘산아연", "zinc"),
            "아연", null),
    POTASSIUM("칼륨",
            List.of("칼륨", "염화칼륨", "potassium"),
            "칼륨\\s*(보충제|보조제|보급제|제제|함유|염)|칼륨을\\s*함유", null), // 칼륨보존이뇨제·혈중 칼륨 농도는 영양제가 아님
    FOLATE("엽산",
            List.of("엽산", "folicacid", "folate"),
            "엽산", null),
    VITAMIN_A("비타민 A",
            List.of("비타민a", "레티놀", "베타카로틴", "vitamina", "retinol"),
            "비타민\\s*A|레티놀", null),
    VITAMIN_B6("비타민 B6",
            List.of("비타민b6", "피리독신", "vitaminb6", "pyridoxine"),
            "비타민\\s*B\\s*6|피리독신", null),
    VITAMIN_B12("비타민 B12",
            List.of("비타민b12", "시아노코발라민", "vitaminb12", "cobalamin"),
            "비타민\\s*B\\s*12|코발라민", null),
    VITAMIN_C("비타민 C",
            List.of("비타민c", "아스코르브산", "vitaminc", "ascorbicacid"),
            "비타민\\s*C|아스코르브산", null),
    VITAMIN_D("비타민 D",
            List.of("비타민d", "콜레칼시페롤", "vitamind", "cholecalciferol"),
            "비타민\\s*D|콜레칼시페롤|에르고칼시페롤", null),
    VITAMIN_E("비타민 E",
            List.of("비타민e", "토코페롤", "vitamine", "tocopherol"),
            "비타민\\s*E|토코페롤", null),
    VITAMIN_K("비타민 K",
            List.of("비타민k", "메나퀴논", "필로퀴논", "vitamink", "menaquinone"),
            "비타민\\s*K", "비타민\\s*K\\s*(길항|의존|결핍)"), // 와파린 계열 이름·응고인자 설명은 영양제가 아님
    BIOTIN("비오틴",
            List.of("비오틴", "biotin"),
            "비오틴", null),
    OMEGA3("오메가3",
            List.of("오메가3", "오메가-3", "epa", "dha", "피쉬오일", "어유", "정제어유", "omega3", "omega-3", "fishoil"),
            "오메가\\s*-?\\s*3|(?<![가-힣])어유|피쉬\\s*오일", null), // "있어 유산"처럼 붙은 글자는 제외
    ST_JOHNS_WORT("세인트존스워트",
            List.of("세인트존스워트", "성요한초", "성요한풀", "히페리쿰", "stjohn", "hypericum"),
            "세인트\\s*존스\\s*워트|성\\s*요한|St\\.?\\s*John|hypericum|히페리쿰", null);

    private static final Pattern NAME_NOISE = Pattern.compile("[\\s'’.·ㆍ]"); // 이름 비교 전에 지울 공백·기호

    private final String label;          // 화면에 보여 줄 이름
    private final List<String> aliases;  // 성분표에 적히는 이름 (공백·기호 없이, 소문자)
    private final Pattern mention;       // 설명서에서 이 성분을 가리키는 표현
    private final Pattern noise;         // 먼저 지울 오탐 표현 (없으면 null)

    NutrientGroup(String label, List<String> aliases, String mention, String noise) {
        this.label = label;
        this.aliases = aliases;
        this.mention = Pattern.compile(mention, Pattern.CASE_INSENSITIVE);
        this.noise = noise == null ? null : Pattern.compile(noise, Pattern.CASE_INSENSITIVE);
    }

    public String getLabel() {
        return label;
    }

    /** 화면용 이름(예: "철분")으로 성분 묶음을 찾는다. */
    public static Optional<NutrientGroup> fromLabel(String label) {
        for (NutrientGroup group : values()) {
            if (group.label.equals(label)) {
                return Optional.of(group);
            }
        }
        return Optional.empty();
    }

    /**
     * 성분표에 적힌 이름으로 성분 묶음을 찾는다. 가장 길게 앞부분이 맞는 이름을 고른다.
     * @param nutrientName 예: "철", "철(푸마르산제일철)", "비타민 D3", "Iron"
     * @return 대응표에 없는 성분(나트륨, 탄수화물, 판토텐산 등)이면 빈 값
     */
    public static Optional<NutrientGroup> fromNutrientName(String nutrientName) {
        if (nutrientName == null || nutrientName.isBlank()) {
            return Optional.empty();
        }
        String key = NAME_NOISE.matcher(nutrientName.toLowerCase(Locale.ROOT)).replaceAll("");
        NutrientGroup best = null;
        int bestLength = 0;
        for (NutrientGroup group : values()) {
            for (String alias : group.aliases) {
                if (key.startsWith(alias) && alias.length() > bestLength) { // "비타민b12"가 "비타민b1…"보다 우선하도록 가장 긴 것
                    best = group;
                    bestLength = alias.length();
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /** 글(설명서 문장 등)에 이 성분이 영양제로서 언급되는지. 오탐 표현을 먼저 지우고 찾는다. */
    public boolean isMentionedIn(String text) {
        return indexOfMention(text) >= 0;
    }

    /** 첫 언급 위치 (없으면 -1). 오탐 표현은 같은 길이의 공백으로 지워서 위치가 어긋나지 않게 한다. */
    public int indexOfMention(String text) {
        if (text == null || text.isBlank()) {
            return -1;
        }
        String cleaned = text;
        if (noise != null) {
            Matcher m = noise.matcher(text);
            StringBuilder sb = new StringBuilder();
            while (m.find()) {
                m.appendReplacement(sb, " ".repeat(m.group().length()));
            }
            cleaned = m.appendTail(sb).toString();
        }
        Matcher found = mention.matcher(cleaned);
        return found.find() ? found.start() : -1;
    }
}
