import React, { useCallback, useEffect, useState } from 'react';
import axios from 'axios';
import {
  View,
  Text,
  SafeAreaView,
  ScrollView,
  TouchableOpacity,
  Image,
  Alert,
  Platform,
} from 'react-native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { RouteProp, useRoute } from '@react-navigation/native';
import { RootStackParamList } from '../navigation/AppNavigator';
import { useDrugStore, SelectedDrug } from '../store/useDrugStore';

import Header from '../components/Header';
import InfoRow from '../components/InfoRow';
import LoadingErrorView from '../components/LoadingErrorView';
import { drugDetailScreenStyles as styles } from '../styles';
import { getDrugDetail } from '../api/drugs';
import type { DrugDetailResponseDto, ParsedIngredient } from '../types/drug';

/** parsedIngredients가 있으면 줄바꿈 리스트로 포맷, 없으면 rawIngredients 반환 */
function formatIngredients(
  parsed: ParsedIngredient[] | null | undefined,
  raw: string | null | undefined
): string {
  if (parsed && parsed.length > 0) {
    return parsed
      .map((p) => {
        const amt = p.amount != null ? String(p.amount) : '';
        const u = p.unit || '';
        return amt && u ? `${p.name} ${amt}${u}` : amt ? `${p.name} ${amt}` : p.name;
      })
      .join('\n');
  }
  return raw || '-';
}

type NavigationProp = NativeStackNavigationProp<RootStackParamList, 'DrugDetail'>;
type RoutePropType = RouteProp<RootStackParamList, 'DrugDetail'>;
interface Props {
  navigation: NavigationProp;
}

const TABS = ['약품정보', '복약정보', '허가정보', 'DUR'];
const PLACEHOLDER_IMAGE = 'https://via.placeholder.com/300x200?text=No+Image';

