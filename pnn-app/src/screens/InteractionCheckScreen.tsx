import React, { useState } from 'react';
import {
  View,
  Text,
  SafeAreaView,
  ScrollView,
  TouchableOpacity,
} from 'react-native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { RootStackParamList } from '../navigation/AppNavigator';

import Header from '../components/Header';
import ActionCard from '../components/ActionCard';
import { interactionCheckScreenStyles as styles } from '../styles';

type NavigationProp = NativeStackNavigationProp<RootStackParamList, 'InteractionCheck'>;

interface Props {
  navigation: NavigationProp;
}

export default function InteractionCheckScreen({ navigation }: Props) {
  const [drugCount] = useState(0);

  const canCompare = drugCount > 0;

  return (
    <SafeAreaView style={styles.safeArea}>
      <Header title="약X영양제 비교" />

      <ScrollView style={styles.container} contentContainerStyle={styles.scrollContent}>
        {/* 헤더 설명 */}
        <View style={styles.header}>
          <Text style={styles.subtitle}>
            복용중인 약과 영양제 사이의 상호작용을 알아보세요.
          </Text>
        </View>

        {/* 약 정보 입력 */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>약 정보 입력</Text>
          <Text style={styles.sectionSubtitle}>
            복용 중인 약을 모두 추가해주세요.
          </Text>
          <View style={styles.buttonRow}>
            <ActionCard
              title="상세검색"
              iconName="search-outline"
              onPress={() => navigation.navigate('DrugSearch')}
              compact
              iconColor="#2563EB"
              iconBgColor="#DBEAFE"
              style={{ flex: 1 }}
            />
            <ActionCard
              title="식별검색"
              iconName="scan-outline"
              onPress={() => navigation.navigate('PillIdentify')}
              compact
              iconColor="#7C3AED"
              iconBgColor="#EDE9FE"
              style={{ flex: 1 }}
            />
          </View>
        </View>

        {/* 영양제 정보 입력 */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>영양제 정보 입력</Text>
          <Text style={styles.sectionSubtitle}>
            영양제 사진을 추가해주세요.
          </Text>
          <View style={styles.buttonRow}>
            <ActionCard
              title="직접 촬영"
              iconName="camera-outline"
              onPress={() => {}}
              compact
              iconColor="#EA580C"
              iconBgColor="#FFEDD5"
              style={{ flex: 1 }}
            />
            <ActionCard
              title="앨범에서 선택"
              iconName="images-outline"
              onPress={() => {}}
              compact
              iconColor="#6B7280"
              iconBgColor="#F3F4F6"
              style={{ flex: 1 }}
            />
          </View>
        </View>

        {/* 약 정보 요약 */}
        <View style={styles.summaryCard}>
          <View style={styles.summaryCardHeader}>
            <View style={styles.summaryCardBarBlue} />
            <Text style={styles.summaryCardTitle}>약 정보 요약 ({drugCount})</Text>
          </View>
          <Text style={styles.summaryCardPlaceholder}>
            복용 중인 약을 추가해주세요.
          </Text>
        </View>

        {/* 영양제 정보 요약 */}
        <View style={styles.summaryCard}>
          <View style={styles.summaryCardHeader}>
            <View style={styles.summaryCardBarOrange} />
            <Text style={styles.summaryCardTitle}>영양제 정보 요약</Text>
          </View>
          <Text style={styles.summaryCardPlaceholder}>
            영양제를 추가해주세요.
          </Text>
        </View>

        {/* 비교 분석 버튼 */}
        <TouchableOpacity
          style={[styles.bottomButton, canCompare && { backgroundColor: '#9333EA' }]}
          onPress={() => canCompare && navigation.navigate('InteractionResult')}
          disabled={!canCompare}
          activeOpacity={0.7}
        >
          <Text style={[styles.bottomButtonText, canCompare && { color: '#FFFFFF' }]}>
            ⇆ 비교 분석하기
          </Text>
        </TouchableOpacity>
      </ScrollView>
    </SafeAreaView>
  );
}
