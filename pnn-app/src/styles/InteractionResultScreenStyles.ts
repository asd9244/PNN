import { StyleSheet } from 'react-native';

export const interactionResultScreenStyles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#F9FAFB',
  },
  scrollContent: {
    padding: 20,
    paddingBottom: 20,
  },
  pageTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    marginBottom: 20,
  },
  summaryCard: {
    backgroundColor: '#fff',
    padding: 16,
    borderRadius: 12,
    marginBottom: 16,
  },
  summaryCardTitle: {
    fontWeight: 'bold',
    fontSize: 16,
    marginBottom: 8,
  },
  groupBlock: {
    marginBottom: 16,
  },
  groupHeader: {
    backgroundColor: '#fff',
    padding: 16,
    borderRadius: 12,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    shadowColor: '#000',
    shadowOpacity: 0.05,
    shadowRadius: 5,
    shadowOffset: { width: 0, height: 2 },
    elevation: 2,
    borderWidth: 1,
  },
  groupHeaderLeft: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
  },
  badgeWrap: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 4,
    marginRight: 12,
  },
  badgeText: {
    fontWeight: 'bold',
    fontSize: 12,
  },
  drugNameText: {
    fontWeight: 'bold',
    fontSize: 16,
    color: '#111827',
    flexShrink: 1,
  },
  groupHeaderRight: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  countText: {
    color: '#6B7280',
    marginRight: 8,
    fontSize: 13,
  },
  chevron: {
    color: '#9CA3AF',
    fontSize: 14,
  },
  expandedBlock: {
    marginTop: 8,
  },
  itemCard: {
    padding: 16,
    borderRadius: 12,
    marginBottom: 8,
  },
  itemLevelLabel: {
    fontWeight: 'bold',
    fontSize: 14,
    marginBottom: 4,
  },
  itemPairTitle: {
    fontWeight: 'bold',
    fontSize: 16,
    marginBottom: 8,
  },
  itemDescription: {
    color: '#374151',
    marginBottom: 12,
    lineHeight: 20,
  },
  guideBox: {
    backgroundColor: 'rgba(255,255,255,0.6)',
    padding: 12,
    borderRadius: 8,
  },
  guideTitle: {
    color: '#111827',
    fontWeight: '500',
  },
  guideBody: {
    color: '#374151',
    marginTop: 4,
  },
});
