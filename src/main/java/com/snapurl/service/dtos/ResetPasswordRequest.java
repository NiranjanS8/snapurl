package com.snapurl.service.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "Reset code is required.")
    @Pattern(regexp = "^\\d{6}$", message = "Reset code must be a 6-digit code.")
    private String code;

    @NotBlank(message = "Password is required.")
    @Size(min = 8, max = 255, message = "Password must be between 8 and 255 characters long.")
    private String password;
}
