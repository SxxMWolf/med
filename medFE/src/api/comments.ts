import { apiClient } from './client';
import type {
  CommentCreateRequest,
  CommentUpdateRequest,
  CommentResponse,
  PageCommentResponse,
  LikeResponse,
} from '../types/api';

export const commentsApi = {
  getCommentsByPostId: async (
    postId: number,
    page = 0,
    size = 20
  ): Promise<PageCommentResponse> => {
    const response = await apiClient.get<PageCommentResponse>(
      `/api/comments/post/${postId}?page=${page}&size=${size}`
    );
    return response.data;
  },

  createComment: async (data: CommentCreateRequest): Promise<CommentResponse> => {
    const response = await apiClient.post<CommentResponse>('/api/comments', data);
    return response.data;
  },

  updateComment: async (commentId: number, data: CommentUpdateRequest): Promise<CommentResponse> => {
    const response = await apiClient.put<CommentResponse>(`/api/comments/${commentId}`, data);
    return response.data;
  },

  deleteComment: async (commentId: number): Promise<void> => {
    await apiClient.delete(`/api/comments/${commentId}`);
  },

  likeComment: async (commentId: number): Promise<LikeResponse> => {
    const response = await apiClient.post<LikeResponse>(`/api/comments/${commentId}/like`);
    return response.data;
  },
};

