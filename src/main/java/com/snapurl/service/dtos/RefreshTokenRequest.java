package com.snapurl.service.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RefreshTokenRequest {
    @NotBlank(message = "Refresh token is required.")
    @Size(max = 255, message = "Refresh token is too long.")
    private String refreshToken;
}
