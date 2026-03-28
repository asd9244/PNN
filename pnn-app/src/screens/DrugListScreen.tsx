import React, { useCallback, useEffect, useState } from 'react';
import axios from 'axios';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  Image,
  ActivityIndicator,
} from 'react-native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { RouteProp, useRoute } from '@react-navigation/native';
import { RootStackParamList } from '../navigation/AppNavigator';

import Header from '../components/Header';
import LoadingErrorView from '../components/LoadingErrorView';
import HomeFooterLink from '../components/HomeFooterLink';
import { drugListScreenStyles as styles, commonStyles } from '../styles';
import { searchDrugDetail, searchPillIdentifier } from '../api/drugs';
import { getNetworkErrorMessage } from '../api/client';
import type { DrugSearchResponseDto } from '../types/drug';

type NavigationProp = NativeStackNavigationProp<RootStackParamList, 'DrugList'>;
type RoutePropType = RouteProp<RootStackParamList, 'DrugList'>;

interface Props {
  navigation: NavigationProp;
}

// 이미지 없을 때 사용할 placeholder (투명 1x1 또는 회색 박스)
const PLACEHOLDER_IMAGE = 'https://via.placeholder.com/80?text=No+Image';
const PAGE_SIZE = 20;

export default function DrugListScreen({ navigation }: Props) {
  const route = useRoute<RoutePropType>();
  const params = route.params;

  const [data, setData] = useState<DrugSearchResponseDto[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);

  const isDetailSearch = Boolean(
    params?.itemName || params?.entpName || params?.ingredient
  );
  const isPillIdentifySearch = Boolean(
    params?.printFront ||
      params?.printBack ||
      params?.drugShape ||
      params?.color ||
      params?.line ||
      params?.formulation
  );

  const fetchPage = async (page: number, append: boolean) => {
    if (!params) return null;

    const response = isDetailSearch
      ? await searchDrugDetail({
          itemName: params.itemName,
          entpName: params.entpName,
          ingredient: params.ingredient,
          page,
          size: PAGE_SIZE,
        })
      : await searchPillIdentifier({
          printFront: params.printFront,
          printBack: params.printBack,
          drugShape: params.drugShape,
          color: params.color,
          line: params.line,
          formulation: params.formulation,
          page,
          size: PAGE_SIZE,
        });

    if (append) {
      setData((prev) => [...prev, ...response.content]);
    } else {
      setData(response.content);
    }
    setTotalElements(response.totalElements);
    setHasMore(!response.last);
    setCurrentPage(page);
    return response;
  };

  const loadInitial = useCallback(async () => {
    if (!params) {
      setData([]);
      setTotalElements(0);
      setLoading(false);
      setError('검색 조건이 없습니다.');
      return;
    }

    if (!isDetailSearch && !isPillIdentifySearch) {
      setData([]);
      setTotalElements(0);
      setLoading(false);
      setError('검색 조건이 없습니다. 상세검색 또는 식별검색에서 조건을 입력해 주세요.');
      return;
    }

    setLoading(true);
    setError(null);
    setCurrentPage(0);
    setHasMore(true);

    try {
      await fetchPage(0, false);
    } catch (err: unknown) {
      let message = '검색 중 오류가 발생했습니다.';
      if (axios.isAxiosError(err)) {
        if (err.code === 'ERR_NETWORK' || err.message?.includes('Network Error')) {
          message = getNetworkErrorMessage(err);
        } else if (err.response?.data) {
          const resData = err.response.data as Record<string, unknown>;
          if (resData && typeof resData.message === 'string') message = resData.message;
        } else if (err.message) {
          message = err.message;
        }
      } else if (err instanceof Error) {
        message = err.message;
      }
      setError(message);
      setData([]);
      setTotalElements(0);
    } finally {
      setLoading(false);
    }
  }, [
    params?.itemName,
    params?.entpName,
    params?.ingredient,
    params?.printFront,
    params?.printBack,
    params?.drugShape,
    params?.color,
    params?.line,
    params?.formulation,
  ]);

  useEffect(() => {
    loadInitial();
  }, [loadInitial]);

  const loadMore = async () => {
    if (loadingMore || !hasMore || loading || data.length >= totalElements) return;
    if (!params || (!isDetailSearch && !isPillIdentifySearch)) return;

    setLoadingMore(true);
    try {
      await fetchPage(currentPage + 1, true);
    } catch {
      setLoadingMore(false);
    } finally {
      setLoadingMore(false);
    }
  };

  const handleItemPress = (drugId: number) => {
    navigation.navigate('DrugDetail', { drugId, sourceScreen: params?.sourceScreen });
  };

  return (
    <LoadingErrorView
      loading={loading}
      error={error}
      onRetry={loadInitial}
      headerTitle="약품 목록"
      loadingMessage="검색 중..."
      safeAreaStyle={styles.safeArea}
    >
      <View style={styles.safeArea}>
      <Header title="약품 목록" />

      <View style={styles.headerInfo}>
        <Text style={styles.resultText}>
          약품 목록 <Text style={styles.resultCountText}>{totalElements}</Text>건
        </Text>
      </View>

      {data.length === 0 ? (
        <View style={commonStyles.centerContainer}>
          <Text style={styles.emptyText}>검색 결과가 없습니다.</Text>
          <HomeFooterLink />
        </View>
      ) : (
        <FlatList
          data={data}
          keyExtractor={(item) => item.drugId.toString()}
          contentContainerStyle={styles.listContent}
          showsVerticalScrollIndicator={false}
          onEndReached={loadMore}
          onEndReachedThreshold={0.3}
          ListFooterComponent={
            <>
              {loadingMore ? (
                <View style={styles.footerLoader}>
                  <ActivityIndicator size="small" color="#9333EA" />
                  <Text style={[styles.footerLoaderText, { marginLeft: 8 }]}>
                    더 불러오는 중...
                  </Text>
                </View>
              ) : null}
              <HomeFooterLink />
            </>
          }
          renderItem={({ item }) => (
            <TouchableOpacity
              onPress={() => handleItemPress(item.drugId)}
              activeOpacity={0.7}
              style={styles.cardContainer}
            >
              <View style={styles.imageContainer}>
                <Image
                  source={{ uri: item.itemImageUrl || PLACEHOLDER_IMAGE }}
                  style={styles.image}
                  resizeMode="cover"
                />
              </View>

              <View style={styles.textContainer}>
                <Text style={styles.itemNameText} numberOfLines={1}>
                  {item.itemName}
                </Text>
                <Text style={styles.entpNameText} numberOfLines={1}>
                  {item.entpName}
                </Text>
              </View>
            </TouchableOpacity>
          )}
        />
      )}
      </View>
    </LoadingErrorView>
  );
}
