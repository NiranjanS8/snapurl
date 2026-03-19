package com.snapurl.service.security;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtAuthenticationResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
}
