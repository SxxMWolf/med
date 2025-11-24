package com.sxxm.med.community.service;

import com.sxxm.med.auth.entity.User;
import com.sxxm.med.auth.repository.UserRepository;
import com.sxxm.med.community.dto.CommentCreateRequest;
import com.sxxm.med.community.dto.CommentResponse;
import com.sxxm.med.community.dto.CommentUpdateRequest;
import com.sxxm.med.community.entity.Comment;
import com.sxxm.med.community.entity.Post;
import com.sxxm.med.community.repository.CommentLikeRepository;
import com.sxxm.med.community.repository.CommentRepository;
import com.sxxm.med.community.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CommentService {
    
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ContentValidationService contentValidationService;
    
    public CommentResponse createComment(String username, CommentCreateRequest request) {
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다"));
        
        // 콘텐츠 검증
        if (!contentValidationService.validateContent(request.getContent())) {
            throw new RuntimeException("부적절한 내용이 포함되어 있습니다");
        }
        
        Comment comment = Comment.builder()
                .post(post)
                .author(author)
                .content(request.getContent())
                .build();
        
        Comment saved = commentRepository.save(comment);
        return toResponse(saved, null);
    }
    
    public List<CommentResponse> getCommentsByPostId(Long postId) {
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        return comments.stream()
                .map(comment -> toResponse(comment, null))
                .collect(Collectors.toList());
    }
    
    public List<CommentResponse> getCommentsByPostIdOrdered(Long postId, Long userId) {
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        return comments.stream()
                .map(comment -> toResponse(comment, userId))
                .collect(Collectors.toList());
    }
    
    public Page<CommentResponse> getCommentsByPostIdWithPagination(Long postId, int page, int size, Long userId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<Comment> comments = commentRepository.findByPostId(postId, pageable);
        return comments.map(comment -> toResponse(comment, userId));
    }
    
    public CommentResponse updateComment(Long commentId, String username, CommentUpdateRequest request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다"));
        
        // 작성자 검증
        if (!comment.getAuthor().getUsername().equals(username)) {
            throw new RuntimeException("댓글 수정 권한이 없습니다");
        }
        
        // 콘텐츠 검증
        if (!contentValidationService.validateContent(request.getContent())) {
            throw new RuntimeException("부적절한 내용이 포함되어 있습니다");
        }
        
        comment.setContent(request.getContent());
        Comment updated = commentRepository.save(comment);
        User user = userRepository.findByUsername(username).orElse(null);
        Long userId = user != null ? user.getId() : null;
        return toResponse(updated, userId);
    }
    
    public void deleteComment(Long commentId, String username) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다"));
        
        // 작성자 검증
        if (!comment.getAuthor().getUsername().equals(username)) {
            throw new RuntimeException("댓글 삭제 권한이 없습니다");
        }
        
        commentRepository.delete(comment);
    }
    
    private CommentResponse toResponse(Comment comment, Long userId) {
        Long likeCount = commentLikeRepository.countByCommentId(comment.getId());
        Boolean isLiked = userId != null && commentLikeRepository.existsByCommentIdAndUserId(comment.getId(), userId);
        
        return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .authorId(comment.getAuthor().getId())
                .authorNickname(comment.getAuthor().getNickname())
                .content(comment.getContent())
                .likeCount(likeCount)
                .isLiked(isLiked)
                .createdAt(comment.getCreatedAt())
                .build();
    }
}

