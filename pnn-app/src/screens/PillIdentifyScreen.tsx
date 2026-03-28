import React, { useState } from 'react';
import {
  View,
  Text,
  Image,
  ScrollView,
  TouchableOpacity,
  KeyboardAvoidingView,
  Platform,
  Alert,
} from 'react-native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { RouteProp, useRoute } from '@react-navigation/native';
import { RootStackParamList } from '../navigation/AppNavigator';

import Header from '../components/Header';
import SearchBar from '../components/SearchBar';
import ActionButtons from '../components/ActionButtons';
import { pillIdentifyScreenStyles as styles } from '../styles';
import { DRUG_FORM_ICONS } from '../../assets/drugFormIcons';
import { SHAPE_ICONS } from '../../assets/shapeIcons';

type NavigationProp = NativeStackNavigationProp<RootStackParamList, 'PillIdentify'>;
type PillIdentifyRouteProp = RouteProp<RootStackParamList, 'PillIdentify'>;
interface Props {
  navigation: NavigationProp;
}

const shapes = ['원형', '타원형', '반원형', '삼각형', '사각형', '마름모', '장방형', '오각형', '육각형', '팔각형', '기타'];
const colors = ['하양', '노랑', '주황', '분홍', '빨강', '갈색', '연두', '초록', '청록', '파랑', '남색', '자주', '보라', '회색', '검정', '투명'];
const forms = ['정제', '경질캡슐', '연질캡슐', '기타'];

const formIcons = DRUG_FORM_ICONS;
const shapeIcons = SHAPE_ICONS;

// 색상 이름에 따른 헥스 코드 맵핑 헬퍼 함수
function getColorHex(colorName: string) {
  const map: Record<string, string> = {
    '하양': '#FFFFFF', '노랑': '#FFEB3B', '주황': '#FF9800', '분홍': '#E91E63',
    '빨강': '#F44336', '갈색': '#795548', '연두': '#8BC34A', '초록': '#4CAF50',
    '청록': '#009688', '파랑': '#2196F3', '남색': '#3F51B5', '자주': '#9C27B0',
    '보라': '#673AB7', '회색': '#9E9E9E', '검정': '#000000', '투명': 'transparent'
  };
  return map[colorName] || '#FFFFFF';
}

export default function PillIdentifyScreen({ navigation }: Props) {
  const route = useRoute<PillIdentifyRouteProp>();
  const sourceScreen = route.params?.sourceScreen;

  const [frontText, setFrontText] = useState('');
  const [backText, setBackText] = useState('');
  const [selectedShape, setSelectedShape] = useState<string | null>(null);
  const [selectedColor, setSelectedColor] = useState<string | null>(null);
  const [selectedForm, setSelectedForm] = useState<string | null>(null);

  const handleReset = () => {
    setFrontText('');
    setBackText('');
    setSelectedShape(null);
    setSelectedColor(null);
    setSelectedForm(null);
  };

  const handleSearch = () => {
    const hasAnyCondition =
      frontText.trim() ||
      backText.trim() ||
      selectedShape ||
      selectedColor ||
      selectedForm;

    if (!hasAnyCondition) {
      Alert.alert(
        '검색 조건 필요',
        '최소 한 가지 이상의 식별 조건(식별문자, 모양, 색상, 제형)을 입력해 주세요.'
      );
      return;
    }

    navigation.navigate('DrugList', {
      printFront: frontText.trim() || undefined,
      printBack: backText.trim() || undefined,
      drugShape: selectedShape || undefined,
      color: selectedColor || undefined,
      formulation: selectedForm || undefined,
      sourceScreen,
    });
  };

  const renderSelectionGrid = (
    data: string[],
    selectedItem: string | null,
    setItem: (item: string) => void,
    itemIcons?: Record<string, ReturnType<typeof require>>
  ) => (
    <View style={styles.gridContainer}>
      {data.map((item) => {
        const isSelected = selectedItem === item;
        const isColorCircle = colors.includes(item);
        const hasFormIcon = itemIcons && itemIcons[item];
        const circleStyle = isColorCircle ? { backgroundColor: getColorHex(item) } : {};
        const circleBorder = isColorCircle && item !== '투명' && item !== '하양' ? styles.circleBorderGray : styles.circleBorderLight;

        return (
          <TouchableOpacity
            key={item}
            onPress={() => setItem(item)}
            style={[
              styles.gridItem,
              isSelected ? styles.gridItemSelected : styles.gridItemNormal
            ]}
          >
            {hasFormIcon ? (
              <Image
                source={itemIcons![item]}
                style={styles.formIconPlaceholder}
                resizeMode="contain"
              />
            ) : (
              <View style={[styles.circlePlaceholder, circleBorder, circleStyle]} />
            )}
            <Text style={[styles.gridText, isSelected && styles.gridTextSelected]}>
              {item}
            </Text>
          </TouchableOpacity>
        );
      })}
    </View>
  );

  return (
    <View style={styles.safeArea}>
      <Header title="식별검색" />

      <KeyboardAvoidingView 
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        style={styles.container}
      >
        <ScrollView style={styles.scrollView} contentContainerStyle={styles.scrollContent}>
          <Text style={styles.descriptionText}>
            약의 색이나 모양, 각인 문자로 정확하게 검색할 수 있습니다.
          </Text>

          {/* 1. 식별문자 영역 */}
          <Text style={styles.sectionTitle}>식별문자</Text>
          <View style={styles.searchBarRow}>
            <View style={styles.searchBarWrapperLeft}>
              <SearchBar 
                placeholder="앞면 식별문자" 
                value={frontText}
                onChangeText={setFrontText}
                style={styles.searchBarMarginClear}
              />
            </View>
            <View style={styles.searchBarWrapperRight}>
              <SearchBar 
                placeholder="뒷면 식별문자" 
                value={backText}
                onChangeText={setBackText}
                style={styles.searchBarMarginClear}
              />
            </View>
          </View>

          {/* 2. 모양 영역 */}
          <View style={styles.sectionContainer}>
            <Text style={styles.sectionTitle}>모양 <Text style={styles.selectionNote}>(1개 선택)</Text></Text>
            {renderSelectionGrid(shapes, selectedShape, setSelectedShape, shapeIcons)}
          </View>

          {/* 3. 색상 영역 */}
          <View style={styles.sectionContainer}>
            <Text style={styles.sectionTitle}>색상 <Text style={styles.selectionNote}>(1개 선택)</Text></Text>
            {renderSelectionGrid(colors, selectedColor, setSelectedColor)}
          </View>

          {/* 4. 제형 영역 */}
          <View style={styles.sectionContainer}>
            <Text style={styles.sectionTitle}>제형 <Text style={styles.selectionNote}>(1개 선택)</Text></Text>
            {renderSelectionGrid(forms, selectedForm, setSelectedForm, formIcons)}
          </View>

          <View style={styles.bottomSpacing} />
        </ScrollView>
      </KeyboardAvoidingView>

      {/* 하단 고정 액션 버튼 */}
      <ActionButtons onReset={handleReset} onSubmit={handleSearch} />
    </View>
  );
}
