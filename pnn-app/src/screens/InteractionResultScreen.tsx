import React, { useState } from 'react';
import {
  Text,
  View,
  ScrollView,
  TouchableOpacity,
  LayoutAnimation,
  Platform,
  UIManager,
} from 'react-native';
import { useEffectiveBottomInset } from '../hooks/useEffectiveBottomInset';
import { RouteProp, useRoute } from '@react-navigation/native';
import { RootStackParamList } from '../navigation/AppNavigator';
import Header from '../components/Header';
import HomeFooterLink from '../components/HomeFooterLink';
import type {
  EvidenceDrugResult,
  EvidenceFinding,
  EvidenceNutrientResult,
  EvidenceStatus,
} from '../api/ocr';
import { interactionResultScreenStyles as styles } from '../styles/InteractionResultScreenStyles';

if (Platform.OS === 'android') {
  if (UIManager.setLayoutAnimationEnabledExperimental) {
    UIManager.setLayoutAnimationEnabledExperimental(true);
  }
}

type InteractionResultRouteProp = RouteProp<RootStackParamList, 'InteractionResult'>;

/** 상태별 이름표·색. 위에서부터 더 주의가 필요한 순서 */
const STATUS_VISUAL: Record<EvidenceStatus, { label: string; textColor: string; bgColor: string }> = {
  CAUTION: { label: '주의', textColor: '#B91C1C', bgColor: '#FEF2F2' },
  OTHER: { label: '기타 영향', textColor: '#B45309', bgColor: '#FFFBEB' },
  UNVERIFIED: { label: '원문 확인 필요', textColor: '#1D4ED8', bgColor: '#EFF6FF' },
  NO_EFFECT: { label: '영향 없음 (설명서)', textColor: '#047857', bgColor: '#ECFDF5' },
  NOT_MENTIONED: { label: '설명서에 언급 없음', textColor: '#374151', bgColor: '#F3F4F6' },
  NOT_IN_TABLE: { label: '검사 대상 아님', textColor: '#6B7280', bgColor: '#F3F4F6' },
  NO_SECTION: { label: '설명서 확인 불가', textColor: '#6B7280', bgColor: '#F3F4F6' },
};
const STATUS_ORDER: EvidenceStatus[] = [
  'CAUTION', 'OTHER', 'UNVERIFIED', 'NO_EFFECT', 'NOT_MENTIONED', 'NO_SECTION', 'NOT_IN_TABLE',
];

/** 설명서 근거가 없는 상태일 때 보여 줄 안내 */
const NO_FINDING_MESSAGE: Partial<Record<EvidenceStatus, string>> = {
  NOT_MENTIONED:
    '이 약의 설명서 상호작용 항목에는 이 성분 이야기가 없어요. 언급이 없다고 안전하다는 뜻은 아니니, 복용 전 의사·약사와 상의하세요.',
  NOT_IN_TABLE: '이 성분은 아직 검사 대상에 들어 있지 않아요.',
  NO_SECTION: '설명서에서 상호작용 항목을 찾지 못했어요. 복용 전 의사·약사와 상의하세요.',
};

function worstStatus(nutrients: EvidenceNutrientResult[]): EvidenceStatus {
  return STATUS_ORDER.find((s) => nutrients.some((n) => n.status === s)) ?? 'NOT_MENTIONED';
}

function sortBySeverity(nutrients: EvidenceNutrientResult[]): EvidenceNutrientResult[] {
  return [...nutrients].sort((a, b) => STATUS_ORDER.indexOf(a.status) - STATUS_ORDER.indexOf(b.status));
}

/** 문장 하나에 대한 설명. 영향 없음은 AI 설명 대신 정해진 문장으로 보여 준다 (안심시키는 말투를 피하려고) */
function findingMessage(finding: EvidenceFinding): string {
  if (finding.kind === 'NO_EFFECT') {
    return '설명서에 영향이 없었다는 내용이 있어요. 아래 원문을 확인하세요.';
  }
  if (finding.kind === 'UNVERIFIED') {
    return finding.note ?? 'AI가 이 문장을 확실히 읽지 못했어요. 아래 원문을 확인하세요.';
  }
  return finding.explanation ?? '쉬운 설명을 만들지 못했어요. 아래 원문을 확인하세요.';
}

function FindingBlock({ finding }: { finding: EvidenceFinding }) {
  const visual = STATUS_VISUAL[finding.kind];
  return (
    <View style={styles.findingBlock}>
      <Text style={[styles.findingKind, { color: visual.textColor }]}>{visual.label}</Text>
      <Text style={styles.itemDescription}>{findingMessage(finding)}</Text>
      {finding.action && (finding.kind === 'CAUTION' || finding.kind === 'OTHER') ? (
        <View style={styles.guideBox}>
          <Text style={styles.guideTitle}>할 일</Text>
          <Text style={styles.guideBody}>{finding.action}</Text>
        </View>
      ) : null}
      <View style={styles.sentenceBox}>
        <Text style={styles.sentenceLabel}>설명서 원문</Text>
        <Text style={styles.sentenceText}>{finding.sentence}</Text>
      </View>
    </View>
  );
}

