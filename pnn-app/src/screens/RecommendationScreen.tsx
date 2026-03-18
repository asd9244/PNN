import React from 'react';
import { Text, View, Button } from 'react-native';
import { recommendationScreenStyles as styles } from '../styles';

export default function RecommendationScreen({ navigation }: any) {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>안전 영양제 추천 (Recommendation-page)</Text>
      <Text>현재 복용 중인 처방약 기반으로 안전 성분 추천</Text>
      <Button title="추천 받기" onPress={() => navigation.navigate('RecommendationResult')} />
    </View>
  );
}
