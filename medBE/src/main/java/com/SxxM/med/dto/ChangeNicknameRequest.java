package com.SxxM.med.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangeNicknameRequest {
    
    @NotBlank(message = "새 닉네임은 필수입니다")
    private String nickname;
}

