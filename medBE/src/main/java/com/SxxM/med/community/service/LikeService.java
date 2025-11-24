package com.sxxm.med.community.service;

import com.sxxm.med.auth.entity.User;
import com.sxxm.med.auth.repository.UserRepository;
import com.sxxm.med.community.dto.LikeResponse;
import com.sxxm.med.community.entity.Comment;
import com.sxxm.med.community.entity.CommentLike;
import com.sxxm.med.community.entity.Post;
import com.sxxm.med.community.entity.PostLike;
import com.sxxm.med.community.repository.CommentLikeRepository;
import com.sxxm.med.community.repository.CommentRepository;
import com.sxxm.med.community.repository.PostLikeRepository;
import com.sxxm.med.community.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LikeService {
    
    private final PostLikeRepository postLikeRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    
    /**
     * 게시글 좋아요
     */
    public LikeResponse likePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        // 이미 좋아요한 경우 무시
        if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            return getPostLikeResponse(postId, userId);
        }
        
        PostLike postLike = PostLike.builder()
                .post(post)
                .user(user)
                .build();
        
        postLikeRepository.save(postLike);
        
        return getPostLikeResponse(postId, userId);
    }
    
    /**
     * 게시글 좋아요 취소
     */
    public LikeResponse unlikePost(Long postId, Long userId) {
        PostLike postLike = postLikeRepository.findByPostIdAndUserId(postId, userId)
                .orElse(null);
        
        if (postLike != null) {
            postLikeRepository.delete(postLike);
        }
        
        return getPostLikeResponse(postId, userId);
    }
    
    /**
     * 댓글 좋아요
     */
    public LikeResponse likeComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        // 이미 좋아요한 경우 무시
        if (commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)) {
            return getCommentLikeResponse(commentId, userId);
        }
        
        CommentLike commentLike = CommentLike.builder()
                .comment(comment)
                .user(user)
                .build();
        
        commentLikeRepository.save(commentLike);
        
        return getCommentLikeResponse(commentId, userId);
    }
    
    /**
     * 댓글 좋아요 취소 (unlike 엔드포인트에서 like 재호출로 처리)
     */
    public LikeResponse unlikeComment(Long commentId, Long userId) {
        CommentLike commentLike = commentLikeRepository.findByCommentIdAndUserId(commentId, userId)
                .orElse(null);
        
        if (commentLike != null) {
            commentLikeRepository.delete(commentLike);
        }
        
        return getCommentLikeResponse(commentId, userId);
    }
    
    private LikeResponse getPostLikeResponse(Long postId, Long userId) {
        long likeCount = postLikeRepository.countByPostId(postId);
        boolean isLiked = postLikeRepository.existsByPostIdAndUserId(postId, userId);
        
        return LikeResponse.builder()
                .likeCount(likeCount)
                .isLiked(isLiked)
                .build();
    }
    
    private LikeResponse getCommentLikeResponse(Long commentId, Long userId) {
        long likeCount = commentLikeRepository.countByCommentId(commentId);
        boolean isLiked = commentLikeRepository.existsByCommentIdAndUserId(commentId, userId);
        
        return LikeResponse.builder()
                .likeCount(likeCount)
                .isLiked(isLiked)
                .build();
    }
}

