import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { postsApi } from '../api/posts';
import { useAuthStore } from '../store/authStore';
import { formatDate } from '../utils/date';
import type { PostResponse, PagePostResponse } from '../types/api';

export default function PostsListPage() {
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const [posts, setPosts] = useState<PostResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [category, setCategory] = useState<string>('');

  const pageSize = 10;

  const fetchPosts = async (page: number, categoryFilter?: string) => {
    try {
      setLoading(true);
      setError(null);
      const response: PagePostResponse = await postsApi.getAllPosts(
        {
          page,
          size: pageSize,
          sort: ['createdAt,desc'],
        },
        categoryFilter || undefined
      );
      setPosts(response.content);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch (err: any) {
      setError(err.response?.data?.message || '게시글을 불러오는데 실패했습니다.');
      console.error('게시글 목록 조회 실패:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPosts(currentPage, category);
  }, [currentPage, category]);

  const handleCategoryChange = (newCategory: string) => {
    setCategory(newCategory);
    setCurrentPage(0);
  };

  const handlePostClick = (postId: number) => {
    navigate(`/posts/${postId}`);
  };

  const handleCreatePost = () => {
    if (!user) {
      navigate('/login');
      return;
    }
    navigate('/posts/create');
  };

  return (
    <div className="max-w-6xl mx-auto px-4 py-8">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-gray-900">커뮤니티</h1>
        <button
          onClick={handleCreatePost}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
        >
          글쓰기
        </button>
      </div>

      {/* 카테고리 필터 */}
      <div className="mb-6 flex gap-2 flex-wrap">
        <button
          onClick={() => handleCategoryChange('')}
          className={`px-4 py-2 rounded-lg transition-colors ${
            category === ''
              ? 'bg-blue-600 text-white'
              : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
          }`}
        >
          전체
        </button>
        <button
          onClick={() => handleCategoryChange('GENERAL')}
          className={`px-4 py-2 rounded-lg transition-colors ${
            category === 'GENERAL'
              ? 'bg-blue-600 text-white'
              : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
          }`}
        >
          일반
        </button>
        <button
          onClick={() => handleCategoryChange('QUESTION')}
          className={`px-4 py-2 rounded-lg transition-colors ${
            category === 'QUESTION'
              ? 'bg-blue-600 text-white'
              : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
          }`}
        >
          질문
        </button>
        <button
          onClick={() => handleCategoryChange('REVIEW')}
          className={`px-4 py-2 rounded-lg transition-colors ${
            category === 'REVIEW'
              ? 'bg-blue-600 text-white'
              : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
          }`}
        >
          후기
        </button>
        <button
          onClick={() => handleCategoryChange('TIP')}
          className={`px-4 py-2 rounded-lg transition-colors ${
            category === 'TIP'
              ? 'bg-blue-600 text-white'
              : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
          }`}
        >
          팁
        </button>
      </div>

      {/* 게시글 목록 */}
      {loading ? (
        <div className="text-center py-12">
          <div className="text-gray-500">로딩 중...</div>
        </div>
      ) : error ? (
        <div className="text-center py-12">
          <div className="text-red-500">{error}</div>
          <button
            onClick={() => fetchPosts(currentPage, category)}
            className="mt-4 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
          >
            다시 시도
          </button>
        </div>
      ) : posts.length === 0 ? (
        <div className="text-center py-12">
          <div className="text-gray-500">게시글이 없습니다.</div>
        </div>
      ) : (
        <>
          <div className="bg-white rounded-lg shadow-sm divide-y">
            {posts.map((post) => (
              <div
                key={post.id}
                onClick={() => handlePostClick(post.id)}
                className="p-6 hover:bg-gray-50 cursor-pointer transition-colors"
              >
                <div className="flex justify-between items-start">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      <h2 className="text-xl font-semibold text-gray-900">{post.title}</h2>
                      {post.category && (
                        <span className="px-2 py-1 text-xs bg-blue-100 text-blue-800 rounded">
                          {post.category}
                        </span>
                      )}
                    </div>
                    <p className="text-gray-600 mb-3 line-clamp-2">{post.content}</p>
                    <div className="flex items-center gap-4 text-sm text-gray-500">
                      <span>{post.authorNickname}</span>
                      <span>•</span>
                      <span>{formatDate(post.createdAt)}</span>
                      <span>•</span>
                      <span>좋아요 {post.likeCount}</span>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {/* 페이지네이션 */}
          {totalPages > 1 && (
            <div className="mt-6 flex justify-center items-center gap-2">
              <button
                onClick={() => setCurrentPage(0)}
                disabled={currentPage === 0}
                className="px-3 py-2 rounded-lg bg-gray-200 text-gray-700 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-300"
              >
                처음
              </button>
              <button
                onClick={() => setCurrentPage(currentPage - 1)}
                disabled={currentPage === 0}
                className="px-3 py-2 rounded-lg bg-gray-200 text-gray-700 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-300"
              >
                이전
              </button>
              <span className="px-4 py-2 text-gray-700">
                {currentPage + 1} / {totalPages} (총 {totalElements}개)
              </span>
              <button
                onClick={() => setCurrentPage(currentPage + 1)}
                disabled={currentPage >= totalPages - 1}
                className="px-3 py-2 rounded-lg bg-gray-200 text-gray-700 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-300"
              >
                다음
              </button>
              <button
                onClick={() => setCurrentPage(totalPages - 1)}
                disabled={currentPage >= totalPages - 1}
                className="px-3 py-2 rounded-lg bg-gray-200 text-gray-700 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-300"
              >
                마지막
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}

