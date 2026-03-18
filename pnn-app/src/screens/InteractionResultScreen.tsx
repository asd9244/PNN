import React from 'react';
import { Text, View, Button } from 'react-native';
import { interactionResultScreenStyles as styles } from '../styles';

export default function InteractionResultScreen({ navigation }: any) {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>충돌 검사 결과 (InteractionResult-page)</Text>
      <Text>SAFE / CAUTION / WARNING 뱃지 표시 영역</Text>
      <Button title="홈으로 돌아가기" onPress={() => navigation.popToTop()} />
    </View>
  );
}
