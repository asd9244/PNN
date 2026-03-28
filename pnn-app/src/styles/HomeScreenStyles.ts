import { StyleSheet } from 'react-native';

export const homeScreenStyles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#F9FAFB',
  },
  container: {
    flex: 1,
  },
  scrollContent: {
    // SafeAreaView가 하단 시스템 UI를 피해 주고, 마지막 카드 아래 시각적 여유
    paddingBottom: 32,
  },
  header: {
    paddingHorizontal: 20,
    paddingTop: 16,
    paddingBottom: 24,
  },
  logoContainer: {
    width: 64,
    height: 64,
    backgroundColor: '#E9D5FF',
    borderRadius: 16,
    marginBottom: 16,
    justifyContent: 'center',
    alignItems: 'center',
  },
  logoText: {
    color: '#7E22CE',
    fontWeight: '900',
    fontSize: 20,
  },
  title: {
    fontSize: 30,
    fontWeight: '800',
    color: '#111827',
    letterSpacing: -0.5,
  },
  subtitle: {
    fontSize: 16,
    color: '#6B7280',
    marginTop: 8,
  },
  searchBar: {
    marginHorizontal: 20,
    marginTop: 16,
  },
  spacing: {
    height: 32,
  },
  sectionTitle: {
    paddingHorizontal: 20,
    fontSize: 18,
    fontWeight: 'bold',
    color: '#111827',
    marginBottom: 16,
  },
});
