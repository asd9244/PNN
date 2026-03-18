import { StyleSheet } from 'react-native';

export const drugDetailScreenStyles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#FFFFFF',
  },
  tabBar: {
    flexDirection: 'row',
    borderBottomWidth: 1,
    borderBottomColor: '#E5E7EB',
  },
  tabButton: {
    flex: 1,
    paddingVertical: 14,
    alignItems: 'center',
    justifyContent: 'center',
    borderBottomWidth: 2,
    borderBottomColor: 'transparent',
  },
  tabButtonActive: {
    borderBottomColor: '#059669',
  },
  tabText: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#9CA3AF',
  },
  tabTextActive: {
    color: '#047857',
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    paddingBottom: 40,
  },
  drugHeader: {
    alignItems: 'center',
    paddingVertical: 24,
    paddingHorizontal: 16,
    backgroundColor: '#FFFFFF',
  },
  drugNameText: {
    fontSize: 24,
    fontWeight: '900',
    color: '#111827',
    marginBottom: 4,
    textAlign: 'center',
  },
  entpNameText: {
    fontSize: 14,
    fontWeight: '500',
    color: '#9CA3AF',
  },
  tabContentContainer: {
    paddingHorizontal: 20,
    marginTop: 16,
  },
  drugImageContainer: {
    width: '100%',
    height: 192,
    backgroundColor: '#E5E7EB',
    borderRadius: 16,
    overflow: 'hidden',
    marginVertical: 16,
    borderWidth: 1,
    borderColor: '#E5E7EB',
  },
  drugImage: {
    width: '100%',
    height: '100%',
  },
  sectionTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 8,
  },
  dot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    marginRight: 8,
  },
  sectionTitleText: {
    fontSize: 16,
    fontWeight: 'bold',
  },
  greenBox: {
    backgroundColor: '#ECFDF5',
    borderRadius: 16,
    padding: 16,
    marginBottom: 24,
  },
  yellowBox: {
    backgroundColor: '#FFFBEB',
    borderRadius: 16,
    padding: 16,
    marginBottom: 24,
  },
  boxText: {
    fontSize: 14,
    color: '#374151',
    lineHeight: 24,
  },
  footerNote: {
    fontSize: 12,
    color: '#9CA3AF',
    marginTop: 16,
  },
  permitGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    borderBottomWidth: 1,
    borderBottomColor: '#F3F4F6',
    paddingBottom: 16,
    marginBottom: 16,
  },
  permitGridItemLeft: {
    width: '50%',
    paddingRight: 8,
    marginBottom: 16,
  },
  permitGridItemRight: {
    width: '50%',
    paddingLeft: 8,
    marginBottom: 16,
  },
  permitSection: {
    marginBottom: 24,
  },
  permitSectionHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 8,
  },
  verticalBar: {
    width: 4,
    height: 18,
    marginRight: 8,
  },
  permitSectionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#111827',
  },
  permitText: {
    fontSize: 14,
    color: '#374151',
    lineHeight: 24,
  },
  grayBox: {
    backgroundColor: '#F3F4F6',
    borderRadius: 16,
    padding: 16,
    marginTop: 8,
  },
  redBox: {
    backgroundColor: '#FEF2F2',
    borderRadius: 16,
    padding: 16,
    marginBottom: 32,
  },
  redBoxTitle: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#B91C1C',
    marginBottom: 8,
  },
  redBoxText: {
    fontSize: 14,
    color: '#991B1B',
    marginBottom: 4,
  },
  contraindicationItem: {
    marginBottom: 16,
  },
  durSection: {
    marginBottom: 24,
  },
  durCard: {
    backgroundColor: '#FFFFFF',
    borderWidth: 1,
    borderColor: '#E5E7EB',
    borderRadius: 16,
    padding: 16,
    marginBottom: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05,
    shadowRadius: 2,
    elevation: 2,
  },
  durCardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 4,
  },
  durCardDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: '#EAB308',
    marginRight: 8,
  },
  durCardTitle: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#374151',
  },
  durCardDesc: {
    fontSize: 14,
    color: '#4B5563',
    marginLeft: 16,
    lineHeight: 20,
  },
  durCardText: {
    fontSize: 14,
    color: '#374151',
    lineHeight: 22,
    marginBottom: 4,
  },
});
