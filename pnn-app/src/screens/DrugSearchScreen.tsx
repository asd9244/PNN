import React, { useState } from 'react';
import { View, Text, SafeAreaView, ScrollView, KeyboardAvoidingView, Platform, Alert } from 'react-native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { RouteProp, useRoute } from '@react-navigation/native';
import { RootStackParamList } from '../navigation/AppNavigator';

import Header from '../components/Header';
import LabeledSearchBar from '../components/LabeledSearchBar';
import ActionButtons from '../components/ActionButtons';
import { drugSearchScreenStyles as styles } from '../styles';

type NavigationProp = NativeStackNavigationProp<RootStackParamList, 'DrugSearch'>;
type DrugSearchRouteProp = RouteProp<RootStackParamList, 'DrugSearch'>;
interface Props {
  navigation: NavigationProp;
}

export default function DrugSearchScreen({ navigation }: Props) {
  const route = useRoute<DrugSearchRouteProp>();
  const sourceScreen = route.params?.sourceScreen;

  const [itemName, setItemName] = useState('');
  const [entpName, setEntpName] = useState('');
  const [ingredient, setIngredient] = useState('');

  const isEmpty = () =>
    !itemName.trim() && !entpName.trim() && !ingredient.trim();

  const handleReset = () => {
    setItemName('');
    setEntpName('');
    setIngredient('');
  };

  const handleSearch = () => {
    if (isEmpty()) {
      Alert.alert('검색 조건 입력', '최소 1개 이상의 검색 조건을 입력해 주세요.');
      return;
    }
    navigation.navigate('DrugList', {
      itemName: itemName.trim() || undefined,
      entpName: entpName.trim() || undefined,
      ingredient: ingredient.trim() || undefined,
      sourceScreen,
    });
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <Header title="상세검색" />

      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        style={styles.container}
      >
        <ScrollView
          style={styles.scrollView}
          contentContainerStyle={styles.scrollContent}
          keyboardShouldPersistTaps="handled"
        >
          <Text style={styles.description}>
            약품명, 제조사, 성분명으로 검색합니다. 셋 중 하나만 입력해도 되고, 복수로 입력해도 됩니다.
          </Text>

          <LabeledSearchBar
            label="약품명"
            placeholder="약품명 입력"
            value={itemName}
            onChangeText={setItemName}
            onSearch={handleSearch}
          />

          <LabeledSearchBar
            label="제조사(업체)명"
            placeholder="제조사명 입력"
            value={entpName}
            onChangeText={setEntpName}
            onSearch={handleSearch}
          />

          <LabeledSearchBar
            label="성분명"
            placeholder="성분명 입력 (한글/영문)"
            value={ingredient}
            onChangeText={setIngredient}
            onSearch={handleSearch}
          />
        </ScrollView>
      </KeyboardAvoidingView>

      <ActionButtons onReset={handleReset} onSubmit={handleSearch} />
    </SafeAreaView>
  );
}
