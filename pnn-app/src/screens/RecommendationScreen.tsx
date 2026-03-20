import React from "react";
import {
  View,
  Text,
  SafeAreaView,
  ScrollView,
  TouchableOpacity,
} from "react-native";
import {useSafeAreaInsets} from "react-native-safe-area-context";
import {NativeStackNavigationProp} from "@react-navigation/native-stack";
import {RootStackParamList} from "../navigation/AppNavigator";
import {useDrugStore} from "../store/useDrugStore";
import { getRecommendations } from '../api/ocr';
import { ActivityIndicator, TextInput, Alert } from 'react-native';
import { Ionicons } from "@expo/vector-icons";

import Header from "../components/Header";
import ActionCard from "../components/ActionCard";
import {inputScreenStyles as styles} from "../styles";

type NavigationProp = NativeStackNavigationProp<
  RootStackParamList,
  "Recommendation"
>;

interface Props {
  navigation: NavigationProp;
}

export default function RecommendationScreen({navigation}: Props) {
  const recommendationDrugs = useDrugStore(
    (state) => state.recommendationDrugs,
  );
  const removeRecommendationDrug = useDrugStore(
    (state) => state.removeRecommendationDrug,
  );
  const drugCount = recommendationDrugs.length;
  const insets = useSafeAreaInsets();

  const [condition, setCondition] = React.useState('');
  const [isRecommending, setIsRecommending] = React.useState(false);

  const canRecommend = drugCount > 0;

  const handleRecommend = async () => {
    if (!canRecommend) return;
    
    setIsRecommending(true);
    try {
      const response = await getRecommendations({
        drugIds: recommendationDrugs.map(d => d.drugId),
        condition: condition.trim() || undefined,
      });
      
      navigation.navigate("RecommendationResult", { result: response });
    } catch (error: any) {
      Alert.alert('추천 실패', error.message || '영양제 추천 중 오류가 발생했습니다.');
    } finally {
      setIsRecommending(false);
    }
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <Header title="영양제 추천" />

      <ScrollView
        style={styles.container}
        contentContainerStyle={[
          styles.scrollContent,
          {paddingBottom: 24 + insets.bottom},
        ]}
      >
        {/* 헤더 설명 */}
        <View style={styles.header}>
          <Text style={styles.subtitle}>
            복용중인 약과 시너지 효과가 있는 영양제를 추천받아보세요.
          </Text>
        </View>

        {/* 처방약 정보 입력 */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>처방약 정보 입력</Text>
          <Text style={styles.sectionSubtitle}>
            현재 복용 중인 약을 모두 추가해주세요.
          </Text>
          <View style={styles.buttonRow}>
            <ActionCard
              title="상세검색"
              iconName="search-outline"
              onPress={() =>
                navigation.navigate("DrugSearch", {
                  sourceScreen: "Recommendation",
                })
              }
              compact
              iconColor="#2563EB"
              iconBgColor="#DBEAFE"
              style={{flex: 1}}
            />
            <ActionCard
              title="식별검색"
              iconName="scan-outline"
              onPress={() =>
                navigation.navigate("PillIdentify", {
                  sourceScreen: "Recommendation",
                })
              }
              compact
              iconColor="#7C3AED"
              iconBgColor="#EDE9FE"
              style={{flex: 1}}
            />
          </View>
        </View>

        {/* 기저 질환 입력 */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>기저 질환 / 건강 고민 (선택)</Text>
          <Text style={styles.sectionSubtitle}>
            고민인 증상이나 질환을 입력하시면 맞춤 추천에 도움이 됩니다.
          </Text>
          <TextInput
            style={{
              backgroundColor: '#FFFFFF',
              borderRadius: 12,
              padding: 16,
              fontSize: 15,
              color: '#111827',
              borderWidth: 1,
              borderColor: '#E5E7EB',
            }}
            placeholder="예) 고혈압, 당뇨, 최근 피로감 등"
            value={condition}
            onChangeText={setCondition}
            maxLength={50}
          />
        </View>

        {/* 처방약 요약 */}
        <View style={styles.summaryCard}>
          <View style={styles.summaryCardHeader}>
            <View style={styles.summaryCardBarGreen} />
            <Text style={styles.summaryCardTitle}>
              처방약 요약 ({drugCount})
            </Text>
          </View>

          {drugCount === 0 ? (
            <Text style={styles.summaryCardPlaceholder}>
              처방약을 추가해주세요.
            </Text>
          ) : (
            <View style={styles.drugListContainer}>
              {recommendationDrugs.map((drug) => (
                <View key={drug.drugId} style={styles.drugListItem}>
                  <View style={styles.drugListItemText}>
                    <Text style={styles.drugItemName} numberOfLines={1}>
                      {drug.itemName}
                    </Text>
                    <View style={{ marginTop: 4 }}>
                      {(drug.ingredientNamesEng ?? []).length > 0
                        ? (drug.ingredientNamesEng ?? []).map((name, i) => (
                            <Text
                              key={`${drug.drugId}-ing-${i}`}
                              style={styles.drugEntpName}
                              numberOfLines={2}
                            >
                              • {name}
                            </Text>
                          ))
                        : (
                            <Text style={styles.drugEntpName} numberOfLines={1}>
                              성분 정보 없음
                            </Text>
                          )}
                    </View>
                  </View>
                  <TouchableOpacity
                    style={styles.removeDrugButton}
                    onPress={() => removeRecommendationDrug(drug.drugId)}
                    hitSlop={{top: 10, bottom: 10, left: 10, right: 10}}
                  >
                    <Ionicons name="close" size={20} color="#9CA3AF" />
                  </TouchableOpacity>
                </View>
              ))}
            </View>
          )}
        </View>

        {/* 맞춤 영양제 추천받기 버튼 */}
        <TouchableOpacity
          style={[
            styles.bottomButton,
            canRecommend && {backgroundColor: "#059669"},
          ]}
          onPress={handleRecommend}
          disabled={!canRecommend || isRecommending}
          activeOpacity={0.7}
        >
          {isRecommending ? (
            <ActivityIndicator size="small" color="#FFFFFF" />
          ) : (
            <Text
              style={[
                styles.bottomButtonText,
                canRecommend && {color: "#FFFFFF"},
              ]}
            >
              맞춤 영양제 추천받기
            </Text>
          )}
        </TouchableOpacity>
      </ScrollView>
    </SafeAreaView>
  );
}
