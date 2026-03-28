import {Platform} from "react-native";

export type ProductSearchLinkItem = {
  label: string;
  url: string;
};

/**
 * 성분명으로 쿠팡·iHerb 제품 검색 URL 목록을 만듭니다.
 */
export function getProductSearchLinks(
  nameKr: string,
  nameEn: string,
): ProductSearchLinkItem[] {
  const coupangKeyword = encodeURIComponent(`${nameKr} 영양제`);
  const iherbKeyword = encodeURIComponent(`${nameEn} supplement`);

  const coupangUrl =
    Platform.OS === "web"
      ? `https://www.coupang.com/np/search?component=&q=${coupangKeyword}`
      : `https://m.coupang.com/nm/search?q=${coupangKeyword}`;

  const iherbUrl = `https://kr.iherb.com/search?kw=${iherbKeyword}`;

  return [
    {label: "Coupang", url: coupangUrl},
    {label: "iHerb", url: iherbUrl},
  ];
}
