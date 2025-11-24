package com.sxxm.med.community.controller;

import com.sxxm.med.auth.entity.User;
import com.sxxm.med.auth.repository.UserRepository;
import com.sxxm.med.community.dto.*;
import com.sxxm.med.community.service.CommentService;
import com.sxxm.med.community.service.LikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Comments", description = "댓글 관리 API")
public class CommentController {
    
    private final CommentService commentService;
    private final LikeService likeService;
    private final UserRepository userRepository;
    
    @PostMapping
    @Operation(summary = "댓글 작성", description = "새로운 댓글을 작성합니다.")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<CommentResponse> createComment(
            Authentication authentication,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        try {
            String username = authentication.getName();
            CommentResponse response = commentService.createComment(username, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("댓글 작성 실패", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
    @GetMapping("/post/{postId}")
    @Operation(summary = "댓글 목록 조회", description = "게시글의 댓글 목록을 페이지네이션하여 조회합니다.")
    public ResponseEntity<Page<CommentResponse>> getCommentsByPostId(
            Authentication authentication,
            @PathVariable Long postId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        try {
            Long userId = getUserId(authentication);
            Page<CommentResponse> comments = commentService.getCommentsByPostIdWithPagination(postId, page, size, userId);
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            log.error("댓글 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PutMapping("/{commentId}")
    @Operation(summary = "댓글 수정", description = "댓글을 수정합니다.")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<CommentResponse> updateComment(
            Authentication authentication,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request
    ) {
        try {
            String username = authentication.getName();
            CommentResponse response = commentService.updateComment(commentId, username, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("댓글 수정 실패", e);
            if (e.getMessage().contains("권한")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("댓글 수정 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @DeleteMapping("/{commentId}")
    @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다.")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<Void> deleteComment(
            Authentication authentication,
            @PathVariable Long commentId
    ) {
        try {
            String username = authentication.getName();
            commentService.deleteComment(commentId, username);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("댓글 삭제 실패", e);
            if (e.getMessage().contains("권한")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("댓글 삭제 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/{commentId}/like")
    @Operation(summary = "댓글 좋아요", description = "댓글에 좋아요를 추가합니다.")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<LikeResponse> likeComment(
            Authentication authentication,
            @PathVariable Long commentId
    ) {
        try {
            Long userId = getUserId(authentication);
            LikeResponse response = likeService.likeComment(commentId, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("댓글 좋아요 실패", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("댓글 좋아요 실패", e);
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

