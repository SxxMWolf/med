package com.SxxM.med.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponse {
    
    private Long id;
    private Long postId;
    private Long authorId;
    private String authorNickname;
    private String content;
    private LocalDateTime createdAt;
}

