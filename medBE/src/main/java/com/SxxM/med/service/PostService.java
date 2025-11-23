package com.SxxM.med.service;

import com.SxxM.med.dto.PostCreateRequest;
import com.SxxM.med.dto.PostResponse;
import com.SxxM.med.dto.PostUpdateRequest;
import com.SxxM.med.entity.Post;
import com.SxxM.med.entity.User;
import com.SxxM.med.repository.PostRepository;
import com.SxxM.med.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PostService {
    
    private final PostRepository postRepository;
    private final UserRepository userRepository;
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
        return toResponse(saved);
    }
    
    public PostResponse getPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다"));
        return toResponse(post);
    }
    
    public Page<PostResponse> getAllPosts(Pageable pageable, String category) {
        Page<Post> posts;
        if (category != null && !category.isEmpty()) {
            posts = postRepository.findByCategory(category, pageable);
        } else {
            posts = postRepository.findAll(pageable);
        }
        return posts.map(this::toResponse);
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
        return toResponse(updated);
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
    
    private PostResponse toResponse(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .authorId(post.getAuthor().getId())
                .authorNickname(post.getAuthor().getNickname())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}

