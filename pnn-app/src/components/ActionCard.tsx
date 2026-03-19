import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet, StyleProp, ViewStyle } from 'react-native';
import { Ionicons } from '@expo/vector-icons';

interface ActionCardProps {
  title: string;
  description?: string;
  iconName: keyof typeof Ionicons.glyphMap;
  onPress: () => void;
  /** 컴팩트 모드: 설명 생략, 아이콘+제목만 (나란히 배치용) */
  compact?: boolean;
  /** 아이콘 색상 (컴팩트 모드에서 섹션별 구분용) */
  iconColor?: string;
  /** 아이콘 배경색 */
  iconBgColor?: string;
  style?: StyleProp<ViewStyle>;
  /** 버튼 비활성화 여부 */
  disabled?: boolean;
}

export default function ActionCard({
  title,
  description = '',
  iconName,
  onPress,
  compact = false,
  iconColor = '#8A2BE2',
  iconBgColor = '#F3E8FF',
  style,
  disabled = false,
}: ActionCardProps) {
  return (
    <TouchableOpacity
      activeOpacity={0.7}
      onPress={onPress}
      disabled={disabled}
      style={[
        styles.container, 
        compact && styles.containerCompact, 
        { backgroundColor: compact ? iconBgColor : '#FFFFFF' }, 
        style,
        disabled && styles.containerDisabled
      ]}
    >
      <View style={[styles.iconContainer, compact && styles.iconContainerCompact, { backgroundColor: compact ? 'transparent' : iconBgColor }]}>
        <Ionicons name={iconName} size={compact ? 28 : 32} color={iconColor} />
      </View>
      <View style={[styles.textContainer, compact && styles.textContainerCompact]}>
        <Text style={[styles.title, compact && styles.titleCompact]}>{title}</Text>
        {!compact && description ? (
          <Text style={styles.description}>{description}</Text>
        ) : null}
      </View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#FFFFFF',
    borderRadius: 16,
    padding: 20,
    marginBottom: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05,
    shadowRadius: 2,
    elevation: 2,
    borderWidth: 1,
    borderColor: '#F3F4F6',
    flexDirection: 'row',
    alignItems: 'center',
    marginHorizontal: 20,
  },
  containerCompact: {
    marginHorizontal: 6,
    marginBottom: 0,
    padding: 16,
    borderWidth: 0,
    flex: 1,
  },
  iconContainer: {
    backgroundColor: '#F3E8FF',
    borderRadius: 50,
    padding: 16,
    marginRight: 16,
    justifyContent: 'center',
    alignItems: 'center',
  },
  iconContainerCompact: {
    padding: 0,
    marginRight: 12,
  },
  textContainer: {
    flex: 1,
  },
  textContainerCompact: {
    justifyContent: 'center',
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#111827',
    marginBottom: 4,
  },
  titleCompact: {
    fontSize: 16,
    marginBottom: 0,
  },
  description: {
    fontSize: 14,
    color: '#6B7280',
    lineHeight: 20,
    paddingRight: 8,
  },
  containerDisabled: {
    opacity: 0.5,
  },
});
