import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  ActivityIndicator,
  Alert,
  TouchableOpacity,
} from 'react-native';
import { useRouter } from 'expo-router';
import { getAnalysisResult, SymptomAnalysisResponse } from '@/lib/services/analysis';
import { useLocalSearchParams } from 'expo-router';

export default function AnalysisResultScreen() {
  const router = useRouter();
  const params = useLocalSearchParams();
  const analysisId = params.id as string;
  
  const [result, setResult] = useState<SymptomAnalysisResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (analysisId) {
      loadResult();
    } else {
      // 분석 ID가 없으면 예시 데이터 표시 (실제로는 분석 요청 화면에서 이동)
      Alert.alert('알림', '분석 ID가 필요합니다');
      router.back();
    }
  }, [analysisId]);

  const loadResult = async () => {
    try {
      setIsLoading(true);
      const data = await getAnalysisResult(analysisId);
      setResult(data);
    } catch (error: any) {
      Alert.alert('오류', error.response?.data?.message || '분석 결과를 불러오는데 실패했습니다');
    } finally {
      setIsLoading(false);
    }
  };

  const getSafetyColor = (level: string) => {
    switch (level) {
      case 'safe':
        return '#4CAF50';
      case 'caution':
        return '#FF9800';
      case 'warning':
        return '#E74C3C';
      default:
        return '#999';
    }
  };

  const getRiskColor = (level: string) => {
    switch (level) {
      case 'low':
        return '#4CAF50';
      case 'medium':
        return '#FF9800';
      case 'high':
        return '#E74C3C';
      default:
        return '#999';
    }
  };

  if (isLoading) {
    return (
      <View style={styles.centerContainer}>
        <ActivityIndicator size="large" color="#007AFF" />
        <Text style={styles.loadingText}>분석 결과를 불러오는 중...</Text>
      </View>
    );
  }

  if (!result) {
    return (
      <View style={styles.centerContainer}>
        <Text style={styles.errorText}>분석 결과를 불러올 수 없습니다</Text>
      </View>
    );
  }

  return (
    <ScrollView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()}>
          <Text style={styles.backButton}>← 뒤로</Text>
        </TouchableOpacity>
        <Text style={styles.title}>분석 결과</Text>
      </View>

      {/* 추천 가능한 약 */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>✅ 추천 가능한 약</Text>
        {result.recommended_medications.length === 0 ? (
          <Text style={styles.emptyText}>추천할 수 있는 약이 없습니다</Text>
        ) : (
          result.recommended_medications.map((med, index) => (
            <View
              key={index}
              style={[
                styles.medicationCard,
                { borderLeftColor: getSafetyColor(med.safety_level) },
              ]}
            >
              <View style={styles.medicationHeader}>
                <Text style={styles.medicationName}>{med.name}</Text>
                <View
                  style={[
                    styles.badge,
                    { backgroundColor: getSafetyColor(med.safety_level) + '20' },
                  ]}
                >
                  <Text
                    style={[
                      styles.badgeText,
                      { color: getSafetyColor(med.safety_level) },
                    ]}
                  >
                    {med.safety_level === 'safe'
                      ? '안전'
                      : med.safety_level === 'caution'
                      ? '주의'
                      : '경고'}
                  </Text>
                </View>
              </View>
              <Text style={styles.medicationReason}>{med.reason}</Text>
            </View>
          ))
        )}
      </View>

      {/* 피해야 할 약 */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>❌ 피해야 할 약</Text>
        {result.medications_to_avoid.length === 0 ? (
          <Text style={styles.emptyText}>피해야 할 약이 없습니다</Text>
        ) : (
          result.medications_to_avoid.map((med, index) => (
            <View
              key={index}
              style={[
                styles.medicationCard,
                { borderLeftColor: getRiskColor(med.risk_level) },
              ]}
            >
              <View style={styles.medicationHeader}>
                <Text style={styles.medicationName}>{med.name}</Text>
                <View
                  style={[
                    styles.badge,
                    { backgroundColor: getRiskColor(med.risk_level) + '20' },
                  ]}
                >
                  <Text
                    style={[
                      styles.badgeText,
                      { color: getRiskColor(med.risk_level) },
                    ]}
                  >
                    {med.risk_level === 'low'
                      ? '낮음'
                      : med.risk_level === 'medium'
                      ? '중간'
                      : '높음'}
                  </Text>
                </View>
              </View>
              <Text style={styles.medicationReason}>{med.reason}</Text>
            </View>
          ))
        )}
      </View>

      {/* 위험 요소 요약 */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>⚠️ 위험 요소 요약</Text>
        <View style={styles.summaryCard}>
          <Text style={styles.summaryLabel}>총 위험 요소</Text>
          <Text style={styles.summaryValue}>{result.risk_summary.total_risks}개</Text>
        </View>

        {result.risk_summary.high_risk_ingredients.length > 0 && (
          <View style={styles.ingredientsContainer}>
            <Text style={styles.subtitle}>고위험 성분</Text>
            <View style={styles.ingredientsList}>
              {result.risk_summary.high_risk_ingredients.map((ingredient, index) => (
                <View key={index} style={styles.ingredientTag}>
                  <Text style={styles.ingredientText}>{ingredient}</Text>
                </View>
              ))}
            </View>
          </View>
        )}

        {result.risk_summary.warnings.length > 0 && (
          <View style={styles.warningsContainer}>
            <Text style={styles.subtitle}>주의사항</Text>
            {result.risk_summary.warnings.map((warning, index) => (
              <Text key={index} style={styles.warningText}>
                • {warning}
              </Text>
            ))}
          </View>
        )}

        {result.risk_summary.gpt_analysis && (
          <View style={styles.gptContainer}>
            <Text style={styles.subtitle}>AI 분석 요약</Text>
            <Text style={styles.gptText}>{result.risk_summary.gpt_analysis}</Text>
          </View>
        )}
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  centerContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  loadingText: {
    marginTop: 16,
    fontSize: 14,
    color: '#666',
  },
  errorText: {
    fontSize: 16,
    color: '#E74C3C',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 16,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
  },
  backButton: {
    fontSize: 16,
    color: '#007AFF',
    marginRight: 12,
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333',
  },
  section: {
    backgroundColor: '#fff',
    padding: 20,
    margin: 16,
    marginTop: 16,
    borderRadius: 12,
  },
  sectionTitle: {
    fontSize: 20,
    fontWeight: '600',
    color: '#333',
    marginBottom: 16,
  },
  emptyText: {
    fontSize: 14,
    color: '#999',
    textAlign: 'center',
    padding: 20,
  },
  medicationCard: {
    backgroundColor: '#f9f9f9',
    padding: 16,
    borderRadius: 8,
    marginBottom: 12,
    borderLeftWidth: 4,
  },
  medicationHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  medicationName: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
    flex: 1,
  },
  badge: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
  },
  badgeText: {
    fontSize: 12,
    fontWeight: '600',
  },
  medicationReason: {
    fontSize: 14,
    color: '#666',
    lineHeight: 20,
  },
  summaryCard: {
    backgroundColor: '#E3F2FD',
    padding: 16,
    borderRadius: 8,
    alignItems: 'center',
    marginBottom: 16,
  },
  summaryLabel: {
    fontSize: 14,
    color: '#666',
    marginBottom: 4,
  },
  summaryValue: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#2196F3',
  },
  ingredientsContainer: {
    marginBottom: 16,
  },
  subtitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
    marginBottom: 12,
  },
  ingredientsList: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  ingredientTag: {
    backgroundColor: '#FFE5E5',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 16,
  },
  ingredientText: {
    fontSize: 12,
    color: '#E74C3C',
    fontWeight: '500',
  },
  warningsContainer: {
    marginBottom: 16,
  },
  warningText: {
    fontSize: 14,
    color: '#666',
    marginBottom: 8,
    lineHeight: 20,
  },
  gptContainer: {
    backgroundColor: '#F3E5F5',
    padding: 16,
    borderRadius: 8,
  },
  gptText: {
    fontSize: 14,
    color: '#666',
    lineHeight: 20,
  },
});

