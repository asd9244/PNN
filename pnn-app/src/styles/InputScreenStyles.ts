import { StyleSheet } from 'react-native';

export const inputScreenStyles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#F9FAFB',
  },
  container: {
    flex: 1,
  },
  scrollContent: {
    paddingHorizontal: 20,
    paddingBottom: 24,
  },
  header: {
    marginBottom: 24,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#111827',
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 14,
    color: '#6B7280',
    lineHeight: 20,
  },
  section: {
    marginBottom: 24,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#1F2937',
    marginBottom: 4,
  },
  sectionSubtitle: {
    fontSize: 13,
    color: '#6B7280',
    marginBottom: 12,
  },
  buttonRow: {
    flexDirection: 'row',
  },
  summaryCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: 16,
    padding: 16,
    marginBottom: 16,
    borderWidth: 1,
    borderColor: '#F3F4F6',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05,
    shadowRadius: 2,
    elevation: 2,
  },
  summaryCardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 12,
  },
  summaryCardBarBlue: {
    width: 4,
    height: 18,
    borderRadius: 2,
    backgroundColor: '#2563EB',
    marginRight: 8,
  },
  summaryCardBarOrange: {
    width: 4,
    height: 18,
    borderRadius: 2,
    backgroundColor: '#EA580C',
    marginRight: 8,
  },
  summaryCardBarGreen: {
    width: 4,
    height: 18,
    borderRadius: 2,
    backgroundColor: '#059669', // Emerald 600
    marginRight: 8,
  },
  summaryCardTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#111827',
  },
  summaryCardPlaceholder: {
    fontSize: 14,
    color: '#9CA3AF',
    fontStyle: 'italic',
  },
  summaryCardNote: {
    fontSize: 11,
    color: '#9CA3AF',
    marginTop: 10,
  },
  summaryCardContent: {
    fontSize: 14,
    color: '#374151',
    lineHeight: 22,
  },
  bottomButton: {
    backgroundColor: '#E5E7EB',
    borderRadius: 16,
    paddingVertical: 18,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 8,
  },
  bottomButtonText: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#9CA3AF',
  },
  drugListContainer: {
    marginTop: 8,
  },
  drugListItem: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#F3F4F6',
  },
  drugListItemText: {
    flex: 1,
    marginRight: 12,
  },
  drugItemName: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#111827',
    marginBottom: 4,
  },
  /** 요약 리스트에서 약품명 탭 시 상세로 이동할 때 사용 */
  drugItemNameLink: {
    color: '#2563EB',
  },
  drugEntpName: {
    fontSize: 12,
    color: '#6B7280',
  },
  removeDrugButton: {
    padding: 4,
  },
});
