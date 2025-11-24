package com.sxxm.med.community.controller;

import com.sxxm.med.auth.entity.User;
import com.sxxm.med.auth.repository.UserRepository;
import com.sxxm.med.community.dto.*;
import com.sxxm.med.community.service.LikeService;
import com.sxxm.med.community.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Posts", description = "게시글 관리 API")
public class PostController {
    
    private final PostService postService;
    private final LikeService likeService;
    private final UserRepository userRepository;
    
    @PostMapping
    @Operation(summary = "게시글 작성", description = "새로운 게시글을 작성합니다.")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<PostResponse> createPost(
            Authentication authentication,
            @Valid @RequestBody PostCreateRequest request
    ) {
        try {
            String username = authentication.getName();
            PostResponse response = postService.createPost(username, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("게시글 작성 실패", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
    @GetMapping
    @Operation(summary = "게시글 목록 조회", description = "게시글 목록을 페이지네이션하여 조회합니다.")
    public ResponseEntity<Page<PostResponse>> getAllPosts(
            Authentication authentication,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        try {
            Long userId = getUserId(authentication);
            Page<PostResponse> posts = postService.getAllPosts(pageable, category, userId);
            return ResponseEntity.ok(posts);
        } catch (Exception e) {
            log.error("게시글 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{postId}")
    @Operation(summary = "게시글 상세 조회", description = "게시글 상세 정보를 조회합니다. withComments=true일 경우 댓글도 함께 반환합니다.")
    public ResponseEntity<?> getPost(
            Authentication authentication,
            @PathVariable Long postId,
            @RequestParam(required = false, defaultValue = "false") boolean withComments
    ) {
        try {
            Long userId = getUserId(authentication);
            
            if (withComments) {
                PostDetailResponse post = postService.getPostWithComments(postId, userId, true);
                return ResponseEntity.ok(post);
            } else {
                PostResponse post = postService.getPost(postId, userId);
                return ResponseEntity.ok(post);
            }
        } catch (RuntimeException e) {
            log.error("게시글 조회 실패", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("게시글 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PutMapping("/{postId}")
    @Operation(summary = "게시글 수정", description = "게시글을 수정합니다.")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<PostResponse> updatePost(
            Authentication authentication,
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateRequest request
    ) {
        try {
            String username = authentication.getName();
            PostResponse response = postService.updatePost(postId, username, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("게시글 수정 실패", e);
            if (e.getMessage().contains("권한")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("게시글 수정 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @DeleteMapping("/{postId}")
    @Operation(summary = "게시글 삭제", description = "게시글을 삭제합니다.")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<Void> deletePost(
            Authentication authentication,
            @PathVariable Long postId
    ) {
        try {
            String username = authentication.getName();
            postService.deletePost(postId, username);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("게시글 삭제 실패", e);
            if (e.getMessage().contains("권한")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("게시글 삭제 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/{postId}/like")
    @Operation(summary = "게시글 좋아요", description = "게시글에 좋아요를 추가합니다.")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<LikeResponse> likePost(
            Authentication authentication,
            @PathVariable Long postId
    ) {
        try {
            Long userId = getUserId(authentication);
            LikeResponse response = likeService.likePost(postId, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("게시글 좋아요 실패", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("게시글 좋아요 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/{postId}/unlike")
    @Operation(summary = "게시글 좋아요 취소", description = "게시글의 좋아요를 취소합니다.")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<LikeResponse> unlikePost(
            Authentication authentication,
            @PathVariable Long postId
    ) {
        try {
            Long userId = getUserId(authentication);
            LikeResponse response = likeService.unlikePost(postId, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("게시글 좋아요 취소 실패", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("게시글 좋아요 취소 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    private Long getUserId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        return userOpt.map(User::getId).orElse(null);
    }
}

