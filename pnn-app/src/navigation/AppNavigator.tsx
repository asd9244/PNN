import React from "react";
import {NavigationContainer} from "@react-navigation/native";
import {createNativeStackNavigator} from "@react-navigation/native-stack";

// Screens
import HomeScreen from "../screens/HomeScreen";
import DrugSearchScreen from "../screens/DrugSearchScreen";
import DrugListScreen from "../screens/DrugListScreen";
import DrugDetailScreen from "../screens/DrugDetailScreen";
import InteractionCheckScreen from "../screens/InteractionCheckScreen";
import InteractionResultScreen from "../screens/InteractionResultScreen";
import type {
  InteractionCompareResponse,
  RecommendationResponse,
} from "../api/ocr";
import RecommendationScreen from "../screens/RecommendationScreen";
import RecommendationResultScreen from "../screens/RecommendationResultScreen";
import ProductSearchLinksScreen from "../screens/ProductSearchLinksScreen";
import PillIdentifyScreen from "../screens/PillIdentifyScreen";

export type SourceScreenType = "InteractionCheck" | "Recommendation";

export type RootStackParamList = {
  Home: undefined;
  DrugSearch: {sourceScreen?: SourceScreenType} | undefined;
  PillIdentify: {sourceScreen?: SourceScreenType} | undefined;
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
  DrugDetail: {drugId: number; sourceScreen?: SourceScreenType};
  InteractionCheck: undefined;
  InteractionResult: {result: InteractionCompareResponse} | undefined;
  Recommendation: undefined;
  RecommendationResult: {result: RecommendationResponse} | undefined;
  ProductSearchLinks: {nutrients: {nameKr: string; nameEn: string}[]};
};

const Stack = createNativeStackNavigator<RootStackParamList>();

export default function AppNavigator() {
  return (
    <NavigationContainer>
      <Stack.Navigator
        initialRouteName="Home"
        screenOptions={{headerShown: false}}
      >
        <Stack.Screen name="Home" component={HomeScreen} />

        {/* 약 검색 및 식별 */}
        <Stack.Screen name="DrugSearch" component={DrugSearchScreen} />
        <Stack.Screen name="PillIdentify" component={PillIdentifyScreen} />
        <Stack.Screen name="DrugList" component={DrugListScreen} />
        <Stack.Screen name="DrugDetail" component={DrugDetailScreen} />

        {/* 상호작용 충돌 검사 (Case A) */}
        <Stack.Screen name="InteractionCheck" component={InteractionCheckScreen} />
        <Stack.Screen name="InteractionResult" component={InteractionResultScreen} />

        {/* 안전 영양제 추천 (Case B) */}
        <Stack.Screen name="Recommendation" component={RecommendationScreen} />
        <Stack.Screen name="RecommendationResult" component={RecommendationResultScreen} />
        <Stack.Screen name="ProductSearchLinks" component={ProductSearchLinksScreen} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
