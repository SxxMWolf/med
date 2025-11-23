package com.SxxM.med.controller;

import com.SxxM.med.dto.CommentCreateRequest;
import com.SxxM.med.dto.CommentResponse;
import com.SxxM.med.dto.CommentUpdateRequest;
import com.SxxM.med.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Slf4j
public class CommentController {
    
    private final CommentService commentService;
    
    @PostMapping
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
    public ResponseEntity<List<CommentResponse>> getCommentsByPostId(@PathVariable Long postId) {
        try {
            List<CommentResponse> comments = commentService.getCommentsByPostId(postId);
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            log.error("댓글 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PutMapping("/{commentId}")
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
}

