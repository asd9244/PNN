import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';

// Screens
import HomeScreen from '../screens/HomeScreen';
import DrugSearchScreen from '../screens/DrugSearchScreen';
import DrugListScreen from '../screens/DrugListScreen';
import DrugDetailScreen from '../screens/DrugDetailScreen';
import InteractionCheckScreen from '../screens/InteractionCheckScreen';
import InteractionAddDrugScreen from '../screens/InteractionAddDrugScreen';
import InteractionResultScreen from '../screens/InteractionResultScreen';
import RecommendationScreen from '../screens/RecommendationScreen';
import RecommendationResultScreen from '../screens/RecommendationResultScreen';
import PillIdentifyScreen from '../screens/PillIdentifyScreen';

export type SourceScreenType = 'InteractionCheck' | 'Recommendation';

export type RootStackParamList = {
  Home: undefined;
  DrugSearch: { sourceScreen?: SourceScreenType } | undefined;
  PillIdentify: { sourceScreen?: SourceScreenType } | undefined;
  DrugList:
    | {
        itemName?: string;
        entpName?: string;
        ingredient?: string;
        printFront?: string;
        printBack?: string;
        drugShape?: string;
        color?: string;
        line?: string;
        formulation?: string;
        sourceScreen?: SourceScreenType;
      }
    | undefined;
  DrugDetail: { drugId: number; sourceScreen?: SourceScreenType };
  InteractionCheck: undefined;
  InteractionAddDrug: undefined;
  InteractionResult: { result: any } | undefined;
  Recommendation: undefined;
  RecommendationResult: { result: any } | undefined;
};

const Stack = createNativeStackNavigator<RootStackParamList>();

export default function AppNavigator() {
  return (
    <NavigationContainer>
      <Stack.Navigator initialRouteName="Home">
        <Stack.Screen name="Home" component={HomeScreen} options={{ title: '홈' }} />
        
        {/* 약 검색 및 식별 */}
        <Stack.Screen name="DrugSearch" component={DrugSearchScreen} options={{ title: '상세검색' }} />
        <Stack.Screen name="PillIdentify" component={PillIdentifyScreen} options={{ title: '식별검색' }} />
        <Stack.Screen name="DrugList" component={DrugListScreen} options={{ title: '약품 목록' }} />
        <Stack.Screen name="DrugDetail" component={DrugDetailScreen} options={{ title: '약 상세 정보' }} />
        
        {/* 상호작용 충돌 검사 (Case A) */}
        <Stack.Screen name="InteractionCheck" component={InteractionCheckScreen} options={{ title: '약X영양제 비교' }} />
        <Stack.Screen name="InteractionAddDrug" component={InteractionAddDrugScreen} options={{ title: '처방약 추가' }} />
        <Stack.Screen name="InteractionResult" component={InteractionResultScreen} options={{ title: '분석 결과' }} />
        
        {/* 안전 영양제 추천 (Case B) */}
        <Stack.Screen name="Recommendation" component={RecommendationScreen} options={{ title: '영양제 추천' }} />
        <Stack.Screen name="RecommendationResult" component={RecommendationResultScreen} options={{ title: '추천 결과' }} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
