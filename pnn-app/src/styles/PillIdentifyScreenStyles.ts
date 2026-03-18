import { StyleSheet } from 'react-native';

export const pillIdentifyScreenStyles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#F9FAFB',
  },
  container: {
    flex: 1,
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    paddingHorizontal: 20,
    paddingTop: 16,
  },
  descriptionText: {
    fontSize: 14,
    color: '#6B7280',
    marginBottom: 24,
    lineHeight: 20,
  },
  sectionContainer: {
    marginBottom: 32,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#1F2937',
    marginBottom: 8,
  },
  selectionNote: {
    fontSize: 12,
    fontWeight: 'normal',
    color: '#9CA3AF',
  },
  searchBarRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 32,
  },
  searchBarWrapperLeft: {
    flex: 1,
    marginRight: 8,
  },
  searchBarWrapperRight: {
    flex: 1,
    marginLeft: 8,
  },
  searchBarMarginClear: {
    marginHorizontal: 0,
    marginTop: 0,
  },
  gridContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginTop: 8,
  },
  gridItem: {
    width: '23%',
    aspectRatio: 1,
    backgroundColor: '#FFFFFF',
    margin: '1%',
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
  },
  gridItemNormal: {
    borderColor: '#E5E7EB',
  },
  gridItemSelected: {
    borderColor: '#9333EA',
    backgroundColor: '#F3E8FF',
  },
  circlePlaceholder: {
    width: 32,
    height: 32,
    borderRadius: 16,
    marginBottom: 8,
    borderWidth: 1,
  },
  formIconPlaceholder: {
    width: 40,
    height: 40,
    marginBottom: 8,
  },
  circleBorderGray: {
    borderColor: '#9CA3AF',
  },
  circleBorderLight: {
    borderColor: '#F3F4F6',
  },
  gridText: {
    fontSize: 12,
    fontWeight: '500',
    color: '#4B5563',
  },
  gridTextSelected: {
    color: '#7E22CE',
  },
  bottomSpacing: {
    height: 40,
  },
});
