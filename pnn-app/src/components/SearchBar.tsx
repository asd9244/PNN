import React from 'react';
import { View, TextInput, TouchableOpacity, StyleSheet, StyleProp, ViewStyle } from 'react-native';
import { Ionicons } from '@expo/vector-icons';

interface SearchBarProps {
  placeholder?: string;
  value?: string;
  onChangeText?: (text: string) => void;
  onSearch?: () => void;
  onPressIn?: () => void;
  editable?: boolean;
  style?: StyleProp<ViewStyle>;
}

export default function SearchBar({
  placeholder = "어떤 약을 찾으시나요?",
  value,
  onChangeText,
  onSearch,
  onPressIn,
  editable = true,
  style,
}: SearchBarProps) {
  return (
    <TouchableOpacity 
      activeOpacity={0.8} 
      onPress={onPressIn}
      style={[styles.container, style]}
    >
      <Ionicons name="search" size={20} color="#888" style={styles.icon} />
      <TextInput
        style={styles.input}
        placeholder={placeholder}
        placeholderTextColor="#A0A0A0"
        value={value}
        onChangeText={onChangeText}
        onSubmitEditing={onSearch}
        editable={editable}
        onPressIn={onPressIn}
        pointerEvents={editable ? 'auto' : 'none'}
      />
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#D1D5DB', // gray-300
    paddingHorizontal: 16,
    paddingVertical: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05,
    shadowRadius: 2,
    elevation: 2,
  },
  icon: {
    marginRight: 8,
  },
  input: {
    flex: 1,
    fontSize: 16,
    color: '#1F2937', // gray-800
    marginLeft: 8,
  },
});
