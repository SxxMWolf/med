import { useEffect, useState } from 'react';
import api from '@/api';

export default function TestPage() {
  const [result, setResult] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setLoading(true);
    api
      .get('/api/health')
      .then((res) => {
        console.log('OK:', res.data);
        setResult(res.data);
        setError(null);
      })
      .catch((err) => {
        console.error('ERR:', err);
        setError(err.message || '헬스체크 실패');
        setResult(null);
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  return (
    <div className="px-4 py-8">
      <div className="max-w-4xl mx-auto">
        <h1 className="text-3xl font-bold text-gray-900 mb-6">헬스체크 테스트</h1>
        
        {loading && (
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-4">
            <p className="text-blue-700">테스트 중...</p>
          </div>
        )}

        {error && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-4">
            <p className="text-red-700 font-semibold">에러:</p>
            <p className="text-red-600">{error}</p>
          </div>
        )}

        {result && (
          <div className="bg-green-50 border border-green-200 rounded-lg p-4 mb-4">
            <p className="text-green-700 font-semibold">성공:</p>
            <pre className="text-green-600 mt-2 overflow-auto">
              {JSON.stringify(result, null, 2)}
            </pre>
          </div>
        )}

        <div className="mt-6">
          <button
            onClick={() => {
              setLoading(true);
              setError(null);
              setResult(null);
              api
                .get('/api/health')
                .then((res) => {
                  console.log('OK:', res.data);
                  setResult(res.data);
                  setError(null);
                })
                .catch((err) => {
                  console.error('ERR:', err);
                  setError(err.message || '헬스체크 실패');
                  setResult(null);
                })
                .finally(() => {
                  setLoading(false);
                });
            }}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
          >
            다시 테스트
          </button>
        </div>
      </div>
    </div>
  );
}

