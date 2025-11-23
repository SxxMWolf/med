import React, { useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useAuthStore } from '@/store/authStore';
import { getCurrentUser } from '@/lib/auth';
import { logout } from '@/lib/auth';

export default function HomeScreen() {
  const router = useRouter();
  const { user, setUser, clearAuth } = useAuthStore();
  const [isLoading, setIsLoading] = React.useState(true);

  useEffect(() => {
    const loadUser = async () => {
      try {
        const userData = await getCurrentUser();
        setUser(userData);
      } catch (error) {
        console.error('Failed to load user:', error);
      } finally {
        setIsLoading(false);
      }
    };
    loadUser();
  }, []);

  const handleLogout = async () => {
    await logout();
    clearAuth();
    router.replace('/login');
  };

  if (isLoading) {
    return (
      <View style={styles.centerContainer}>
        <ActivityIndicator size="large" color="#007AFF" />
      </View>
    );
  }

  return (
    <ScrollView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>복약 안전 관리</Text>
        <Text style={styles.subtitle}>
          {user?.name}님, 안녕하세요
        </Text>
      </View>

      <View style={styles.menuGrid}>
        <TouchableOpacity
          style={styles.menuItem}
          onPress={() => router.push('/allergies')}
        >
          <View style={[styles.iconContainer, { backgroundColor: '#FFE5E5' }]}>
            <Text style={[styles.iconText, { color: '#E74C3C' }]}>⚠️</Text>
          </View>
          <Text style={styles.menuTitle}>알러지 관리</Text>
          <Text style={styles.menuDescription}>
            복용하면 안 되는 성분을 등록하세요
          </Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.menuItem}
          onPress={() => router.push('/medicine-search')}
        >
          <View style={[styles.iconContainer, { backgroundColor: '#E3F2FD' }]}>
            <Text style={[styles.iconText, { color: '#2196F3' }]}>🔍</Text>
          </View>
          <Text style={styles.menuTitle}>약 검색</Text>
          <Text style={styles.menuDescription}>
            약품 정보를 검색하세요
          </Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.menuItem}
          onPress={() => router.push('/ocr-upload')}
        >
          <View style={[styles.iconContainer, { backgroundColor: '#E8F5E9' }]}>
            <Text style={[styles.iconText, { color: '#4CAF50' }]}>📷</Text>
          </View>
          <Text style={styles.menuTitle}>OCR 분석</Text>
          <Text style={styles.menuDescription}>
            성분표 사진을 업로드하세요
          </Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.menuItem}
          onPress={() => router.push('/analysis-result')}
        >
          <View style={[styles.iconContainer, { backgroundColor: '#FFF3E0' }]}>
            <Text style={[styles.iconText, { color: '#FF9800' }]}>📊</Text>
          </View>
          <Text style={styles.menuTitle}>분석 결과</Text>
          <Text style={styles.menuDescription}>
            약물 안전성 분석 결과를 확인하세요
          </Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.menuItem}
          onPress={() => router.push('/community')}
        >
          <View style={[styles.iconContainer, { backgroundColor: '#F3E5F5' }]}>
            <Text style={[styles.iconText, { color: '#9C27B0' }]}>💬</Text>
          </View>
          <Text style={styles.menuTitle}>커뮤니티</Text>
          <Text style={styles.menuDescription}>
            다른 사용자들과 정보를 공유하세요
          </Text>
        </TouchableOpacity>
      </View>

      <TouchableOpacity style={styles.logoutButton} onPress={handleLogout}>
        <Text style={styles.logoutText}>로그아웃</Text>
      </TouchableOpacity>
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
  },
  header: {
    padding: 20,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 4,
  },
  subtitle: {
    fontSize: 16,
    color: '#666',
  },
  menuGrid: {
    padding: 16,
    gap: 16,
  },
  menuItem: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  iconContainer: {
    width: 60,
    height: 60,
    borderRadius: 30,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 12,
  },
  iconText: {
    fontSize: 28,
  },
  menuTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#333',
    marginBottom: 4,
  },
  menuDescription: {
    fontSize: 14,
    color: '#666',
  },
  logoutButton: {
    margin: 20,
    padding: 16,
    backgroundColor: '#fff',
    borderRadius: 8,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#e0e0e0',
  },
  logoutText: {
    color: '#E74C3C',
    fontSize: 16,
    fontWeight: '600',
  },
});
