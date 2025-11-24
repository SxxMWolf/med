package com.sxxm.med.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentCreateRequest {
    
    @NotNull(message = "게시글 ID는 필수입니다")
    private Long postId;
    
    @NotBlank(message = "댓글 내용은 필수입니다")
    private String content;
}

