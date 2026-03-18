import React from 'react';
import { Text, View, Button } from 'react-native';
import { interactionAddDrugScreenStyles as styles } from '../styles';

export default function InteractionAddDrugScreen({ navigation }: any) {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>처방약 추가 (InteractionCheck-addDrug)</Text>
      <Button title="완료 후 돌아가기" onPress={() => navigation.goBack()} />
    </View>
  );
}
