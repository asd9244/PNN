import React, { useState } from "react";
import {
  Text,
  View,
  Button,
  SafeAreaView,
  ScrollView,
  TouchableOpacity,
  LayoutAnimation,
  Platform,
  UIManager
} from "react-native";
import {useSafeAreaInsets} from "react-native-safe-area-context";
import {RouteProp, useRoute} from "@react-navigation/native";
import {RootStackParamList} from "../navigation/AppNavigator";
import Header from "../components/Header";

// 안드로이드 레이아웃 애니메이션 활성화
if (Platform.OS === 'android') {
  if (UIManager.setLayoutAnimationEnabledExperimental) {
    UIManager.setLayoutAnimationEnabledExperimental(true);
  }
}

type InteractionResultRouteProp = RouteProp<
  RootStackParamList,
  "InteractionResult"
>;

/** API level 문자열 정규화 (대소문자 무시) */
function normLevel(level: string | undefined): string {
  return (level || "").toUpperCase().trim();
}

/** 카드/헤더용: WARNING · SAFE · 그 외(레거시 등급) */
function getLevelVisual(level: string | undefined) {
  const L = normLevel(level);
  if (L === "WARNING") {
    return { label: "위험", bgColor: "#FEF2F2", textColor: "#B91C1C" };
  }
  if (L === "SAFE") {
    return { label: "안전", bgColor: "#ECFDF5", textColor: "#047857" };
  }
  return { label: "안내", bgColor: "#F3F4F6", textColor: "#374151" };
}

