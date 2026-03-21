import React from 'react';
import {
  Text,
  View,
  SafeAreaView,
  ScrollView,
  TouchableOpacity,
  Linking,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { RouteProp, useRoute } from '@react-navigation/native';
import { RootStackParamList } from '../navigation/AppNavigator';
import { productSearchLinksScreenStyles as styles } from '../styles';
import Header from '../components/Header';
import { getProductSearchLinks } from '../utils/productSearchLinks';
import { Ionicons } from '@expo/vector-icons';

type ProductSearchLinksRouteProp = RouteProp<RootStackParamList, 'ProductSearchLinks'>;

export default function ProductSearchLinksScreen() {
  const route = useRoute<ProductSearchLinksRouteProp>();
  const { nutrients } = route.params;
  const insets = useSafeAreaInsets();

  const openUrl = async (url: string) => {
    try {
      await Linking.openURL(url);
    } catch (error) {
      console.error('링크 열기 실패:', error);
    }
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <Header title="제품 검색 링크" />
      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={[styles.scrollContent, { paddingBottom: 24 + insets.bottom }]}
      >
        <Text style={styles.intro}>
          추천된 모든 성분에 대해 쿠팡·iHerb 검색 링크를 한 화면에서 확인할 수 있습니다.
        </Text>

        {nutrients.map((nutrient, nutrientIndex) => {
          const links = getProductSearchLinks(nutrient.nameKr, nutrient.nameEn);
          return (
            <View key={`${nutrient.nameKr}-${nutrientIndex}`} style={styles.nutrientSection}>
              <Text style={styles.sectionIngredientName}>{nutrient.nameKr}</Text>
              <Text style={styles.sectionIngredientNameEn}>{nutrient.nameEn}</Text>

              {links.map((item) => (
                <TouchableOpacity
                  key={`${nutrientIndex}-${item.label}`}
                  style={styles.linkRow}
                  onPress={() => openUrl(item.url)}
                  activeOpacity={0.7}
                >
                  <Text style={styles.linkLabel}>{item.label}</Text>
                  <Ionicons name="open-outline" size={22} color="#8A2BE2" />
                </TouchableOpacity>
              ))}
            </View>
          );
        })}
      </ScrollView>
    </SafeAreaView>
  );
}
