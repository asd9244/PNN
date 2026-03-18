import React from 'react';
import { View, Text, StyleSheet } from 'react-native';

interface InfoRowProps {
  label?: string;
  value: string | undefined;
}

export default function InfoRow({ label, value }: InfoRowProps) {
  return (
    <View style={styles.container}>
      {label ? <Text style={styles.label}>{label}</Text> : null}
      <Text style={styles.value}>{value || '-'}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    paddingVertical: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#F3F4F6',
  },
  label: {
    fontSize: 12,
    color: '#9CA3AF',
    fontWeight: '500',
    marginBottom: 4,
  },
  value: {
    fontSize: 16,
    color: '#1F2937',
    fontWeight: '500',
    lineHeight: 22,
  },
});
