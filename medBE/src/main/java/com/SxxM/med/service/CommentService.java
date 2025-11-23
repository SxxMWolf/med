package com.SxxM.med.service;

import com.SxxM.med.dto.CommentCreateRequest;
import com.SxxM.med.dto.CommentResponse;
import com.SxxM.med.dto.CommentUpdateRequest;
import com.SxxM.med.entity.Comment;
import com.SxxM.med.entity.Post;
import com.SxxM.med.entity.User;
import com.SxxM.med.repository.CommentRepository;
import com.SxxM.med.repository.PostRepository;
import com.SxxM.med.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        return toResponse(saved);
    }
    
    public List<CommentResponse> getCommentsByPostId(Long postId) {
        List<Comment> comments = commentRepository.findByPostId(postId);
        return comments.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
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
        return toResponse(updated);
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
    
    private CommentResponse toResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .authorId(comment.getAuthor().getId())
                .authorNickname(comment.getAuthor().getNickname())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}

