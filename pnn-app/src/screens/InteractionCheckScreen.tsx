import React, { useState } from 'react';
import {
  View,
  Text,
  SafeAreaView,
  ScrollView,
  TouchableOpacity,
  Alert,
  ActivityIndicator,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import * as ImagePicker from 'expo-image-picker';
import { RootStackParamList } from '../navigation/AppNavigator';
import { useDrugStore, Supplement } from '../store/useDrugStore';
import { Ionicons } from '@expo/vector-icons';

import Header from '../components/Header';
import ActionCard from '../components/ActionCard';
import { inputScreenStyles as styles } from '../styles';
import { analyzeSupplementImage, InteractionCompareRequest, compareInteractions } from '../api/ocr';

type NavigationProp = NativeStackNavigationProp<RootStackParamList, 'InteractionCheck'>;

interface Props {
  navigation: NavigationProp;
}

export default function InteractionCheckScreen({ navigation }: Props) {
  const interactionDrugs = useDrugStore((state) => state.interactionDrugs);
  const removeInteractionDrug = useDrugStore((state) => state.removeInteractionDrug);
  
  const interactionSupplements = useDrugStore((state) => state.interactionSupplements);
  const addInteractionSupplement = useDrugStore((state) => state.addInteractionSupplement);
  const removeInteractionSupplement = useDrugStore((state) => state.removeInteractionSupplement);

  const drugCount = interactionDrugs.length;
  const supplementCount = interactionSupplements.length;
  const insets = useSafeAreaInsets();

  const [isProcessingImage, setIsProcessingImage] = useState(false);
  const [isComparing, setIsComparing] = useState(false);

  // 약물과 영양제가 둘 다 하나 이상 있거나 영양제만 여러 개 있거나 등등의 조건을 설정할 수 있습니다.
  const canCompare = drugCount > 0 && supplementCount > 0;

  const handleTakePhoto = async () => {
    if (supplementCount >= 3) {
      Alert.alert('안내', '최대 3개까지만 추가할 수 있습니다.');
      return;
    }

    const { status } = await ImagePicker.requestCameraPermissionsAsync();
    if (status !== 'granted') {
      Alert.alert('권한 필요', '카메라 접근 권한이 필요합니다.');
      return;
    }

    const result = await ImagePicker.launchCameraAsync({
      allowsEditing: true,
      aspect: [4, 3],
      quality: 0.8,
    });

    if (!result.canceled && result.assets && result.assets.length > 0) {
      processSupplementImage(result.assets[0].uri);
    }
  };

  const handlePickImage = async () => {
    if (supplementCount >= 3) {
      Alert.alert('안내', '최대 3개까지만 추가할 수 있습니다.');
      return;
    }

    const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (status !== 'granted') {
      Alert.alert('권한 필요', '사진 앨범 접근 권한이 필요합니다.');
      return;
    }

    const result = await ImagePicker.launchImageLibraryAsync({
      allowsEditing: true,
      aspect: [4, 3],
      quality: 0.8,
    });

    if (!result.canceled && result.assets && result.assets.length > 0) {
      processSupplementImage(result.assets[0].uri);
    }
  };

  const processSupplementImage = async (uri: string) => {
    setIsProcessingImage(true);
    try {
      const supplementData = await analyzeSupplementImage(uri);
      // 서버에서 파싱된 결과를 받아와서 Store에 추가
      addInteractionSupplement({
        ...supplementData,
        id: Math.random().toString(36).substr(2, 9),
      });
    } catch (error) {
      Alert.alert('분석 실패', '영양제 성분 분석 중 오류가 발생했습니다. 다시 촬영해주세요.');
    } finally {
      setIsProcessingImage(false);
    }
  };

  const handleCompare = async () => {
    if (!canCompare) return;

    setIsComparing(true);
    try {
      const requestData: InteractionCompareRequest = {
        drugIds: interactionDrugs.map((d) => d.drugId),
        supplements: interactionSupplements.map((s) => ({
          name: s.name,
          nutrients: s.ingredients.map(ing => ({
            name: ing.name,
            amount: ing.amount,
            unit: ing.unit
          })),
        })),
      };

      // API 호출 (현재는 Mock)
      const result = await compareInteractions(requestData);

      // 결과를 InteractionResultScreen으로 넘겨줌
      navigation.navigate('InteractionResult', { result });
    } catch (error) {
      Alert.alert('비교 실패', '분석 중 오류가 발생했습니다.');
    } finally {
      setIsComparing(false);
    }
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <Header title="약X영양제 비교" />

      <ScrollView
          style={styles.container}
          contentContainerStyle={[
            styles.scrollContent,
            { paddingBottom: 24 + insets.bottom },
          ]}
        >
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
              onPress={() => navigation.navigate('DrugSearch', { sourceScreen: 'InteractionCheck' })}
              compact
              iconColor="#2563EB"
              iconBgColor="#DBEAFE"
              style={{ flex: 1 }}
            />
            <ActionCard
              title="식별검색"
              iconName="scan-outline"
              onPress={() => navigation.navigate('PillIdentify', { sourceScreen: 'InteractionCheck' })}
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
            영양제 사진을 추가해주세요. (최대 3장)
          </Text>
          <View style={styles.buttonRow}>
            <ActionCard
              title="직접 촬영"
              iconName="camera-outline"
              onPress={handleTakePhoto}
              compact
              iconColor="#EA580C"
              iconBgColor="#FFEDD5"
              style={{ flex: 1 }}
              disabled={isProcessingImage || supplementCount >= 3}
            />
            <ActionCard
              title="앨범에서 선택"
              iconName="images-outline"
              onPress={handlePickImage}
              compact
              iconColor="#6B7280"
              iconBgColor="#F3F4F6"
              style={{ flex: 1 }}
              disabled={isProcessingImage || supplementCount >= 3}
            />
          </View>
        </View>

        {/* 약 정보 요약 */}
        <View style={styles.summaryCard}>
          <View style={styles.summaryCardHeader}>
            <View style={styles.summaryCardBarBlue} />
            <Text style={styles.summaryCardTitle}>약 정보 요약 ({drugCount})</Text>
          </View>
          
          {drugCount === 0 ? (
            <Text style={styles.summaryCardPlaceholder}>
              복용 중인 약을 추가해주세요.
            </Text>
          ) : (
            <View style={styles.drugListContainer}>
              {interactionDrugs.map((drug) => (
                <View key={drug.drugId} style={styles.drugListItem}>
                  <View style={styles.drugListItemText}>
                    <TouchableOpacity
                      onPress={() =>
                        navigation.navigate('DrugDetail', {
                          drugId: drug.drugId,
                          sourceScreen: 'InteractionCheck',
                        })
                      }
                      activeOpacity={0.7}
                      hitSlop={{ top: 6, bottom: 6, left: 0, right: 0 }}
                    >
                      <Text
                        style={[styles.drugItemName, styles.drugItemNameLink]}
                        numberOfLines={1}
                      >
                        {drug.itemName}
                      </Text>
                    </TouchableOpacity>
                    {(drug.ingredientNamesEng ?? []).length > 0 ? (
                      <View style={{ marginTop: 4 }}>
                        {(drug.ingredientNamesEng ?? []).map((name, i) => (
                          <Text
                            key={`${drug.drugId}-ing-${i}`}
                            style={styles.drugEntpName}
                            numberOfLines={2}
                          >
                            • {name}
                          </Text>
                        ))}
                      </View>
                    ) : null}
                  </View>
                  <TouchableOpacity 
                    style={styles.removeDrugButton} 
                    onPress={() => removeInteractionDrug(drug.drugId)}
                    hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
                  >
                    <Ionicons name="close-circle" size={20} color="#9CA3AF" />
                  </TouchableOpacity>
                </View>
              ))}
            </View>
          )}
        </View>

        {/* 영양제 정보 요약 */}
        <View style={styles.summaryCard}>
          <View style={styles.summaryCardHeader}>
            <View style={styles.summaryCardBarOrange} />
            <Text style={styles.summaryCardTitle}>영양제 정보 요약 ({supplementCount}/3)</Text>
          </View>
          
          {supplementCount === 0 && !isProcessingImage ? (
            <Text style={styles.summaryCardPlaceholder}>
              영양제를 추가해주세요.
            </Text>
          ) : (
            <View style={styles.drugListContainer}>
              {interactionSupplements.map((sup, index) => (
                <View key={sup.id} style={styles.drugListItem}>
                  <View style={styles.drugListItemText}>
                    <Text style={styles.drugItemName} numberOfLines={1}>
                      분석완료된 영양제 {index + 1}
                    </Text>
                    <View style={{ marginTop: 4 }}>
                      {sup.ingredients.length > 0
                        ? sup.ingredients.map((ing, i) => {
                            const amountStr = ing.amount != null ? ` ${ing.amount}` : '';
                            const unitStr = ing.unit ? ` ${ing.unit}` : '';
                            const label = `${ing.name}${amountStr}${unitStr}`.trim();
                            return label ? (
                              <Text key={`${sup.id}-${i}-${ing.name}`} style={styles.drugEntpName} numberOfLines={1}>
                                • {label}
                              </Text>
                            ) : null;
                          })
                        : null}
                    </View>
                  </View>
                  <TouchableOpacity 
                    style={styles.removeDrugButton} 
                    onPress={() => removeInteractionSupplement(sup.id)}
                    hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
                  >
                    <Ionicons name="close-circle" size={20} color="#9CA3AF" />
                  </TouchableOpacity>
                </View>
              ))}
              
              {isProcessingImage && (
                <View style={[styles.drugListItem, { justifyContent: 'center', borderBottomWidth: 0 }]}>
                  <ActivityIndicator size="small" color="#EA580C" />
                  <Text style={{ marginLeft: 8, color: '#6B7280', fontSize: 13 }}>성분 분석 중...</Text>
                </View>
              )}
            </View>
          )}
          {(supplementCount > 0 || isProcessingImage) && (
            <Text style={styles.summaryCardNote}>ai 모델이 인식 가능한 성분만 표시됩니다.</Text>
          )}
        </View>

        {/* 비교 분석 버튼 */}
        <TouchableOpacity
          style={[styles.bottomButton, canCompare && { backgroundColor: '#9333EA' }]}
          onPress={handleCompare}
          disabled={!canCompare || isComparing}
          activeOpacity={0.7}
        >
          {isComparing ? (
            <ActivityIndicator size="small" color="#FFFFFF" />
          ) : (
            <Text style={[styles.bottomButtonText, canCompare && { color: '#FFFFFF' }]}>
              ⇆ 비교 분석하기
            </Text>
          )}
        </TouchableOpacity>
      </ScrollView>
    </SafeAreaView>
  );
}
