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
import type { InteractionItem } from '../api/ocr';
import { interactionResultScreenStyles as styles } from '../styles/InteractionResultScreenStyles';

if (Platform.OS === 'android') {
  if (UIManager.setLayoutAnimationEnabledExperimental) {
    UIManager.setLayoutAnimationEnabledExperimental(true);
  }
}

type InteractionResultRouteProp = RouteProp<RootStackParamList, 'InteractionResult'>;

function normLevel(level: string | undefined): string {
  return (level || '').toUpperCase().trim();
}

/** 카드/헤더용: WARNING · SAFE · 그 외(CAUTION 등) */
function getLevelVisual(level: string | undefined) {
  const L = normLevel(level);
  if (L === 'WARNING') {
    return { label: '위험', bgColor: '#FEF2F2', textColor: '#B91C1C' };
  }
  if (L === 'SAFE') {
    return { label: '안전', bgColor: '#ECFDF5', textColor: '#047857' };
  }
  return { label: '안내', bgColor: '#F3F4F6', textColor: '#374151' };
}

function getGroupHeaderBadge(items: InteractionItem[]) {
  const levels = items.map((i) => normLevel(i.level));
  if (levels.some((l) => l === 'WARNING')) {
    return { label: '위험', color: '#B91C1C', bgColor: '#FEF2F2' };
  }
  if (levels.some((l) => l === 'SAFE')) {
    return { label: '안전', color: '#047857', bgColor: '#ECFDF5' };
  }
  return { label: '안내', color: '#374151', bgColor: '#F3F4F6' };
}

/**
 * LLM이 nutrient를 "알 수 없음"으로 준 노이즈 행은 숨기고,
 * 백엔드 시스템 폴백(성분 없음·AI 오류 등)은 동일 문자열을 쓰므로 반드시 표시한다.
 */
function shouldShowInteractionItem(item: InteractionItem): boolean {
  if (
    item.nutrient === '알 수 없음' &&
    item.contraindicatedDrugIngredient === '데이터 없음'
  ) {
    return true;
  }
  return item.nutrient !== '알 수 없음';
}

export default function InteractionResultScreen() {
  const route = useRoute<InteractionResultRouteProp>();
  const result = route.params?.result;
  const bottomInset = useEffectiveBottomInset();

  const [expandedGroups, setExpandedGroups] = useState<Record<string, boolean>>({});

  const toggleGroup = (drugName: string) => {
    LayoutAnimation.configureNext(LayoutAnimation.Presets.easeInEaseOut);
    setExpandedGroups((prev) => ({
      ...prev,
      [drugName]: !prev[drugName],
    }));
  };

  const visibleInteractions =
    result?.interactions?.filter(shouldShowInteractionItem) ?? [];

  const groupedInteractions = visibleInteractions.reduce<Record<string, InteractionItem[]>>(
    (acc, item) => {
      const key = item.drugName || '이름 없는 약품';
      if (!acc[key]) acc[key] = [];
      acc[key].push(item);
      return acc;
    },
    {},
  );

  return (
    <View style={styles.safeArea}>
      <Header title="분석 결과" />
      <ScrollView
        contentContainerStyle={[styles.scrollContent, { paddingBottom: 20 + bottomInset }]}
      >
        <Text style={styles.pageTitle}>충돌 검사 결과</Text>

        <View style={styles.summaryCard}>
          <Text style={styles.summaryCardTitle}>분석 결과 요약</Text>
          <Text>
            {visibleInteractions.length > 0
              ? '상호작용이 감지된 항목이 있습니다.'
              : '특이한 상호작용이 발견되지 않았습니다.'}
          </Text>
        </View>

        {Object.keys(groupedInteractions).map((drugName) => {
          const items = groupedInteractions[drugName];
          const isExpanded = expandedGroups[drugName];
          const headerInfo = getGroupHeaderBadge(items);

          return (
            <View key={drugName} style={styles.groupBlock}>
              <TouchableOpacity
                onPress={() => toggleGroup(drugName)}
                activeOpacity={0.8}
                style={[styles.groupHeader, { borderColor: headerInfo.bgColor }]}
              >
                <View style={styles.groupHeaderLeft}>
                  <View
                    style={[styles.badgeWrap, { backgroundColor: headerInfo.bgColor }]}
                  >
                    <Text style={[styles.badgeText, { color: headerInfo.color }]}>
                      {headerInfo.label}
                    </Text>
                  </View>
                  <Text style={styles.drugNameText} numberOfLines={1}>
                    {drugName}
                  </Text>
                </View>
                <View style={styles.groupHeaderRight}>
                  <Text style={styles.countText}>{items.length}건</Text>
                  <Text style={styles.chevron}>{isExpanded ? '▲' : '▼'}</Text>
                </View>
              </TouchableOpacity>

              {isExpanded && (
                <View style={styles.expandedBlock}>
                  {items.map((item, index) => {
                    const visual = getLevelVisual(item.level);

                    return (
                      <View
                        key={`${drugName}-${index}-${item.nutrient}`}
                        style={[styles.itemCard, { backgroundColor: visual.bgColor }]}
                      >
                        <Text
                          style={[styles.itemLevelLabel, { color: visual.textColor }]}
                        >
                          {visual.label}
                        </Text>

                        <Text
                          style={[styles.itemPairTitle, { color: visual.textColor }]}
                        >
                          {`${item.nutrient} × ${item.contraindicatedDrugIngredient}`}
                        </Text>

                        <Text style={styles.itemDescription}>{item.description}</Text>

                        <View style={styles.guideBox}>
                          <Text style={styles.guideTitle}>안전 복약 가이드</Text>
                          <Text style={styles.guideBody}>{item.actionGuide}</Text>
                        </View>
                      </View>
                    );
                  })}
                </View>
              )}
            </View>
          );
        })}

        <HomeFooterLink />
      </ScrollView>
    </View>
  );
}
