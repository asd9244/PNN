import React from 'react';
import { View, Text, StyleProp, ViewStyle } from 'react-native';
import SearchBar from './SearchBar';

interface LabeledSearchBarProps {
  label: string;
  placeholder?: string;
  value?: string;
  onChangeText?: (text: string) => void;
  onSearch?: () => void;
  style?: StyleProp<ViewStyle>;
}

export default function LabeledSearchBar({
  label,
  placeholder,
  value,
  onChangeText,
  onSearch,
  style,
}: LabeledSearchBarProps) {
  return (
    <View style={style}>
      <Text style={labelStyle}>{label}</Text>
      <SearchBar
        placeholder={placeholder}
        value={value}
        onChangeText={onChangeText}
        onSearch={onSearch}
        style={searchBarStyle}
      />
    </View>
  );
}

const labelStyle = {
  fontSize: 14,
  fontWeight: '600' as const,
  color: '#374151',
  marginBottom: 8,
  marginTop: 16,
};

const searchBarStyle = {
  marginHorizontal: 0,
  marginTop: 0,
};
