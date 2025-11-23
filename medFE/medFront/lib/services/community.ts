import apiClient from '../api';

export interface Post {
  id: string;
  title: string;
  content: string;
  author: {
    id: string;
    name: string;
  };
  created_at: string;
  comment_count: number;
  like_count: number;
}

export interface Comment {
  id: string;
  content: string;
  author: {
    id: string;
    name: string;
  };
  created_at: string;
}

// 게시글 목록 조회
export const getPosts = async (page: number = 1, limit: number = 20): Promise<Post[]> => {
  const response = await apiClient.get('/community/posts', {
    params: { page, limit },
  });
  return response.data;
};

// 게시글 상세 조회
export const getPost = async (id: string): Promise<Post> => {
  const response = await apiClient.get(`/community/posts/${id}`);
  return response.data;
};

// 게시글 작성
export const createPost = async (title: string, content: string): Promise<Post> => {
  const response = await apiClient.post('/community/posts', { title, content });
  return response.data;
};

// 게시글 삭제
export const deletePost = async (id: string): Promise<void> => {
  await apiClient.delete(`/community/posts/${id}`);
};

// 댓글 목록 조회
export const getComments = async (postId: string): Promise<Comment[]> => {
  const response = await apiClient.get(`/community/posts/${postId}/comments`);
  return response.data;
};

// 댓글 작성
export const createComment = async (postId: string, content: string): Promise<Comment> => {
  const response = await apiClient.post(`/community/posts/${postId}/comments`, { content });
  return response.data;
};

// 댓글 삭제
export const deleteComment = async (postId: string, commentId: string): Promise<void> => {
  await apiClient.delete(`/community/posts/${postId}/comments/${commentId}`);
};

