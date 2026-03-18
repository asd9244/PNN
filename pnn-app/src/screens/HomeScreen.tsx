import React, { useState } from 'react';
import { View, Text, SafeAreaView, ScrollView } from 'react-native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import SearchBar from '../components/SearchBar';
import ActionCard from '../components/ActionCard';
import { homeScreenStyles as styles } from '../styles';
import { RootStackParamList } from '../navigation/AppNavigator';

type HomeScreenNavigationProp = NativeStackNavigationProp<RootStackParamList, 'Home'>;

interface Props {
  navigation: HomeScreenNavigationProp;
}

export default function HomeScreen({ navigation }: Props) {
  const [searchText, setSearchText] = useState('');

  const handleSearch = () => {
    const trimmed = searchText.trim();
    if (trimmed) {
      navigation.navigate('DrugList', { itemName: trimmed });
    }
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <ScrollView style={styles.container} contentContainerStyle={styles.scrollContent}>
        
        {/* 헤더 영역 (로고 및 타이틀) */}
        <View style={styles.header}>
          <View style={styles.logoContainer}>
            <Text style={styles.logoText}>PNN</Text>
          </View>
          <Text style={styles.title}>
            가장 안전한{'\n'}나만의 영양제 찾기
          </Text>
          <Text style={styles.subtitle}>
            처방약과 영양제의 충돌을 미리 예방하세요
          </Text>
        </View>

        {/* 메인 검색창 - 약품명으로 바로 검색 */}
        <SearchBar
          placeholder="어떤 약을 찾으시나요?"
          editable={true}
          value={searchText}
          onChangeText={setSearchText}
          onSearch={handleSearch}
          style={styles.searchBar}
        />

        <View style={styles.spacing} />

        {/* 핵심 기능 버튼 영역 */}
        <View>
          <Text style={styles.sectionTitle}>주요 서비스</Text>

          <ActionCard
            title="상세검색"
            description="약품명, 성분명으로 자세한 정보를 검색합니다."
            iconName="search-outline"
            onPress={() => navigation.navigate('DrugSearch')}
          />

          <ActionCard
            title="식별검색"
            description="모양, 색상, 마크로 모르는 약을 찾아줍니다."
            iconName="scan-outline"
            onPress={() => navigation.navigate('PillIdentify')}
          />

          <ActionCard
            title="약X영양제 비교"
            description="현재 먹고 있는 처방약과 영양제가 충돌하는지 검사합니다."
            iconName="shield-checkmark-outline"
            onPress={() => navigation.navigate('InteractionCheck')}
          />

          <ActionCard
            title="영양제 추천"
            description="내 처방약에 맞는 안전하고 효과적인 영양제를 추천받습니다."
            iconName="sparkles-outline"
            onPress={() => navigation.navigate('Recommendation')}
          />
        </View>

      </ScrollView>
    </SafeAreaView>
  );
}
