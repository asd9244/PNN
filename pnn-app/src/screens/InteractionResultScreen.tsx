import React from 'react';
import { Text, View, Button, SafeAreaView, ScrollView } from 'react-native';
import { interactionResultScreenStyles as styles } from '../styles';
import { RouteProp, useRoute } from '@react-navigation/native';
import { RootStackParamList } from '../navigation/AppNavigator';
import Header from '../components/Header';

type InteractionResultRouteProp = RouteProp<RootStackParamList, 'InteractionResult'>;

export default function InteractionResultScreen({ navigation }: any) {
  const route = useRoute<InteractionResultRouteProp>();
  const result = route.params?.result;

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: '#F9FAFB' }}>
      <Header title="분석 결과" />
      <ScrollView contentContainerStyle={{ padding: 20 }}>
        <Text style={{ fontSize: 20, fontWeight: 'bold', marginBottom: 20 }}>
          충돌 검사 결과
        </Text>
        
        <View style={{ backgroundColor: '#fff', padding: 16, borderRadius: 12, marginBottom: 16 }}>
          <Text style={{ fontWeight: 'bold', fontSize: 16, marginBottom: 8 }}>분석 결과 요약</Text>
          <Text>{result?.interactions?.length ? '상호작용이 감지된 항목이 있습니다.' : '특이한 상호작용이 발견되지 않았습니다.'}</Text>
        </View>

        {result?.interactions && result.interactions.map((item: any, index: number) => {
          let bgColor = '#F3F4F6';
          let textColor = '#374151';
          let icon = 'ℹ️';
          
          if (item.level === 'WARNING') {
            bgColor = '#FEF2F2';
            textColor = '#B91C1C';
            icon = '🚨';
          } else if (item.level === 'CAUTION') {
            bgColor = '#FFFBEB';
            textColor = '#B45309';
            icon = '⚠️';
          } else if (item.level === 'SYNERGY') {
            bgColor = '#EFF6FF';
            textColor = '#1D4ED8';
            icon = '✨';
          } else if (item.level === 'SAFE') {
            bgColor = '#ECFDF5';
            textColor = '#047857';
            icon = '✅';
          }

          return (
            <View key={index} style={{ backgroundColor: bgColor, padding: 16, borderRadius: 12, marginBottom: 16 }}>
              <Text style={{ fontWeight: 'bold', fontSize: 16, color: textColor, marginBottom: 8 }}>
                {icon} {item.nutrient} × {item.contraindicatedDrugIngredient}
              </Text>
              <Text style={{ color: '#374151', marginBottom: 12, lineHeight: 20 }}>
                {item.description}
              </Text>
              <View style={{ backgroundColor: 'rgba(255,255,255,0.6)', padding: 12, borderRadius: 8 }}>
                <Text style={{ color: '#111827', fontWeight: '500' }}>💡 행동 지침</Text>
                <Text style={{ color: '#374151', marginTop: 4 }}>{item.actionGuide}</Text>
              </View>
            </View>
          );
        })}

        <Button title="홈으로 돌아가기" onPress={() => navigation.popToTop()} />
      </ScrollView>
    </SafeAreaView>
  );
}
