import React from 'react';
import {
  View,
  Text,
  SafeAreaView,
  TouchableOpacity,
  ActivityIndicator,
  StyleProp,
  ViewStyle,
} from 'react-native';
import { commonStyles } from '../styles';
import Header from './Header';

interface LoadingErrorViewProps {
  loading: boolean;
  error: string | null;
  onRetry: () => void;
  headerTitle: string;
  loadingMessage?: string;
  safeAreaStyle?: StyleProp<ViewStyle>;
  children: React.ReactNode;
}

export default function LoadingErrorView({
  loading,
  error,
  onRetry,
  headerTitle,
  loadingMessage = '불러오는 중...',
  safeAreaStyle,
  children,
}: LoadingErrorViewProps) {
  if (loading) {
    return (
      <SafeAreaView style={safeAreaStyle}>
        <Header title={headerTitle} />
        <View style={commonStyles.centerContainer}>
          <ActivityIndicator size="large" color="#9333EA" />
          <Text style={commonStyles.loadingText}>{loadingMessage}</Text>
        </View>
      </SafeAreaView>
    );
  }

  if (error) {
    return (
      <SafeAreaView style={safeAreaStyle}>
        <Header title={headerTitle} />
        <View style={commonStyles.centerContainer}>
          <Text style={commonStyles.errorText}>{error}</Text>
          <TouchableOpacity
            style={commonStyles.retryButton}
            onPress={onRetry}
            activeOpacity={0.7}
          >
            <Text style={commonStyles.retryButtonText}>다시 시도</Text>
          </TouchableOpacity>
        </View>
      </SafeAreaView>
    );
  }

  return <>{children}</>;
}
