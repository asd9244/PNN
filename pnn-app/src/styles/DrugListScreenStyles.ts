import { StyleSheet } from 'react-native';

export const drugListScreenStyles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#F9FAFB',
  },
  emptyText: {
    fontSize: 14,
    color: '#6B7280',
  },
  headerInfo: {
    paddingHorizontal: 20,
    paddingTop: 16,
    paddingBottom: 8,
  },
  resultText: {
    color: '#6B7280',
    fontSize: 14,
    fontWeight: '500',
    marginBottom: 16,
  },
  resultCountText: {
    color: '#059669',
    fontWeight: 'bold',
  },
  listContent: {
    paddingHorizontal: 20,
    paddingBottom: 40,
  },
  footerLoader: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 16,
  },
  footerLoaderText: {
    fontSize: 12,
    color: '#9CA3AF',
  },
  cardContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#FFFFFF',
    borderRadius: 16,
    padding: 16,
    marginBottom: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05,
    shadowRadius: 2,
    elevation: 2,
    borderWidth: 1,
    borderColor: '#F3F4F6',
  },
  imageContainer: {
    width: 64,
    height: 64,
    borderRadius: 12,
    backgroundColor: '#E5E7EB',
    overflow: 'hidden',
    marginRight: 16,
    borderWidth: 1,
    borderColor: '#F3F4F6',
  },
  image: {
    width: '100%',
    height: '100%',
  },
  textContainer: {
    flex: 1,
    justifyContent: 'center',
  },
  itemNameText: {
    fontSize: 16,
    fontWeight: '900',
    color: '#111827',
    marginBottom: 4,
  },
  entpNameText: {
    fontSize: 12,
    fontWeight: '500',
    color: '#9CA3AF',
  },
});
