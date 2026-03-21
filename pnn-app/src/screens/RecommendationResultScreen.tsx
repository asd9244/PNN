import React from 'react';
import { Text, View, SafeAreaView, ScrollView, TouchableOpacity } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { RouteProp, useNavigation, useRoute } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { RootStackParamList } from '../navigation/AppNavigator';
import { recommendationResultScreenStyles as styles } from '../styles';
import Header from '../components/Header';
import { RecommendationResponse, RecommendedNutrient } from '../api/ocr';
import { Ionicons } from '@expo/vector-icons';

type RecommendationResultRouteProp = RouteProp<RootStackParamList, 'RecommendationResult'>;
type RecommendationResultNavigationProp = NativeStackNavigationProp<
  RootStackParamList,
  'RecommendationResult'
>;

export default function RecommendationResultScreen() {
  const route = useRoute<RecommendationResultRouteProp>();
  const navigation = useNavigation<RecommendationResultNavigationProp>();
  const result: RecommendationResponse = route.params?.result;
  const insets = useSafeAreaInsets();

  const renderNutrientCard = (nutrient: RecommendedNutrient, index: number) => {
    return (
      <View key={index} style={styles.card}>
        <View style={styles.cardHeader}>
          <Text style={styles.cardTitle}>{nutrient.nameKr}</Text>
          <Text style={styles.cardSubtitle}>{nutrient.nameEn}</Text>
        </View>

        <View style={styles.cardBody}>
          <View style={styles.reasonBlock}>
            <Text style={styles.blockLabel}>추천 사유</Text>
            <Text style={styles.blockText}>{nutrient.reason}</Text>
          </View>
          
          <View style={styles.precautionBlock}>
            <Text style={styles.blockLabel}>주의사항</Text>
            <Text style={styles.blockText}>{nutrient.precaution}</Text>
          </View>
        </View>
      </View>
    );
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <Header title="추천 결과" />
      
      <ScrollView 
        style={styles.scrollView}
        contentContainerStyle={[styles.scrollContent, { paddingBottom: 24 + insets.bottom }]}
      >
        <Text style={styles.headerTitle}>맞춤 영양제 추천</Text>
        
        {/* 상호작용 및 분석 의견 */}
        <View style={styles.analysisCard}>
          <View style={styles.analysisHeader}>
            <Ionicons name="analytics-outline" size={20} color="#059669" />
            <Text style={styles.analysisTitle}>AI 분석 의견</Text>
          </View>
          <Text style={styles.analysisText}>
            {result?.interactionAnalysis || '입력하신 처방약과 기저 질환을 분석하여 가장 안전하고 적합한 영양 성분을 선별했습니다.'}
          </Text>
        </View>

        {/* 추천 성분 리스트 */}
        {result?.recommendedNutrients && result.recommendedNutrients.length > 0 ? (
          result.recommendedNutrients.map((item, index) => renderNutrientCard(item, index))
        ) : (
          <View style={styles.emptyCard}>
            <Ionicons name="information-circle-outline" size={48} color="#9CA3AF" />
            <Text style={styles.emptyTitle}>추천 가능한 성분이 없습니다.</Text>
            <Text style={styles.emptyText}>
              입력하신 약물과의 안전성을 최우선으로 고려한 결과, 현재 확실하게 추천해 드릴 수 있는 성분이 부족합니다.
            </Text>
          </View>
        )}

        {/* 하단 유의사항 */}
        <View style={styles.disclaimerContainer}>
          <Text style={styles.disclaimerText}>
            ※ 본 추천은 AI에 의한 참고용 정보이며, 의학적 진단이나 치료를 대신할 수 없습니다. 
            새로운 영양제를 복용하기 전에는 반드시 담당 의사 또는 약사와 상담하시기 바랍니다.
          </Text>
        </View>

        {result?.recommendedNutrients && result.recommendedNutrients.length > 0 ? (
          <View style={styles.bottomProductSearchSection}>
            <TouchableOpacity
              style={styles.productSearchButton}
              onPress={() =>
                navigation.navigate('ProductSearchLinks', {
                  nutrients: result.recommendedNutrients.map((n) => ({
                    nameKr: n.nameKr,
                    nameEn: n.nameEn,
                  })),
                })
              }
              activeOpacity={0.8}
            >
              <Text style={styles.productSearchButtonText}>이 성분이 포함된 제품 찾아보기</Text>
            </TouchableOpacity>
          </View>
        ) : null}
      </ScrollView>
    </SafeAreaView>
  );
}
