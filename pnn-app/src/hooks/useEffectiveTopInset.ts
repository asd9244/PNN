import {useMemo} from "react";
import {Platform, StatusBar} from "react-native";
import {useSafeAreaInsets} from "react-native-safe-area-context";

/**
 * 상태 표시줄·노치 영역. Android에서 insets.top 이 0이면 StatusBar 높이로 보완.
 */
export function useEffectiveTopInset(): number {
  const insets = useSafeAreaInsets();
  return useMemo(() => {
    if (insets.top > 0) {
      return insets.top;
    }
    if (Platform.OS === "android") {
      return StatusBar.currentHeight ?? 24;
    }
    return 0;
  }, [insets.top]);
}