export default function InteractionResultScreen({navigation}: any) {
  const route = useRoute<InteractionResultRouteProp>();
  const result = route.params?.result;
  const insets = useSafeAreaInsets();

  const [expandedGroups, setExpandedGroups] = useState<Record<string, boolean>>({});

  const toggleGroup = (drugName: string) => {
    LayoutAnimation.configureNext(LayoutAnimation.Presets.easeInEaseOut);
    setExpandedGroups((prev) => ({
      ...prev,
      [drugName]: !prev[drugName],
    }));
  };

  /** 성분 데이터 부족 등으로 표시만 하던 항목(알 수 없음)은 약 추가 단계에서 걸러지므로 결과에서 제외 */
  const visibleInteractions =
    result?.interactions?.filter((item: any) => item.nutrient !== "알 수 없음") ?? [];

  // 데이터를 약 이름 기준으로 그룹화
  const groupedInteractions = visibleInteractions.reduce((acc: any, item: any) => {
    const key = item.drugName || "이름 없는 약품";
    if (!acc[key]) {
      acc[key] = [];
    }
    acc[key].push(item);
    return acc;
  }, {});

  return (
    <SafeAreaView style={{flex: 1, backgroundColor: "#F9FAFB"}}>
      <Header title="분석 결과" />
      <ScrollView
        contentContainerStyle={{
          padding: 20,
          paddingBottom: 20 + insets.bottom,
        }}
      >
        <Text style={{fontSize: 20, fontWeight: "bold", marginBottom: 20}}>
          충돌 검사 결과
        </Text>

        <View
          style={{
            backgroundColor: "#fff",
            padding: 16,
            borderRadius: 12,
            marginBottom: 16,
          }}
        >
          <Text style={{fontWeight: "bold", fontSize: 16, marginBottom: 8}}>
            분석 결과 요약
          </Text>
          <Text>
            {visibleInteractions.length > 0
              ? "상호작용이 감지된 항목이 있습니다."
              : "특이한 상호작용이 발견되지 않았습니다."}
          </Text>
        </View>

        {groupedInteractions &&
          Object.keys(groupedInteractions).map((drugName: string) => {
            const items = groupedInteractions[drugName];
            const isExpanded = expandedGroups[drugName];

            // 그룹 헤더 배지: WARNING > SAFE > 기타(안내)
            const getHighestLevel = (items: any[]) => {
              const levels = items.map((i) => normLevel(i.level));
              if (levels.some((l) => l === "WARNING")) {
                return { label: "위험", color: "#B91C1C", bgColor: "#FEF2F2" };
              }
              if (levels.some((l) => l === "SAFE")) {
                return { label: "안전", color: "#047857", bgColor: "#ECFDF5" };
              }
              return { label: "안내", color: "#374151", bgColor: "#F3F4F6" };
            };

            const headerInfo = getHighestLevel(items);

            return (
              <View key={drugName} style={{ marginBottom: 16 }}>
                {/* 그룹 헤더 토글 버튼 */}
                <TouchableOpacity
                  onPress={() => toggleGroup(drugName)}
                  activeOpacity={0.8}
                  style={{
                    backgroundColor: "#fff",
                    padding: 16,
                    borderRadius: 12,
                    flexDirection: "row",
                    alignItems: "center",
                    justifyContent: "space-between",
                    shadowColor: "#000",
                    shadowOpacity: 0.05,
                    shadowRadius: 5,
                    shadowOffset: { width: 0, height: 2 },
                    elevation: 2,
                    borderWidth: 1,
                    borderColor: headerInfo.bgColor,
                  }}
                >
                  <View style={{ flex: 1, flexDirection: "row", alignItems: "center" }}>
                    <View
                      style={{
                        backgroundColor: headerInfo.bgColor,
                        paddingHorizontal: 8,
                        paddingVertical: 4,
                        borderRadius: 4,
                        marginRight: 12,
                      }}
                    >
                      <Text style={{ color: headerInfo.color, fontWeight: "bold", fontSize: 12 }}>
                        {headerInfo.label}
                      </Text>
                    </View>
                    <Text
                      style={{
                        fontWeight: "bold",
                        fontSize: 16,
                        color: "#111827",
                        flexShrink: 1,
                      }}
                      numberOfLines={1}
                    >
                      {drugName}
                    </Text>
                  </View>
                  <View style={{ flexDirection: "row", alignItems: "center" }}>
                    <Text style={{ color: "#6B7280", marginRight: 8, fontSize: 13 }}>
                      {items.length}건
                    </Text>
                    <Text style={{ color: "#9CA3AF", fontSize: 14 }}>
                      {isExpanded ? "▲" : "▼"}
                    </Text>
                  </View>
                </TouchableOpacity>

                {/* 본문 (개별 상호작용 목록) */}
                {isExpanded && (
                  <View style={{ marginTop: 8 }}>
                    {items.map((item: any, index: number) => {
                      const visual = getLevelVisual(item.level);

                      return (
                        <View
                          key={index}
                          style={{
                            backgroundColor: visual.bgColor,
                            padding: 16,
                            borderRadius: 12,
                            marginBottom: 8,
                          }}
                        >
                          <Text
                            style={{
                              fontWeight: "bold",
                              fontSize: 14,
                              color: visual.textColor,
                              marginBottom: 4,
                            }}
                          >
                            {visual.label}
                          </Text>

                          <Text
                            style={{
                              fontWeight: "bold",
                              fontSize: 16,
                              color: visual.textColor,
                              marginBottom: 8,
                            }}
                          >
                            {`${item.nutrient} × ${item.contraindicatedDrugIngredient}`}
                          </Text>

                          <Text
                            style={{ color: "#374151", marginBottom: 12, lineHeight: 20 }}
                          >
                            {item.description}
                          </Text>

                          <View
                            style={{
                              backgroundColor: "rgba(255,255,255,0.6)",
                              padding: 12,
                              borderRadius: 8,
                            }}
                          >
                            <Text style={{ color: "#111827", fontWeight: "500" }}>
                              안전 복약 가이드
                            </Text>
                            <Text style={{ color: "#374151", marginTop: 4 }}>
                              {item.actionGuide}
                            </Text>
                          </View>
                        </View>
                      );
                    })}
                  </View>
                )}
              </View>
            );
          })}

        <Button title="홈으로 돌아가기" onPress={() => navigation.popToTop()} />
      </ScrollView>
    </SafeAreaView>
  );
}
