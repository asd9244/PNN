import React from 'react';
import { Text, View, Button } from 'react-native';
import { recommendationResultScreenStyles as styles } from '../styles';

export default function RecommendationResultScreen({ navigation }: any) {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>추천 결과 (Recommendation-Result-page)</Text>
      <Text>금기 성분 경고 및 안전 성분 2가지 추천</Text>
      <Button title="처음으로" onPress={() => navigation.popToTop()} />
    </View>
  );
}
