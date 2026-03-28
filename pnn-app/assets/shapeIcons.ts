import type {ImageSourcePropType} from "react-native";

/**
 * 식별검색 모양 그리드 — `shape_icons/shape01` … 순서가 원형, 타원형, … 와 동일.
 * `기타`는 아이콘 없음(화면에서 빈 플레이스홀더).
 */
export const SHAPE_ICONS: Record<string, ImageSourcePropType> = {
  원형: require("./shape_icons/shape01.jpg"),
  타원형: require("./shape_icons/shape02.jpg"),
  반원형: require("./shape_icons/shape03.jpg"),
  삼각형: require("./shape_icons/shape04.jpg"),
  사각형: require("./shape_icons/shape05.jpg"),
  마름모: require("./shape_icons/shape06.jpg"),
  장방형: require("./shape_icons/shape07.jpg"),
  오각형: require("./shape_icons/shape08.jpg"),
  육각형: require("./shape_icons/shape9.jpg"),
  팔각형: require("./shape_icons/shape10.jpg"),
};
