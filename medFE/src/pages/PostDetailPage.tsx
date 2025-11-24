import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { postsApi } from '../api/posts';
import { commentsApi } from '../api/comments';
import { useAuthStore } from '../store/authStore';
import { formatDate } from '../utils/date';
import type { PostResponse, CommentResponse } from '../types/api';

export default function PostDetailPage() {
  const { postId } = useParams<{ postId: string }>();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const [post, setPost] = useState<PostResponse | null>(null);
  const [comments, setComments] = useState<CommentResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [commentContent, setCommentContent] = useState('');
  const [editingCommentId, setEditingCommentId] = useState<number | null>(null);
  const [editingCommentContent, setEditingCommentContent] = useState('');
  const [isEditingPost, setIsEditingPost] = useState(false);
  const [editPostTitle, setEditPostTitle] = useState('');
  const [editPostContent, setEditPostContent] = useState('');

  const fetchPost = async () => {
    if (!postId) return;
    
    try {
      setLoading(true);
      setError(null);
      const postData = await postsApi.getPost(Number(postId), true);
      setPost(postData);
      setEditPostTitle(postData.title);
      setEditPostContent(postData.content);
    } catch (err: any) {
      setError(err.response?.data?.message || '게시글을 불러오는데 실패했습니다.');
      console.error('게시글 조회 실패:', err);
    } finally {
      setLoading(false);
    }
  };

  const fetchComments = async () => {
    if (!postId) return;
    
    try {
      const response = await commentsApi.getCommentsByPostId(Number(postId));
      setComments(response.content);
    } catch (err: any) {
      console.error('댓글 조회 실패:', err);
    }
  };

  useEffect(() => {
    fetchPost();
    fetchComments();
  }, [postId]);

  const handleLikePost = async () => {
    if (!postId || !user) return;
    
    try {
      const response = post?.isLiked
        ? await postsApi.unlikePost(Number(postId))
        : await postsApi.likePost(Number(postId));
      
      if (post) {
        setPost({ ...post, likeCount: response.likeCount, isLiked: response.isLiked });
      }
    } catch (err: any) {
      console.error('좋아요 실패:', err);
    }
  };

  const handleCreateComment = async () => {
    if (!postId || !commentContent.trim() || !user) return;
    
    try {
      await commentsApi.createComment({
        postId: Number(postId),
        content: commentContent.trim(),
      });
      setCommentContent('');
      await fetchComments();
    } catch (err: any) {
      setError(err.response?.data?.message || '댓글 작성에 실패했습니다.');
      console.error('댓글 작성 실패:', err);
    }
  };

  const handleUpdateComment = async (commentId: number) => {
    if (!editingCommentContent.trim()) return;
    
    try {
      await commentsApi.updateComment(commentId, {
        content: editingCommentContent.trim(),
      });
      setEditingCommentId(null);
      setEditingCommentContent('');
      await fetchComments();
    } catch (err: any) {
      setError(err.response?.data?.message || '댓글 수정에 실패했습니다.');
      console.error('댓글 수정 실패:', err);
    }
  };

  const handleDeleteComment = async (commentId: number) => {
    if (!window.confirm('댓글을 삭제하시겠습니까?')) return;
    
    try {
      await commentsApi.deleteComment(commentId);
      await fetchComments();
    } catch (err: any) {
      setError(err.response?.data?.message || '댓글 삭제에 실패했습니다.');
      console.error('댓글 삭제 실패:', err);
    }
  };

  const handleLikeComment = async (commentId: number) => {
    if (!user) return;
    
    try {
      await commentsApi.likeComment(commentId);
      await fetchComments();
    } catch (err: any) {
      console.error('댓글 좋아요 실패:', err);
    }
  };

  const handleUpdatePost = async () => {
    if (!postId || !editPostTitle.trim() || !editPostContent.trim()) return;
    
    try {
      const updatedPost = await postsApi.updatePost(Number(postId), {
        title: editPostTitle.trim(),
        content: editPostContent.trim(),
        category: post?.category,
      });
      setPost(updatedPost);
      setIsEditingPost(false);
    } catch (err: any) {
      setError(err.response?.data?.message || '게시글 수정에 실패했습니다.');
      console.error('게시글 수정 실패:', err);
    }
  };

  const handleDeletePost = async () => {
    if (!postId || !window.confirm('게시글을 삭제하시겠습니까?')) return;
    
    try {
      await postsApi.deletePost(Number(postId));
      navigate('/posts');
    } catch (err: any) {
      setError(err.response?.data?.message || '게시글 삭제에 실패했습니다.');
      console.error('게시글 삭제 실패:', err);
    }
  };

  if (loading) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-8">
        <div className="text-center py-12">
          <div className="text-gray-500">로딩 중...</div>
        </div>
      </div>
    );
  }

  if (error && !post) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-8">
        <div className="text-center py-12">
          <div className="text-red-500 mb-4">{error}</div>
          <button
            onClick={() => navigate('/posts')}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
          >
            목록으로
          </button>
        </div>
      </div>
    );
  }

  if (!post) {
    return null;
  }

  const isAuthor = user?.id === post.authorId;

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <div className="mb-4">
        <button
          onClick={() => navigate('/posts')}
          className="text-blue-600 hover:text-blue-800"
        >
          ← 목록으로
        </button>
      </div>

      {error && (
        <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg text-red-700">
          {error}
        </div>
      )}

      <div className="bg-white rounded-lg shadow-sm p-6 mb-6">
        {isEditingPost ? (
          <div>
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                제목
              </label>
              <input
                type="text"
                value={editPostTitle}
                onChange={(e) => setEditPostTitle(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
            </div>
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                내용
              </label>
              <textarea
                value={editPostContent}
                onChange={(e) => setEditPostContent(e.target.value)}
                rows={10}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 resize-none"
              />
            </div>
            <div className="flex gap-3">
              <button
                onClick={handleUpdatePost}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
              >
                저장
              </button>
              <button
                onClick={() => {
                  setIsEditingPost(false);
                  setEditPostTitle(post.title);
                  setEditPostContent(post.content);
                }}
                className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50"
              >
                취소
              </button>
            </div>
          </div>
        ) : (
          <>
            <div className="flex justify-between items-start mb-4">
              <div className="flex-1">
                <div className="flex items-center gap-3 mb-2">
                  <h1 className="text-3xl font-bold text-gray-900">{post.title}</h1>
                  {post.category && (
                    <span className="px-2 py-1 text-xs bg-blue-100 text-blue-800 rounded">
                      {post.category}
                    </span>
                  )}
                </div>
                <div className="flex items-center gap-4 text-sm text-gray-500 mb-4">
                  <span>{post.authorNickname}</span>
                  <span>•</span>
                  <span>{formatDate(post.createdAt)}</span>
                  {post.updatedAt !== post.createdAt && (
                    <>
                      <span>•</span>
                      <span>수정됨: {formatDate(post.updatedAt)}</span>
                    </>
                  )}
                </div>
              </div>
              {isAuthor && (
                <div className="flex gap-2">
                  <button
                    onClick={() => setIsEditingPost(true)}
                    className="px-3 py-1 text-sm border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50"
                  >
                    수정
                  </button>
                  <button
                    onClick={handleDeletePost}
                    className="px-3 py-1 text-sm border border-red-300 rounded-lg text-red-700 hover:bg-red-50"
                  >
                    삭제
                  </button>
                </div>
              )}
            </div>

            <div className="prose max-w-none mb-6">
              <p className="whitespace-pre-wrap text-gray-700">{post.content}</p>
            </div>

            <div className="flex items-center gap-4 pt-4 border-t">
              <button
                onClick={handleLikePost}
                className={`flex items-center gap-2 px-4 py-2 rounded-lg transition-colors ${
                  post.isLiked
                    ? 'bg-red-100 text-red-700 hover:bg-red-200'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
                disabled={!user}
              >
                <span>❤️</span>
                <span>좋아요 {post.likeCount}</span>
              </button>
            </div>
          </>
        )}
      </div>

      {/* 댓글 섹션 */}
      <div className="bg-white rounded-lg shadow-sm p-6">
        <h2 className="text-xl font-bold text-gray-900 mb-4">
          댓글 {comments.length}
        </h2>

        {/* 댓글 작성 */}
        {user ? (
          <div className="mb-6">
            <textarea
              value={commentContent}
              onChange={(e) => setCommentContent(e.target.value)}
              rows={3}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 resize-none mb-2"
              placeholder="댓글을 입력하세요"
            />
            <button
              onClick={handleCreateComment}
              disabled={!commentContent.trim()}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              댓글 작성
            </button>
          </div>
        ) : (
          <div className="mb-6 p-4 bg-gray-50 rounded-lg text-gray-500 text-center">
            댓글을 작성하려면 로그인이 필요합니다.
          </div>
        )}

        {/* 댓글 목록 */}
        <div className="space-y-4">
          {comments.length === 0 ? (
            <div className="text-center py-8 text-gray-500">댓글이 없습니다.</div>
          ) : (
            comments.map((comment) => (
              <div key={comment.id} className="border-b pb-4 last:border-b-0">
                {editingCommentId === comment.id ? (
                  <div>
                    <textarea
                      value={editingCommentContent}
                      onChange={(e) => setEditingCommentContent(e.target.value)}
                      rows={3}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 resize-none mb-2"
                    />
                    <div className="flex gap-2">
                      <button
                        onClick={() => handleUpdateComment(comment.id)}
                        className="px-3 py-1 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700"
                      >
                        저장
                      </button>
                      <button
                        onClick={() => {
                          setEditingCommentId(null);
                          setEditingCommentContent('');
                        }}
                        className="px-3 py-1 text-sm border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50"
                      >
                        취소
                      </button>
                    </div>
                  </div>
                ) : (
                  <>
                    <div className="flex justify-between items-start mb-2">
                      <div className="flex-1">
                        <div className="flex items-center gap-2 mb-1">
                          <span className="font-semibold text-gray-900">
                            {comment.authorNickname}
                          </span>
                          <span className="text-sm text-gray-500">
                            {formatDate(comment.createdAt)}
                          </span>
                        </div>
                        <p className="text-gray-700 whitespace-pre-wrap">{comment.content}</p>
                      </div>
                      <div className="flex gap-2">
                        <button
                          onClick={() => handleLikeComment(comment.id)}
                          className={`px-2 py-1 text-sm rounded-lg transition-colors ${
                            comment.isLiked
                              ? 'bg-red-100 text-red-700'
                              : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                          }`}
                          disabled={!user}
                        >
                          ❤️ {comment.likeCount}
                        </button>
                        {user?.id === comment.authorId && (
                          <>
                            <button
                              onClick={() => {
                                setEditingCommentId(comment.id);
                                setEditingCommentContent(comment.content);
                              }}
                              className="px-2 py-1 text-sm border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50"
                            >
                              수정
                            </button>
                            <button
                              onClick={() => handleDeleteComment(comment.id)}
                              className="px-2 py-1 text-sm border border-red-300 rounded-lg text-red-700 hover:bg-red-50"
                            >
                              삭제
                            </button>
                          </>
                        )}
                      </div>
                    </div>
                  </>
                )}
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}