export default function DrugDetailScreen({ navigation }: Props) {
  const route = useRoute<RoutePropType>();
  const drugId = route.params?.drugId;
  const sourceScreen = route.params?.sourceScreen;

  const addInteractionDrug = useDrugStore((state) => state.addInteractionDrug);
  const addRecommendationDrug = useDrugStore((state) => state.addRecommendationDrug);

  const [data, setData] = useState<DrugDetailResponseDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState('약품정보');

  const handleAddDrug = () => {
    if (!data || !sourceScreen) return;
    if (drugId == null || !Number.isFinite(Number(drugId))) {
      Alert.alert('오류', '약품 식별 정보가 없어 추가할 수 없습니다.');
      return;
    }

    const engList =
      data.drugInfo?.ingredientNamesEng?.filter(
        (s): s is string => typeof s === 'string' && s.trim().length > 0
      ) ?? [];

    const selectedDrug: SelectedDrug = {
      drugId: Number(drugId),
      itemName: data.header?.itemName ?? '이름 없음',
      entpName: data.drugInfo?.entpName ?? '제조사 없음',
      ingredientNamesEng: engList,
    };

    const proceed = () => {
      if (sourceScreen === 'InteractionCheck') {
        addInteractionDrug(selectedDrug);
        navigation.navigate('InteractionCheck');
      } else if (sourceScreen === 'Recommendation') {
        addRecommendationDrug(selectedDrug);
        navigation.navigate('Recommendation');
      }
    };

    if (engList.length === 0) {
      const message =
        '이 약품은 등록된 성분 데이터가 없어 상호작용 비교 및 영양제 추천 분석을 할 수 없습니다. 성분 정보가 있는 약품만 목록에 추가할 수 있습니다.';
      if (Platform.OS === 'web') {
        window.alert(message);
      } else {
        Alert.alert('안내', message, [{ text: '확인' }]);
      }
      return;
    }

    proceed();
  };

  const fetchDetail = useCallback(async () => {
    if (drugId == null) {
      setError('약품 정보를 불러올 수 없습니다.');
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const response = await getDrugDetail(drugId);
      setData(response);
    } catch (err: unknown) {
      let message = '약품 상세 정보를 불러오는 중 오류가 발생했습니다.';
      if (axios.isAxiosError(err) && err.response?.data) {
        const resData = err.response.data as Record<string, unknown>;
        if (resData && typeof resData.message === 'string')
          message = resData.message;
      } else if (err instanceof Error) {
        message = err.message;
      }
      setError(message);
    } finally {
      setLoading(false);
    }
  }, [drugId]);

  useEffect(() => {
    fetchDetail();
  }, [fetchDetail]);

  const renderContent = () => {
    if (!data) return null;

    switch (activeTab) {
      case '약품정보':
        return (
          <View style={styles.tabContentContainer}>
            <View style={styles.drugImageContainer}>
              <Image
                source={{
                  uri: data.drugInfo?.itemImageUrl || PLACEHOLDER_IMAGE,
                }}
                style={styles.drugImage}
                resizeMode="cover"
              />
            </View>
            <InfoRow label="구분" value={data.drugInfo?.etcOtcCode} />
            <InfoRow label="판매사" value={data.drugInfo?.entpName} />
            <InfoRow label="제조사" value={data.drugInfo?.consignEntp} />
            <InfoRow label="보험코드" value={data.drugInfo?.insurCode} />
            <InfoRow
              label="영문 전체 성분 및 함량"
              value={formatIngredients(data.drugInfo?.parsedIngredients, data.drugInfo?.rawIngredients)}
            />
            <InfoRow label="주성분 코드" value={data.drugInfo?.mainIngrCode} />
            <InfoRow label="분류명" value={data.drugInfo?.className} />
            <InfoRow label="ATC코드" value={data.drugInfo?.atcCode} />
          </View>
        );

      case '복약정보':
        return (
          <View style={styles.tabContentContainer}>
            <InfoRow
              label="이 약은 어떤 효능이 있나요?"
              value={data.medicationInfo?.efficacy || '정보가 없습니다.'}
            />
            <InfoRow
              label="복용 시 주의사항 및 안내"
              value={data.medicationInfo?.cautionUse || '정보가 없습니다.'}
            />
            <Text style={styles.footerNote}>
              * 본 정보는 'e약은요' 공공데이터를 기반으로 작성되었습니다. 자세한
              내용은 전문가와 상의하십시오.
            </Text>
          </View>
        );

      case '허가정보':
        return (
          <View style={styles.tabContentContainer}>
            <View style={styles.permitGrid}>
              <View style={styles.permitGridItemLeft}>
                <InfoRow label="품목기준코드" value={data.permitInfo?.itemSeq} />
              </View>
              <View style={styles.permitGridItemRight}>
                <InfoRow label="유효기간" value={data.permitInfo?.validityPeriod} />
              </View>
              <View style={styles.permitGridItemLeft}>
                <InfoRow label="저장방법" value={data.permitInfo?.storageMethod} />
              </View>
              <View style={styles.permitGridItemRight}>
                <InfoRow label="포장단위" value={data.permitInfo?.packageUnit} />
              </View>
            </View>

            <View style={styles.permitSection}>
              <View style={styles.permitSectionHeader}>
                <View
                  style={[styles.verticalBar, { backgroundColor: '#111827' }]}
                />
                <Text style={styles.permitSectionTitle}>효능효과</Text>
              </View>
              <Text style={styles.permitText}>
                {data.permitInfo?.efficacy || '정보가 없습니다.'}
              </Text>
            </View>

            <View style={styles.permitSection}>
              <View style={styles.permitSectionHeader}>
                <View
                  style={[styles.verticalBar, { backgroundColor: '#111827' }]}
                />
                <Text style={styles.permitSectionTitle}>용법용량</Text>
              </View>
              <Text style={styles.permitText}>
                {data.permitInfo?.dosage || '정보가 없습니다.'}
              </Text>
            </View>

            <View style={styles.permitSection}>
              <View style={styles.permitSectionHeader}>
                <View
                  style={[styles.verticalBar, { backgroundColor: '#111827' }]}
                />
                <Text style={styles.permitSectionTitle}>주의사항</Text>
              </View>
              <View style={styles.grayBox}>
                <Text style={styles.permitText}>
                  {data.permitInfo?.caution || '정보가 없습니다.'}
                </Text>
              </View>
              <Text style={styles.footerNote}>
                * 상세 주의사항 원문은 식약처 의약품 안전나라에서 확인 가능합니다.
              </Text>
            </View>
          </View>
        );

      case 'DUR': {
        const contraindications = data.durInfo?.contraindications ?? [];
        const durWarnings = data.durInfo?.durWarnings ?? [];
        const hasDUR = contraindications.length > 0 || durWarnings.length > 0;

        return (
          <View style={styles.tabContentContainer}>
            <View style={styles.permitSectionHeader}>
              <View
                style={[styles.verticalBar, { backgroundColor: '#111827' }]}
              />
              <Text style={styles.permitSectionTitle}>DUR(의약품 안심 서비스)</Text>
            </View>

            {!hasDUR ? (
              <Text style={styles.emptyDurText}>
                제품에 대한 DUR 지침이 존재하지 않습니다.
              </Text>
            ) : (
              <>
                {contraindications.length > 0 && (
                  <View style={styles.durSection}>
                    <Text style={styles.durSubTitle}>병용금기 성분</Text>
                    {contraindications.map((item, index) => (
                      <View key={index} style={styles.durItemContainer}>
                        <InfoRow label="원료명1" value={item.ingrName1} noBorder />
                        <InfoRow label="원료명2" value={item.ingrName2} noBorder />
                        <InfoRow label="금기내용" value={item.contraindReason} noBorder />
                      </View>
                    ))}
                  </View>
                )}

                {durWarnings.length > 0 && (() => {
                  const warnings = durWarnings;
                  const byType = warnings.reduce<Record<string, typeof warnings>>(
                    (acc, w) => {
                      const key = w.durType || '기타';
                      if (!acc[key]) acc[key] = [];
                      acc[key].push(w);
                      return acc;
                    },
                    {}
                  );

                  return Object.entries(byType).map(([durType, items]) => (
                    <View key={durType} style={styles.durSection}>
                      <Text style={styles.durSubTitle}>{durType}</Text>
                      {items.map((item, idx) => (
                        <View key={idx} style={styles.durItemContainer}>
                          <InfoRow label="원료명" value={item.ingrName} noBorder />
                          <InfoRow label="경고문구" value={item.warningText} noBorder />
                        </View>
                      ))}
                    </View>
                  ));
                })()}
              </>
            )}

            <Text style={styles.footerNote}>
              * 본 정보는 건강보험심사평가원(HIRA) DUR 공공데이터를 기반으로
              제공됩니다.
            </Text>
          </View>
        );
      }

      default:
        return null;
    }
  };

  return (
    <LoadingErrorView
      loading={loading}
      error={error}
      onRetry={fetchDetail}
      headerTitle="약품정보"
      loadingMessage="약품 정보를 불러오는 중..."
      safeAreaStyle={styles.safeArea}
    >
      <SafeAreaView style={styles.safeArea}>
      <Header
        title={data?.header?.itemName ?? '약품정보'}
        rightComponent={
          sourceScreen ? (
            <TouchableOpacity
              onPress={handleAddDrug}
              style={styles.addDrugButton}
              activeOpacity={0.7}
              hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
            >
              <Text style={styles.addDrugButtonText}>약 추가</Text>
            </TouchableOpacity>
          ) : undefined
        }
      />

      <View style={styles.tabBar}>
        {TABS.map((tab) => {
          const isActive = activeTab === tab;
          return (
            <TouchableOpacity
              key={tab}
              style={[styles.tabButton, isActive && styles.tabButtonActive]}
              onPress={() => setActiveTab(tab)}
            >
              <Text style={[styles.tabText, isActive && styles.tabTextActive]}>
                {tab}
              </Text>
            </TouchableOpacity>
          );
        })}
      </View>

      <ScrollView
        style={styles.scrollView}
        showsVerticalScrollIndicator={false}
        contentContainerStyle={styles.scrollContent}
      >
        <View style={styles.drugHeader}>
          <Text style={styles.drugNameText}>
            {data?.header?.itemName ?? '-'}
          </Text>
          <Text style={styles.entpNameText}>
            {data?.drugInfo?.entpName ?? '-'}
          </Text>
        </View>

        {renderContent()}
      </ScrollView>
      </SafeAreaView>
    </LoadingErrorView>
  );
}
