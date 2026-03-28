import React from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  ActivityIndicator,
  StyleProp,
  ViewStyle,
} from 'react-native';
import { commonStyles } from '../styles';
import Header from './Header';
import HomeFooterLink from './HomeFooterLink';

interface LoadingErrorViewProps {
  loading: boolean;
  error: string | null;
  onRetry: () => void;
  headerTitle: string;
  loadingMessage?: string;
  safeAreaStyle?: StyleProp<ViewStyle>;
  children: React.ReactNode;
  /** true면 상단 인라인 Header를 렌더하지 않음 */
  hideHeader?: boolean;
}

export default function LoadingErrorView({
  loading,
  error,
  onRetry,
  headerTitle,
  loadingMessage = '불러오는 중...',
  safeAreaStyle,
  children,
  hideHeader = false,
}: LoadingErrorViewProps) {
  if (loading) {
    return (
      <View style={safeAreaStyle}>
        {!hideHeader ? <Header title={headerTitle} /> : null}
        <View style={commonStyles.centerContainer}>
          <ActivityIndicator size="large" color="#9333EA" />
          <Text style={commonStyles.loadingText}>{loadingMessage}</Text>
        </View>
        <HomeFooterLink />
      </View>
    );
  }

  if (error) {
    return (
      <View style={safeAreaStyle}>
        {!hideHeader ? <Header title={headerTitle} /> : null}
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
        <HomeFooterLink />
      </View>
    );
  }

  return <>{children}</>;
}
