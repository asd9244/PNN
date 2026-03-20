import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';

interface ActionButtonsProps {
  onReset: () => void;
  onSubmit: () => void;
  submitText?: string;
}

export default function ActionButtons({ onReset, onSubmit, submitText = "검색하기" }: ActionButtonsProps) {
  const insets = useSafeAreaInsets();

  return (
    <View style={[styles.container, { paddingBottom: 12 + insets.bottom }]}>
      <TouchableOpacity onPress={onReset} style={styles.resetButton}>
        <Ionicons name="refresh" size={20} color="#4B5563" style={styles.icon} />
        <Text style={styles.resetText}>초기화</Text>
      </TouchableOpacity>

      <TouchableOpacity onPress={onSubmit} style={styles.submitButton}>
        <View style={styles.submitContent}>
          <Ionicons name="search" size={20} color="#FFFFFF" style={styles.icon} />
          <Text style={styles.submitText}>{submitText}</Text>
        </View>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
    backgroundColor: '#FFFFFF',
    borderTopWidth: 1,
    borderTopColor: '#F3F4F6', // gray-100
  },
  resetButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#F3F4F6', // gray-100
    paddingVertical: 16,
    paddingHorizontal: 24,
    borderRadius: 12,
    marginRight: 12,
  },
  icon: {
    marginRight: 8,
  },
  resetText: {
    color: '#374151', // gray-700
    fontWeight: 'bold',
    fontSize: 16,
  },
  submitButton: {
    flex: 1,
    backgroundColor: '#9333EA', // purple-600
    paddingVertical: 16,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  submitContent: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  submitText: {
    color: '#FFFFFF',
    fontWeight: 'bold',
    fontSize: 16,
  },
});
