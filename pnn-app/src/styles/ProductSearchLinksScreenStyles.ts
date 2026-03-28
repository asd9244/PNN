import {StyleSheet} from "react-native";

export const productSearchLinksScreenStyles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: "#F9FAFB",
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    paddingHorizontal: 20,
    paddingTop: 16,
    paddingBottom: 24,
  },
  intro: {
    fontSize: 14,
    color: "#6B7280",
    marginBottom: 20,
    lineHeight: 22,
  },
  nutrientSection: {
    marginBottom: 28,
  },
  sectionIngredientName: {
    fontSize: 18,
    fontWeight: "bold",
    color: "#111827",
    marginBottom: 4,
  },
  sectionIngredientNameEn: {
    fontSize: 14,
    color: "#6B7280",
    marginBottom: 12,
  },
  linkRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    backgroundColor: "#FFFFFF",
    borderRadius: 12,
    paddingVertical: 16,
    paddingHorizontal: 16,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: "#F3F4F6",
    shadowColor: "#000",
    shadowOffset: {width: 0, height: 1},
    shadowOpacity: 0.05,
    shadowRadius: 2,
    elevation: 2,
  },
  linkLabel: {
    fontSize: 16,
    fontWeight: "600",
    color: "#1F2937",
  },
});
