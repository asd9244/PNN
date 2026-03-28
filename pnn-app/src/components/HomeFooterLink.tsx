import React from "react";
import {Text, TouchableOpacity, StyleSheet} from "react-native";
import {CommonActions, useNavigation} from "@react-navigation/native";
import {NativeStackNavigationProp} from "@react-navigation/native-stack";
import {RootStackParamList} from "../navigation/AppNavigator";
import {useEffectiveBottomInset} from "../hooks/useEffectiveBottomInset";

type Nav = NativeStackNavigationProp<RootStackParamList>;

export interface HomeFooterLinkProps {
  /** ActionButtons 등 부모가 하단 safe area를 이미 줄 때 (중복 여백 방지) */
  embedded?: boolean;
}

export default function HomeFooterLink({embedded = false}: HomeFooterLinkProps) {
  const navigation = useNavigation<Nav>();
  const bottomInset = useEffectiveBottomInset();

  const goHome = () => {
    navigation.dispatch(
      CommonActions.reset({
        index: 0,
        routes: [{name: "Home"}],
      }),
    );
  };

  return (
    <TouchableOpacity
      onPress={goHome}
      activeOpacity={0.7}
      style={[
        styles.wrap,
        embedded ? styles.wrapEmbedded : styles.wrapStandalone,
        !embedded && {paddingBottom: 8 + bottomInset},
      ]}
    >
      <Text style={styles.text}>홈화면으로 이동</Text>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  wrap: {
    alignSelf: "center",
  },
  wrapStandalone: {
    paddingTop: 10,
    marginTop: 4,
  },
  wrapEmbedded: {
    paddingTop: 6,
    paddingBottom: 2,
  },
  text: {
    fontSize: 12,
    color: "#6B7280",
    textDecorationLine: "underline",
  },
});
