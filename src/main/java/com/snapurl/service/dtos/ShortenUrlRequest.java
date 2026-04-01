package com.snapurl.service.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ShortenUrlRequest {

    @NotBlank(message = "Original URL is required.")
    @Size(max = 2048, message = "Original URL is too long.")
    private String originalUrl;

    @Size(max = 32, message = "Custom alias must be at most 32 characters long.")
    @Pattern(
            regexp = "^$|^[A-Za-z0-9_-]{3,32}$",
            message = "Custom alias can only use letters, numbers, hyphens, or underscores and must be 3 to 32 characters long."
    )
    private String customAlias;
}
