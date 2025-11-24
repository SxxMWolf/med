package com.sxxm.med.community.service;

import com.sxxm.med.auth.entity.User;
import com.sxxm.med.auth.repository.UserRepository;
import com.sxxm.med.community.dto.*;
import com.sxxm.med.community.entity.Comment;
import com.sxxm.med.community.entity.Post;
import com.sxxm.med.community.repository.CommentLikeRepository;
import com.sxxm.med.community.repository.CommentRepository;
import com.sxxm.med.community.repository.PostLikeRepository;
import com.sxxm.med.community.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PostService {
    
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final ContentValidationService contentValidationService;
    
    public PostResponse createPost(String username, PostCreateRequest request) {
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        // 콘텐츠 검증
        if (!contentValidationService.validateContent(request.getContent())) {
            throw new RuntimeException("부적절한 내용이 포함되어 있습니다");
        }
        
        Post post = Post.builder()
                .author(author)
                .title(request.getTitle())
                .content(request.getContent())
                .category(request.getCategory() != null ? request.getCategory() : "자유게시판")
                .build();
        
        Post saved = postRepository.save(post);
        return toResponse(saved, null);
    }
    
    public PostResponse getPost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다"));
        return toResponse(post, userId);
    }
    
    public PostDetailResponse getPostWithComments(Long postId, Long userId, boolean withComments) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다"));
        
        PostDetailResponse.PostDetailResponseBuilder builder = PostDetailResponse.builder()
                .id(post.getId())
                .authorId(post.getAuthor().getId())
                .authorNickname(post.getAuthor().getNickname())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory())
                .likeCount(postLikeRepository.countByPostId(postId))
                .isLiked(userId != null && postLikeRepository.existsByPostIdAndUserId(postId, userId))
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt());
        
        if (withComments) {
            List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
            List<CommentResponse> commentResponses = comments.stream()
                    .map(comment -> toCommentResponse(comment, userId))
                    .collect(java.util.stream.Collectors.toList());
            builder.comments(commentResponses);
        }
        
        return builder.build();
    }
    
    private CommentResponse toCommentResponse(Comment comment, Long userId) {
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
    
    public Page<PostResponse> getAllPosts(Pageable pageable, String category, Long userId) {
        Page<Post> posts;
        if (category != null && !category.isEmpty()) {
            posts = postRepository.findByCategory(category, pageable);
        } else {
            posts = postRepository.findAll(pageable);
        }
        return posts.map(post -> toResponse(post, userId));
    }
    
    public PostResponse updatePost(Long postId, String username, PostUpdateRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다"));
        
        // 작성자 검증
        if (!post.getAuthor().getUsername().equals(username)) {
            throw new RuntimeException("게시글 수정 권한이 없습니다");
        }
        
        // 콘텐츠 검증
        if (!contentValidationService.validateContent(request.getContent())) {
            throw new RuntimeException("부적절한 내용이 포함되어 있습니다");
        }
        
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        if (request.getCategory() != null) {
            post.setCategory(request.getCategory());
        }
        
        Post updated = postRepository.save(post);
        User user = userRepository.findByUsername(username).orElse(null);
        Long userId = user != null ? user.getId() : null;
        return toResponse(updated, userId);
    }
    
    public void deletePost(Long postId, String username) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다"));
        
        // 작성자 검증
        if (!post.getAuthor().getUsername().equals(username)) {
            throw new RuntimeException("게시글 삭제 권한이 없습니다");
        }
        
        postRepository.delete(post);
    }
    
    private PostResponse toResponse(Post post, Long userId) {
        Long likeCount = postLikeRepository.countByPostId(post.getId());
        Boolean isLiked = userId != null && postLikeRepository.existsByPostIdAndUserId(post.getId(), userId);
        
        return PostResponse.builder()
                .id(post.getId())
                .authorId(post.getAuthor().getId())
                .authorNickname(post.getAuthor().getNickname())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory())
                .likeCount(likeCount)
                .isLiked(isLiked)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}

