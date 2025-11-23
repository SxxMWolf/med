import React, { useState } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Image,
  ScrollView,
  Alert,
  ActivityIndicator,
} from 'react-native';
import { useRouter } from 'expo-router';
import * as ImagePicker from 'expo-image-picker';
import { analyzeMedicationImage, OCRAnalysisResponse } from '@/lib/services/ocr';

export default function OCRUploadScreen() {
  const router = useRouter();
  const [imageUri, setImageUri] = useState<string | null>(null);
  const [result, setResult] = useState<OCRAnalysisResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const pickImage = async () => {
    // 권한 요청
    const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (status !== 'granted') {
      Alert.alert('권한 필요', '갤러리 접근 권한이 필요합니다');
      return;
    }

    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsEditing: true,
      aspect: [4, 3],
      quality: 0.8,
    });

    if (!result.canceled && result.assets[0]) {
      setImageUri(result.assets[0].uri);
      setResult(null);
    }
  };

  const takePhoto = async () => {
    // 권한 요청
    const { status } = await ImagePicker.requestCameraPermissionsAsync();
    if (status !== 'granted') {
      Alert.alert('권한 필요', '카메라 접근 권한이 필요합니다');
      return;
    }

    const result = await ImagePicker.launchCameraAsync({
      allowsEditing: true,
      aspect: [4, 3],
      quality: 0.8,
    });

    if (!result.canceled && result.assets[0]) {
      setImageUri(result.assets[0].uri);
      setResult(null);
    }
  };

  const handleAnalyze = async () => {
    if (!imageUri) {
      Alert.alert('알림', '이미지를 선택해주세요');
      return;
    }

    try {
      setIsLoading(true);
      const analysis = await analyzeMedicationImage(imageUri);
      setResult(analysis);
    } catch (error: any) {
      Alert.alert('오류', error.response?.data?.message || '분석에 실패했습니다');
    } finally {
      setIsLoading(false);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'safe':
        return '#4CAF50';
      case 'caution':
        return '#FF9800';
      case 'high_risk':
        return '#E74C3C';
      default:
        return '#999';
    }
  };

  const getStatusLabel = (status: string) => {
    switch (status) {
      case 'safe':
        return '복용 가능';
      case 'caution':
        return '주의 필요';
      case 'high_risk':
        return '고위험 성분 포함';
      default:
        return status;
    }
  };

  return (
    <ScrollView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()}>
          <Text style={styles.backButton}>← 뒤로</Text>
        </TouchableOpacity>
        <Text style={styles.title}>OCR 성분표 분석</Text>
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>이미지 선택</Text>
        <View style={styles.buttonRow}>
          <TouchableOpacity style={styles.button} onPress={pickImage}>
            <Text style={styles.buttonText}>📷 갤러리에서 선택</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.button} onPress={takePhoto}>
            <Text style={styles.buttonText}>📸 사진 촬영</Text>
          </TouchableOpacity>
        </View>

        {imageUri && (
          <View style={styles.imageContainer}>
            <Image source={{ uri: imageUri }} style={styles.image} />
            <TouchableOpacity
              style={styles.analyzeButton}
              onPress={handleAnalyze}
              disabled={isLoading}
            >
              {isLoading ? (
                <ActivityIndicator color="#fff" />
              ) : (
                <Text style={styles.analyzeButtonText}>분석 시작</Text>
              )}
            </TouchableOpacity>
          </View>
        )}
      </View>

      {result && (
        <View style={styles.section}>
          <View
            style={[
              styles.statusCard,
              { backgroundColor: getStatusColor(result.analysis.status) + '20' },
            ]}
          >
            <Text style={styles.statusTitle}>분석 결과</Text>
            <Text
              style={[
                styles.statusText,
                { color: getStatusColor(result.analysis.status) },
              ]}
            >
              {getStatusLabel(result.analysis.status)}
            </Text>
            <Text style={styles.riskLevel}>
              위험도: {result.analysis.risk_level === 'low' ? '낮음' : result.analysis.risk_level === 'medium' ? '중간' : '높음'}
            </Text>
          </View>

          {result.normalized_ingredients.length > 0 && (
            <View style={styles.ingredientsContainer}>
              <Text style={styles.sectionTitle}>추출된 성분</Text>
              <View style={styles.ingredientsList}>
                {result.normalized_ingredients.map((ingredient, index) => (
                  <View key={index} style={styles.ingredientTag}>
                    <Text style={styles.ingredientText}>{ingredient}</Text>
                  </View>
                ))}
              </View>
            </View>
          )}

          {result.analysis.matching_allergens.length > 0 && (
            <View style={styles.warningContainer}>
              <Text style={styles.warningTitle}>⚠️ 매칭된 알러지 성분</Text>
              {result.analysis.matching_allergens.map((allergen, index) => (
                <Text key={index} style={styles.warningText}>
                  • {allergen}
                </Text>
              ))}
            </View>
          )}

          {result.analysis.warnings.length > 0 && (
            <View style={styles.warningContainer}>
              <Text style={styles.warningTitle}>주의사항</Text>
              {result.analysis.warnings.map((warning, index) => (
                <Text key={index} style={styles.warningText}>
                  • {warning}
                </Text>
              ))}
            </View>
          )}

          {result.analysis.gpt_summary && (
            <View style={styles.summaryContainer}>
              <Text style={styles.summaryTitle}>요약</Text>
              <Text style={styles.summaryText}>{result.analysis.gpt_summary}</Text>
            </View>
          )}
        </View>
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
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
    borderRadius: 12,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#333',
    marginBottom: 16,
  },
  buttonRow: {
    flexDirection: 'row',
    gap: 12,
    marginBottom: 16,
  },
  button: {
    flex: 1,
    backgroundColor: '#007AFF',
    padding: 16,
    borderRadius: 8,
    alignItems: 'center',
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  imageContainer: {
    marginTop: 16,
  },
  image: {
    width: '100%',
    height: 300,
    borderRadius: 8,
    marginBottom: 16,
    resizeMode: 'contain',
    backgroundColor: '#f0f0f0',
  },
  analyzeButton: {
    backgroundColor: '#4CAF50',
    padding: 16,
    borderRadius: 8,
    alignItems: 'center',
  },
  analyzeButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  statusCard: {
    padding: 20,
    borderRadius: 8,
    marginBottom: 20,
    alignItems: 'center',
  },
  statusTitle: {
    fontSize: 14,
    color: '#666',
    marginBottom: 8,
  },
  statusText: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 4,
  },
  riskLevel: {
    fontSize: 14,
    color: '#666',
  },
  ingredientsContainer: {
    marginBottom: 20,
  },
  ingredientsList: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  ingredientTag: {
    backgroundColor: '#E3F2FD',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 16,
  },
  ingredientText: {
    fontSize: 12,
    color: '#2196F3',
  },
  warningContainer: {
    backgroundColor: '#FFF3E0',
    padding: 16,
    borderRadius: 8,
    marginBottom: 16,
    borderLeftWidth: 4,
    borderLeftColor: '#FF9800',
  },
  warningTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
    marginBottom: 8,
  },
  warningText: {
    fontSize: 14,
    color: '#666',
    marginBottom: 4,
  },
  summaryContainer: {
    backgroundColor: '#F3E5F5',
    padding: 16,
    borderRadius: 8,
  },
  summaryTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
    marginBottom: 8,
  },
  summaryText: {
    fontSize: 14,
    color: '#666',
    lineHeight: 20,
  },
});

