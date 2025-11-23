import { apiClient } from './client';
import type {
  PostCreateRequest,
  PostUpdateRequest,
  PostResponse,
  PagePostResponse,
  LikeResponse,
  Pageable,
} from '../types/api';

export const postsApi = {
  getAllPosts: async (
    pageable: Pageable,
    category?: string
  ): Promise<PagePostResponse> => {
    const params = new URLSearchParams({
      page: pageable.page.toString(),
      size: pageable.size.toString(),
    });
    if (category) {
      params.append('category', category);
    }
    if (pageable.sort) {
      pageable.sort.forEach((sort) => params.append('sort', sort));
    }

    const response = await apiClient.get<PagePostResponse>(
      `/api/posts?${params.toString()}`
    );
    return response.data;
  },

  getPost: async (postId: number, withComments = false): Promise<PostResponse> => {
    const params = withComments ? '?withComments=true' : '';
    const response = await apiClient.get<PostResponse>(`/api/posts/${postId}${params}`);
    return response.data;
  },

  createPost: async (data: PostCreateRequest): Promise<PostResponse> => {
    const response = await apiClient.post<PostResponse>('/api/posts', data);
    return response.data;
  },

  updatePost: async (postId: number, data: PostUpdateRequest): Promise<PostResponse> => {
    const response = await apiClient.put<PostResponse>(`/api/posts/${postId}`, data);
    return response.data;
  },

  deletePost: async (postId: number): Promise<void> => {
    await apiClient.delete(`/api/posts/${postId}`);
  },

  likePost: async (postId: number): Promise<LikeResponse> => {
    const response = await apiClient.post<LikeResponse>(`/api/posts/${postId}/like`);
    return response.data;
  },

  unlikePost: async (postId: number): Promise<LikeResponse> => {
    const response = await apiClient.post<LikeResponse>(`/api/posts/${postId}/unlike`);
    return response.data;
  },
};

