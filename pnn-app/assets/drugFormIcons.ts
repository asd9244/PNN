import type {ImageSourcePropType} from "react-native";

/**
 * 식별검색 제형 그리드 아이콘.
 * PNG는 이 파일과 같은 디렉터리의 `drug_icons/` 에만 두면 됩니다 (`pnn-app/assets/drug_icons/`).
 */
export const DRUG_FORM_ICONS: Record<string, ImageSourcePropType> = {
  정제: require("./drug_icons/Tablet.png"),
  경질캡슐: require("./drug_icons/HardCapsule.png"),
  연질캡슐: require("./drug_icons/SoftCapsule.png"),
};
