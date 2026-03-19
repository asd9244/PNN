import { create } from 'zustand';

export type SelectedDrug = {
  drugId: number;
  itemName: string;
  entpName: string;
};

export type Nutrient = {
  name: string;
  amount?: number | null;
  unit?: string | null;
};

export type Supplement = {
  id: string; // 로컬에서 임의로 생성하는 고유 ID
  name: string; // 영양제 이름 (OCR 결과)
  ingredients: Nutrient[]; // 파싱된 영양성분
  imageUrl?: string; // 촬영/선택한 이미지 URI
};

interface DrugStoreState {
  // 상호작용 체크를 위한 약품 목록
  interactionDrugs: SelectedDrug[];
  interactionSupplements: Supplement[];
  addInteractionDrug: (drug: SelectedDrug) => void;
  removeInteractionDrug: (drugId: number) => void;
  clearInteractionDrugs: () => void;
  addInteractionSupplement: (supplement: Supplement) => void;
  removeInteractionSupplement: (id: string) => void;
  clearInteractionSupplements: () => void;

  // 영양제 추천을 위한 약품 목록
  recommendationDrugs: SelectedDrug[];
  addRecommendationDrug: (drug: SelectedDrug) => void;
  removeRecommendationDrug: (drugId: number) => void;
  clearRecommendationDrugs: () => void;
}

export const useDrugStore = create<DrugStoreState>((set) => ({
  interactionDrugs: [],
  interactionSupplements: [],
  addInteractionDrug: (drug) =>
    set((state) => {
      if (state.interactionDrugs.some((d) => d.drugId === drug.drugId)) return state;
      return { interactionDrugs: [...state.interactionDrugs, drug] };
    }),
  removeInteractionDrug: (drugId) =>
    set((state) => ({
      interactionDrugs: state.interactionDrugs.filter((d) => d.drugId !== drugId),
    })),
  clearInteractionDrugs: () => set({ interactionDrugs: [] }),
  addInteractionSupplement: (supplement) =>
    set((state) => {
      // 최대 3개까지만 허용
      if (state.interactionSupplements.length >= 3) return state;
      return { interactionSupplements: [...state.interactionSupplements, supplement] };
    }),
  removeInteractionSupplement: (id) =>
    set((state) => ({
      interactionSupplements: state.interactionSupplements.filter((s) => s.id !== id),
    })),
  clearInteractionSupplements: () => set({ interactionSupplements: [] }),

  recommendationDrugs: [],
  addRecommendationDrug: (drug) =>
    set((state) => {
      if (state.recommendationDrugs.some((d) => d.drugId === drug.drugId)) return state;
      return { recommendationDrugs: [...state.recommendationDrugs, drug] };
    }),
  removeRecommendationDrug: (drugId) =>
    set((state) => ({
      recommendationDrugs: state.recommendationDrugs.filter((d) => d.drugId !== drugId),
    })),
  clearRecommendationDrugs: () => set({ recommendationDrugs: [] }),
}));
