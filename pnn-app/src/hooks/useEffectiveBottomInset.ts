import {useMemo} from "react";
import {Platform} from "react-native";
import {useSafeAreaInsets} from "react-native-safe-area-context";

/**
 * Android edge-to-edge 등으로 insets.bottom 이 0이면 시스템 내비와 겹침.
 * 보고된 inset이 없을 때만 Android에 최소 하단 여백(dp) 적용.
 */
export function useEffectiveBottomInset(): number {
  const insets = useSafeAreaInsets();
  return useMemo(() => {
    if (insets.bottom > 0) {
      return insets.bottom;
    }
    if (Platform.OS === "android") {
      return 64;
    }
    return 0;
  }, [insets.bottom]);
}
