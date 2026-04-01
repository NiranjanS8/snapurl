package com.snapurl.service.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Email is required.")
    @Email(message = "Please provide a valid email address.")
    @Size(max = 160, message = "Email is too long.")
    private String email;

    @NotBlank(message = "Password is required.")
    @Size(max = 255, message = "Password is too long.")
    private String password;
}
