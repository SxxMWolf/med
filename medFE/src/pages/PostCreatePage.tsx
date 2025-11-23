import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { postsApi } from '../api/posts';
import type { PostCreateRequest } from '../types/api';

export default function PostCreatePage() {
  const navigate = useNavigate();
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [category, setCategory] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!title.trim()) {
      setError('제목을 입력해주세요.');
      return;
    }
    
    if (!content.trim()) {
      setError('내용을 입력해주세요.');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      
      const request: PostCreateRequest = {
        title: title.trim(),
        content: content.trim(),
        ...(category && { category }),
      };

      const response = await postsApi.createPost(request);
      navigate(`/posts/${response.id}`);
    } catch (err: any) {
      setError(err.response?.data?.message || '게시글 작성에 실패했습니다.');
      console.error('게시글 작성 실패:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    navigate('/posts');
  };

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold text-gray-900 mb-6">게시글 작성</h1>

      <form onSubmit={handleSubmit} className="bg-white rounded-lg shadow-sm p-6">
        {error && (
          <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg text-red-700">
            {error}
          </div>
        )}

        <div className="mb-4">
          <label htmlFor="category" className="block text-sm font-medium text-gray-700 mb-2">
            카테고리
          </label>
          <select
            id="category"
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          >
            <option value="">선택 안함</option>
            <option value="GENERAL">일반</option>
            <option value="QUESTION">질문</option>
            <option value="REVIEW">후기</option>
            <option value="TIP">팁</option>
          </select>
        </div>

        <div className="mb-4">
          <label htmlFor="title" className="block text-sm font-medium text-gray-700 mb-2">
            제목 <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            id="title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            placeholder="제목을 입력하세요"
            maxLength={200}
          />
        </div>

        <div className="mb-6">
          <label htmlFor="content" className="block text-sm font-medium text-gray-700 mb-2">
            내용 <span className="text-red-500">*</span>
          </label>
          <textarea
            id="content"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            rows={15}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 resize-none"
            placeholder="내용을 입력하세요"
          />
        </div>

        <div className="flex justify-end gap-3">
          <button
            type="button"
            onClick={handleCancel}
            className="px-6 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition-colors"
            disabled={loading}
          >
            취소
          </button>
          <button
            type="submit"
            className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            disabled={loading}
          >
            {loading ? '저장 중...' : '저장'}
          </button>
        </div>
      </form>
    </div>
  );
}