function NutrientCard({ nutrient }: { nutrient: EvidenceNutrientResult }) {
  const visual = STATUS_VISUAL[nutrient.status];
  const showGroup = nutrient.matchedGroup && nutrient.matchedGroup !== nutrient.nutrient;
  return (
    <View style={[styles.itemCard, { backgroundColor: visual.bgColor }]}>
      <Text style={[styles.itemLevelLabel, { color: visual.textColor }]}>{visual.label}</Text>
      <Text style={[styles.itemPairTitle, { color: visual.textColor }]}>
        {showGroup ? `${nutrient.nutrient} (${nutrient.matchedGroup})` : nutrient.nutrient}
      </Text>
      {nutrient.findings.length === 0 ? (
        <Text style={styles.itemDescription}>{NO_FINDING_MESSAGE[nutrient.status] ?? ''}</Text>
      ) : (
        nutrient.findings.map((finding, index) => <FindingBlock key={index} finding={finding} />)
      )}
    </View>
  );
}

function summaryText(drugs: EvidenceDrugResult[]): string {
  const all = drugs.flatMap((d) => d.nutrients);
  const count = (s: EvidenceStatus) => all.filter((n) => n.status === s).length;
  const caution = count('CAUTION') + count('OTHER');
  const check = count('UNVERIFIED');
  if (caution > 0) {
    return `설명서에 주의 내용이 있는 성분이 ${caution}개 있어요.` + (check > 0 ? ` 원문 확인이 필요한 성분도 ${check}개 있어요.` : '');
  }
  if (check > 0) {
    return `원문 확인이 필요한 성분이 ${check}개 있어요.`;
  }
  return '설명서에서 주의 내용을 찾지 못했어요. 다만 설명서에 없다고 안전하다는 뜻은 아니에요.';
}

export default function InteractionResultScreen() {
  const route = useRoute<InteractionResultRouteProp>();
  const drugs = route.params?.result?.drugs ?? [];
  const bottomInset = useEffectiveBottomInset();

  const [collapsed, setCollapsed] = useState<Record<number, boolean>>({}); // 기본은 펼침

  const toggleGroup = (drugId: number) => {
    LayoutAnimation.configureNext(LayoutAnimation.Presets.easeInEaseOut);
    setCollapsed((prev) => ({ ...prev, [drugId]: !prev[drugId] }));
  };

  return (
    <View style={styles.safeArea}>
      <Header title="분석 결과" />
      <ScrollView
        contentContainerStyle={[styles.scrollContent, { paddingBottom: 20 + bottomInset }]}
      >
        <Text style={styles.pageTitle}>충돌 검사 결과</Text>

        <View style={styles.summaryCard}>
          <Text style={styles.summaryCardTitle}>분석 결과 요약</Text>
          <Text>{summaryText(drugs)}</Text>
        </View>

        {drugs.map((drug) => {
          const isExpanded = !collapsed[drug.drugId];
          const headerInfo = STATUS_VISUAL[worstStatus(drug.nutrients)];

          return (
            <View key={drug.drugId} style={styles.groupBlock}>
              <TouchableOpacity
                onPress={() => toggleGroup(drug.drugId)}
                activeOpacity={0.8}
                style={[styles.groupHeader, { borderColor: headerInfo.bgColor }]}
              >
                <View style={styles.groupHeaderLeft}>
                  <View style={[styles.badgeWrap, { backgroundColor: headerInfo.bgColor }]}>
                    <Text style={[styles.badgeText, { color: headerInfo.textColor }]}>
                      {headerInfo.label}
                    </Text>
                  </View>
                  <Text style={styles.drugNameText} numberOfLines={1}>
                    {drug.drugName}
                  </Text>
                </View>
                <View style={styles.groupHeaderRight}>
                  <Text style={styles.countText}>{drug.nutrients.length}개 성분</Text>
                  <Text style={styles.chevron}>{isExpanded ? '▲' : '▼'}</Text>
                </View>
              </TouchableOpacity>

              {isExpanded && (
                <View style={styles.expandedBlock}>
                  {sortBySeverity(drug.nutrients).map((nutrient) => (
                    <NutrientCard key={`${drug.drugId}-${nutrient.nutrient}`} nutrient={nutrient} />
                  ))}
                </View>
              )}
            </View>
          );
        })}

        <Text style={styles.disclaimer}>
          이 결과는 식약처 허가 의약품 설명서(사용상의 주의사항)를 근거로 합니다. 정보 제공용이며, 복용 결정은 반드시 의사·약사와 상의하세요.
        </Text>

        <HomeFooterLink />
      </ScrollView>
    </View>
  );
}
